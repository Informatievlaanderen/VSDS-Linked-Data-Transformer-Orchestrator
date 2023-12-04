package be.vlaanderen.informatievlaanderen.ldes.ldio.config;

import be.vlaanderen.informatievlaanderen.ldes.ldi.services.ComponentExecutor;
import be.vlaanderen.informatievlaanderen.ldes.ldi.types.LdiAdapter;
import be.vlaanderen.informatievlaanderen.ldes.ldi.types.LdiComponent;
import be.vlaanderen.informatievlaanderen.ldes.ldi.types.LdiOutput;
import be.vlaanderen.informatievlaanderen.ldes.ldio.config.logging.ComponentExecutorLogger;
import be.vlaanderen.informatievlaanderen.ldes.ldio.config.logging.LdiOutputLogger;
import be.vlaanderen.informatievlaanderen.ldes.ldio.config.logging.TransformerLogger;
import be.vlaanderen.informatievlaanderen.ldes.ldio.configurator.LdioConfigurator;
import be.vlaanderen.informatievlaanderen.ldes.ldio.configurator.LdioInputConfigurator;
import be.vlaanderen.informatievlaanderen.ldes.ldio.configurator.LdioTransformerConfigurator;
import be.vlaanderen.informatievlaanderen.ldes.ldio.events.PipelineCreatedEvent;
import be.vlaanderen.informatievlaanderen.ldes.ldio.services.ComponentExecutorImpl;
import be.vlaanderen.informatievlaanderen.ldes.ldio.services.LdioSender;
import be.vlaanderen.informatievlaanderen.ldes.ldio.types.LdioTransformer;
import be.vlaanderen.informatievlaanderen.ldes.ldio.valueobjects.ComponentDefinition;
import be.vlaanderen.informatievlaanderen.ldes.ldio.valueobjects.ComponentProperties;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.*;

import static be.vlaanderen.informatievlaanderen.ldes.ldio.config.OrchestratorConfig.DEBUG;
import static be.vlaanderen.informatievlaanderen.ldes.ldio.config.OrchestratorConfig.ORCHESTRATOR_NAME;
import static be.vlaanderen.informatievlaanderen.ldes.ldio.config.PipelineConfig.PIPELINE_NAME;

@Configuration
@ComponentScan("be.vlaanderen.informatievlaanderen")
@Component
public class FlowAutoConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowAutoConfiguration.class);
	private final OrchestratorConfig orchestratorConfig;
	private final ConfigurableApplicationContext configContext;
	private final ApplicationEventPublisher eventPublisher;
	private final ObservationRegistry observationRegistry;

	public FlowAutoConfiguration(OrchestratorConfig orchestratorConfig,
								 ConfigurableApplicationContext configContext, ApplicationEventPublisher eventPublisher, ObservationRegistry observationRegistry) {
		this.orchestratorConfig = orchestratorConfig;
		this.configContext = configContext;
		this.eventPublisher = eventPublisher;
		this.observationRegistry = observationRegistry;
	}

	@PostConstruct
	public void registerInputBeans() {
		orchestratorConfig.getPipelines().forEach(this::initialiseLdiInput);
	}

	public ComponentExecutor componentExecutor(final PipelineConfig pipelineConfig) {
		List<LdioTransformer> ldioTransformers = pipelineConfig.getTransformers()
				.stream()
				.map(this::getLdioTransformer)
				.toList();

		List<LdiOutput> ldiOutputs = pipelineConfig.getOutputs()
				.stream()
				.map(this::getLdioOutput)
				.toList();

		LdioSender ldioSender = new LdioSender(pipelineConfig.getName(), eventPublisher, ldiOutputs);

		List<LdioTransformer> processorChain = new ArrayList<>(ldioTransformers.subList(0, ldioTransformers.size()));

		processorChain.add(ldioSender);

		LdioTransformer ldioTransformerPipeline = processorChain.get(0);

		if (processorChain.size() > 1) {
			ldioTransformerPipeline = LdioTransformer.link(processorChain.get(0), processorChain);
		}

		registerBean(pipelineConfig.getName() + "-ldiSender", ldioSender);

		return new ComponentExecutorLogger(new ComponentExecutorImpl(ldioTransformerPipeline), observationRegistry);
	}

	public void initialiseLdiInput(PipelineConfig config) {
		LdioInputConfigurator configurator = (LdioInputConfigurator) configContext.getBean(
				config.getInput().getName());

		LdiAdapter adapter = Optional.ofNullable(config.getInput().getAdapter())
				.map(this::getLdioAdapter)
				.orElseGet(() -> {
					LOGGER.warn("No adapter configured for pipeline {}. Please verify this is a desired scenario.", config.getName());
					return null;
				});

		ComponentExecutor executor = componentExecutor(config);

		String pipeLineName = config.getName();

		Map<String, String> inputConfig = new HashMap<>(config.getInput().getConfig().getConfig());
		inputConfig.put(ORCHESTRATOR_NAME, orchestratorConfig.getName());
		inputConfig.put(PIPELINE_NAME, pipeLineName);

		Object ldiInput = configurator.configure(adapter, executor, new ComponentProperties(inputConfig));

		registerBean(pipeLineName, ldiInput);
		eventPublisher.publishEvent(new PipelineCreatedEvent(this, config));
	}

	private LdiAdapter getLdioAdapter(ComponentDefinition componentDefinition) {
		boolean debug = componentDefinition.getConfig().getOptionalBoolean(DEBUG).orElse(false);

		LdiAdapter adapter = (LdiAdapter) getLdiComponent(componentDefinition.getName(),
				componentDefinition.getConfig());

		return debug ? new AdapterDebugger(adapter) : adapter;
	}

	private LdioTransformer getLdioTransformer(ComponentDefinition componentDefinition) {
		boolean debug = componentDefinition.getConfig().getOptionalBoolean(DEBUG).orElse(false);

		LdioTransformer ldiTransformer = ((LdioTransformerConfigurator) configContext
				.getBean(componentDefinition.getName()))
				.configure(componentDefinition.getConfig());

		return debug
				? new TransformerLogger(new TransformerDebugger(ldiTransformer), observationRegistry)
				: new TransformerLogger(ldiTransformer, observationRegistry);
	}

	private LdiOutput getLdioOutput(ComponentDefinition componentDefinition) {
		boolean debug = componentDefinition.getConfig().getOptionalBoolean(DEBUG).orElse(false);

		LdiOutput ldiOutput = (LdiOutput) getLdiComponent(componentDefinition.getName(),
				componentDefinition.getConfig());


		return debug ?
				new LdiOutputLogger(new OutputDebugger(ldiOutput), observationRegistry) :
				new LdiOutputLogger(ldiOutput, observationRegistry);
	}

	private LdiComponent getLdiComponent(String beanName, ComponentProperties config) {
		return ((LdioConfigurator) configContext.getBean(beanName)).configure(config);
	}

	private void registerBean(String pipelineName, Object bean) {
		SingletonBeanRegistry beanRegistry = configContext.getBeanFactory();
		if (!beanRegistry.containsSingleton(pipelineName)) {
			beanRegistry.registerSingleton(pipelineName, bean);
		}
	}

}
