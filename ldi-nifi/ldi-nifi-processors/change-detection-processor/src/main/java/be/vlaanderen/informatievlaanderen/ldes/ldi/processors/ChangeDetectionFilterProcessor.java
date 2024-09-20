package be.vlaanderen.informatievlaanderen.ldes.ldi.processors;


import be.vlaanderen.informatievlaanderen.ldes.ldi.ChangeDetectionFilter;
import be.vlaanderen.informatievlaanderen.ldes.ldi.h2.H2EntityManager;
import be.vlaanderen.informatievlaanderen.ldes.ldi.h2.H2Properties;
import be.vlaanderen.informatievlaanderen.ldes.ldi.processors.config.PersistenceProperties;
import be.vlaanderen.informatievlaanderen.ldes.ldi.processors.services.FlowManager;
import be.vlaanderen.informatievlaanderen.ldes.ldi.repositories.HashedStateMemberRepository;
import be.vlaanderen.informatievlaanderen.ldes.ldi.repositories.sql.SqlHashedStateMemberRepository;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnRemoved;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;

import java.util.List;
import java.util.Set;

import static be.vlaanderen.informatievlaanderen.ldes.ldi.processors.config.ChangeDetectionFilterProperties.DATA_SOURCE_FORMAT;
import static be.vlaanderen.informatievlaanderen.ldes.ldi.processors.config.ChangeDetectionFilterProperties.getDataSourceFormat;
import static be.vlaanderen.informatievlaanderen.ldes.ldi.processors.config.ChangeDetectionFilterRelationships.IGNORED;
import static be.vlaanderen.informatievlaanderen.ldes.ldi.processors.config.ChangeDetectionFilterRelationships.NEW_STATE_RECEIVED;
import static be.vlaanderen.informatievlaanderen.ldes.ldi.processors.config.PersistenceProperties.KEEP_STATE;
import static be.vlaanderen.informatievlaanderen.ldes.ldi.processors.services.FlowManager.sendRDFToRelation;

@SuppressWarnings("java:S2160") // nifi handles equals/hashcode of processors
@Tags({"change-detection-filter", "vsds"})
@CapabilityDescription("Checks if the state of state members has been changed, and if not the member will be ignored")
public class ChangeDetectionFilterProcessor extends AbstractProcessor {
	private ChangeDetectionFilter changeDetectionFilter;

	@Override
	public Set<Relationship> getRelationships() {
		return Set.of(NEW_STATE_RECEIVED, IGNORED);
	}

	@Override
	protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return List.of(
				DATA_SOURCE_FORMAT,
				KEEP_STATE
		);
	}

	@OnScheduled
	public void onScheduled(final ProcessContext context) {
		var h2EntityManager = H2EntityManager.getInstance(context.getName(), new H2Properties(context.getName()).getProperties());
		final HashedStateMemberRepository repository = new SqlHashedStateMemberRepository(h2EntityManager, context.getName());

		final boolean keepState = PersistenceProperties.stateKept(context);
		changeDetectionFilter = new ChangeDetectionFilter(repository, keepState);
	}

	@Override
	public void onTrigger(final ProcessContext context, final ProcessSession session) {
		final Lang fallbackLang = getDataSourceFormat(context);

		final FlowFile flowFile = session.get();
		if (flowFile == null) {
			return;
		}

		final String mimeType = flowFile.getAttribute("mime.type");
		final Lang lang = mimeType != null
				? RDFLanguages.contentTypeToLang(mimeType)
				: fallbackLang;
		final Model model = FlowManager.receiveDataAsModel(session, flowFile, lang);

		final Model filteredModel = changeDetectionFilter.transform(model);

		if (filteredModel.isEmpty()) {
			sendRDFToRelation(session, flowFile, IGNORED);
		} else {
			sendRDFToRelation(session, flowFile, NEW_STATE_RECEIVED);
		}
	}

	@OnRemoved
	public void onRemoved() {
		changeDetectionFilter.destroyState();
	}
}
