package be.vlaanderen.informatievlaanderen.ldes.ldio.config;

import be.vlaanderen.informatievlaanderen.ldes.ldi.services.ComponentExecutor;
import be.vlaanderen.informatievlaanderen.ldes.ldi.types.LdiAdapter;
import be.vlaanderen.informatievlaanderen.ldes.ldio.LdioKafkaIn;
import be.vlaanderen.informatievlaanderen.ldes.ldio.configurator.LdioInputConfigurator;
import be.vlaanderen.informatievlaanderen.ldes.ldio.types.LdioInput;
import be.vlaanderen.informatievlaanderen.ldes.ldio.valueobjects.ComponentProperties;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static be.vlaanderen.informatievlaanderen.ldes.ldio.LdioKafkaIn.NAME;
import static be.vlaanderen.informatievlaanderen.ldes.ldio.config.KafkaInConfigKeys.*;
import static be.vlaanderen.informatievlaanderen.ldes.ldio.config.OrchestratorConfig.ORCHESTRATOR_NAME;
import static be.vlaanderen.informatievlaanderen.ldes.ldio.exception.LdiAdapterMissingException.verifyAdapterPresent;
import static be.vlaanderen.informatievlaanderen.ldes.ldio.valueobjects.PipelineStatus.STARTING;

@Configuration
public class LdioKafkaInAutoConfig {

	@SuppressWarnings("java:S6830")
	@Bean(NAME)
	public LdioKafkaInConfigurator ldioConfigurator(ObservationRegistry observationRegistry) {
		return new LdioKafkaInConfigurator(observationRegistry);
	}

	public static class LdioKafkaInConfigurator implements LdioInputConfigurator {
		private final ObservationRegistry observationRegistry;

		public LdioKafkaInConfigurator(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
		}

		@Override
		public LdioInput configure(LdiAdapter adapter, ComponentExecutor executor, ApplicationEventPublisher applicationEventPublisher, ComponentProperties config) {
			String pipelineName = config.getPipelineName();
			verifyAdapterPresent(pipelineName, adapter);

			LdioKafkaIn ldioIn = new LdioKafkaIn(pipelineName, executor, adapter, observationRegistry, applicationEventPublisher, config);
			ldioIn.updateStatus(STARTING);
			return ldioIn;
		}
	}
}
