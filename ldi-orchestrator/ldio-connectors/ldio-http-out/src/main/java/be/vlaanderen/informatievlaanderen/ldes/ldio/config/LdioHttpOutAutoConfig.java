package be.vlaanderen.informatievlaanderen.ldes.ldio.config;

import be.vlaanderen.informatievlaanderen.ldes.ldi.rdf.formatter.LdiRdfWriterProperties;
import be.vlaanderen.informatievlaanderen.ldes.ldi.requestexecutor.executor.RequestExecutor;
import be.vlaanderen.informatievlaanderen.ldes.ldi.types.LdiComponent;
import be.vlaanderen.informatievlaanderen.ldes.ldio.LdioHttpOut;
import be.vlaanderen.informatievlaanderen.ldes.ldio.configurator.LdioOutputConfigurator;
import be.vlaanderen.informatievlaanderen.ldes.ldio.requestexecutor.LdioRequestExecutorSupplier;
import be.vlaanderen.informatievlaanderen.ldes.ldio.valueobjects.ComponentProperties;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static be.vlaanderen.informatievlaanderen.ldes.ldi.rdf.formatter.LdiRdfWriterProperties.RDF_WRITER;

@Configuration
public class LdioHttpOutAutoConfig {

	@SuppressWarnings("java:S6830")
	@Bean("be.vlaanderen.informatievlaanderen.ldes.ldio.LdioHttpOut")
	public LdioOutputConfigurator ldiHttpOutConfigurator() {
		return new LdioHttpOutConfigurator();
	}

	public static class LdioHttpOutConfigurator implements LdioOutputConfigurator {

		@Override
		public LdiComponent configure(ComponentProperties config) {
			final RequestExecutor requestExecutor = new LdioRequestExecutorSupplier().getRequestExecutor(config);

			String targetURL = config.getProperty("endpoint");
			String contentType = config.getOptionalProperty("content-type").orElse("text/turtle");
			var properties = new LdiRdfWriterProperties(config.extractNestedProperties(RDF_WRITER).getConfig()).withLang(RDFLanguages.nameToLang(contentType));

			return new LdioHttpOut(requestExecutor, targetURL, properties);
		}
	}
}
