package be.vlaanderen.informatievlaanderen.ldes.ldi.processors;

import be.vlaanderen.informatievlaanderen.ldes.ldio.ArchiveFile;
import be.vlaanderen.informatievlaanderen.ldes.ldio.TimestampExtractor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFWriter;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static be.vlaanderen.informatievlaanderen.ldes.ldi.processors.config.ArchiveFileOutProperties.*;
import static be.vlaanderen.informatievlaanderen.ldes.ldi.processors.services.FlowManager.*;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

@SuppressWarnings("java:S2160") // nifi handles equals/hashcode of processors
@Tags({ "ldes, vsds, archive, file" })
@CapabilityDescription("Writes members to a file archive.")
public class ArchiveFileOutProcessor extends AbstractProcessor {

    private TimestampExtractor timestampExtractor;
    private String archiveRootDir;

    @Override
    public Set<Relationship> getRelationships() {
        return Set.of(SUCCESS, FAILURE);
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return List.of(TIMESTAMP_PATH, ARCHIVE_ROOT_DIR);
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        timestampExtractor = new TimestampExtractor(createProperty(getTimestampPath(context)));
        archiveRootDir = getArchiveRootDirectory(context);
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        final FlowFile flowFile = session.get();
        Lang dataSourceFormat = getDataSourceFormat(context);
        Model model = receiveDataAsModel(session, flowFile, dataSourceFormat);
        ArchiveFile archiveFile = ArchiveFile.from(model, timestampExtractor, archiveRootDir);
        try {
            Files.createDirectories(archiveFile.getDirectoryPath());
            RDFWriter.source(model).lang(Lang.NQUADS).output(archiveFile.getFilePath());
            sendRDFToRelation(session, flowFile, model, SUCCESS, dataSourceFormat);
        } catch (IOException e) {
            getLogger().error("Failed to write model to file in archive directory: {}", e.getMessage());
            sendRDFToRelation(session, flowFile, FAILURE);
        }

    }

}
