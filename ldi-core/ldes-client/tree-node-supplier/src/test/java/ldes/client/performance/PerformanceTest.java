package ldes.client.performance;

import be.vlaanderen.informatievlaanderen.ldes.ldi.requestexecutor.executor.RequestExecutor;
import be.vlaanderen.informatievlaanderen.ldes.ldi.requestexecutor.executor.noauth.DefaultConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import ldes.client.performance.csvwriter.CsvFile;
import ldes.client.treenodesupplier.TreeNodeProcessor;
import ldes.client.treenodesupplier.domain.services.MemberRepositoryFactory;
import ldes.client.treenodesupplier.domain.services.TreeNodeRecordRepositoryFactory;
import ldes.client.treenodesupplier.domain.valueobject.LdesMetaData;
import ldes.client.treenodesupplier.domain.valueobject.StatePersistence;
import ldes.client.treenodesupplier.domain.valueobject.StatePersistenceStrategy;
import ldes.client.treenodesupplier.repository.MemberRepository;
import ldes.client.treenodesupplier.repository.TreeNodeRecordRepository;
import ldes.client.treenodesupplier.repository.sql.postgres.PostgresProperties;
import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class PerformanceTest {

    private static WireMockServer wireMockServer;
    private static final String CSV_PATH = "results.csv";
    private static CsvFile csvFile;
    private static final int TEST_SIZE = 1000;

    @BeforeAll
    static void setUp() {
        csvFile = new CsvFile();
        ResponseTemplateTransformer templateTransformer = new ResponseTemplateTransformer(false);
        wireMockServer = new WireMockServer(WireMockConfiguration.options().extensions(templateTransformer).port(10101));
        wireMockServer.start();
    }

    @AfterAll
    static void tearDown() {
        csvFile.writeToFile(CSV_PATH);
        wireMockServer.stop();
    }

    /**
     * Wat willen we:
     * - verschillende fragmentgrootte
     * - verschillende persistence methodes
     * <p>
     * grafiek van members/seconde op Y as en members processed op X -as
     */
    @ParameterizedTest
    @EnumSource(TestScenario.class)
    void performanceTest(TestScenario test) {
        final TreeNodeProcessor treeNodeProcessor =
                createTreeNodeProcessor(test.getPersistenceStrategy(), test.getStartingEndpoint());

        LocalDateTime lastInterval = LocalDateTime.now();
        for (int i = 1; i <= TEST_SIZE; i++) {
            treeNodeProcessor.getMember();
            if (i % (TEST_SIZE / 20) == 0) {
                int msIntervals = (int) ChronoUnit.MILLIS.between(lastInterval, lastInterval = LocalDateTime.now());
                csvFile.addLine(i, msIntervals, test);
                System.out.println(i + ": " + msIntervals);
            }
        }
    }

    private TreeNodeProcessor createTreeNodeProcessor(StatePersistenceStrategy statePersistenceStrategy, String url) {
        final LdesMetaData ldesMetaData = new LdesMetaData(url, Lang.TURTLE);
        final StatePersistence statePersistence = switch (statePersistenceStrategy) {
            case MEMORY -> createInMemoryStatePersistence();
            case SQLITE -> createSqliteStatePersistence();
            case FILE -> createFileStatePersistence();
            case POSTGRES -> createPostgresPersistence();
        };
        final RequestExecutor requestExecutor = new DefaultConfig().createRequestExecutor();
        return new TreeNodeProcessor(ldesMetaData, statePersistence, requestExecutor);
    }

    private PostgreSQLContainer startPostgresContainer() {
        PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:11.1")
                .withDatabaseName("integration-test-client-persistence")
                .withUsername("sa")
                .withPassword("sa");
        postgreSQLContainer.start();
        return postgreSQLContainer;
    }

    private StatePersistence createFileStatePersistence() {
        MemberRepository memberRepository = MemberRepositoryFactory.getMemberRepository(StatePersistenceStrategy.FILE, Map.of());
        TreeNodeRecordRepository treeNodeRecordRepository = TreeNodeRecordRepositoryFactory
                .getTreeNodeRecordRepository(StatePersistenceStrategy.FILE, Map.of());
        return new StatePersistence(memberRepository, treeNodeRecordRepository);
    }

    private StatePersistence createSqliteStatePersistence() {
        MemberRepository memberRepository = MemberRepositoryFactory.getMemberRepository(StatePersistenceStrategy.SQLITE, Map.of());
        TreeNodeRecordRepository treeNodeRecordRepository = TreeNodeRecordRepositoryFactory
                .getTreeNodeRecordRepository(StatePersistenceStrategy.SQLITE, Map.of());
        return new StatePersistence(memberRepository, treeNodeRecordRepository);
    }

    private StatePersistence createPostgresPersistence() {
        final PostgreSQLContainer postgreSQLContainer = startPostgresContainer();

        PostgresProperties postgresProperties = new PostgresProperties(postgreSQLContainer.getJdbcUrl(),
                postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword(), false);
        MemberRepository memberRepository = MemberRepositoryFactory.getMemberRepository(StatePersistenceStrategy.POSTGRES,
                postgresProperties.getProperties());
        TreeNodeRecordRepository treeNodeRecordRepository = TreeNodeRecordRepositoryFactory
                .getTreeNodeRecordRepository(StatePersistenceStrategy.POSTGRES, postgresProperties.getProperties());

        return new StatePersistence(memberRepository, treeNodeRecordRepository);
    }

    private StatePersistence createInMemoryStatePersistence() {
        MemberRepository memberRepository = MemberRepositoryFactory.getMemberRepository(StatePersistenceStrategy.MEMORY, Map.of());
        TreeNodeRecordRepository treeNodeRecordRepository = TreeNodeRecordRepositoryFactory
                .getTreeNodeRecordRepository(StatePersistenceStrategy.MEMORY, Map.of());
        return new StatePersistence(memberRepository, treeNodeRecordRepository);
    }
}
