package ldes.client.treenodesupplier.repository.sql;

import be.vlaanderen.informatievlaanderen.ldes.ldi.EntityManagerFactory;
import be.vlaanderen.informatievlaanderen.ldes.ldi.entities.MemberVersionRecordEntity;
import ldes.client.treenodesupplier.domain.entities.MemberVersionRecord;
import ldes.client.treenodesupplier.repository.MemberVersionRepository;
import ldes.client.treenodesupplier.repository.mapper.MemberVersionRecordEntityMapper;

import javax.persistence.EntityManager;

public class SqlMemberVersionRepository implements MemberVersionRepository {

    private final EntityManagerFactory entityManagerFactory;
    private final EntityManager entityManager;
    private final String instanceName;

    public SqlMemberVersionRepository(EntityManagerFactory entityManagerFactory,
                                      String instanceName) {
        this.entityManagerFactory = entityManagerFactory;
        this.entityManager = entityManagerFactory.getEntityManager();
        this.instanceName = instanceName;
    }

    @Override
    public void addMemberVersion(MemberVersionRecord memberVersion) {
        entityManager.merge(MemberVersionRecordEntityMapper.fromMemberVersionRecord(memberVersion));
    }

    @Override
    public boolean isVersionAfterTimestamp(MemberVersionRecord memberVersion) {
        return entityManager.createNamedQuery("MemberVersion.findMemberVersionAfterTimestamp", MemberVersionRecordEntity.class)
                .setParameter("versionOf", memberVersion.getVersionOf())
                .setParameter("timestamp", memberVersion.getTimestamp())
                .getResultStream()
                .findFirst()
                .isEmpty();
    }

    @Override
    public void destroyState() {
        entityManagerFactory.destroyState(instanceName);
    }

}
