package ldes.client.treenodesupplier;

import ldes.client.treenodefetcher.TreeNodeFetcher;
import ldes.client.treenodefetcher.domain.entities.TreeNode;
import ldes.client.treenodesupplier.domain.entities.MemberRecord;
import ldes.client.treenodesupplier.domain.valueobject.SuppliedMember;
import ldes.client.treenodesupplier.domain.entities.TreeNodeRecord;
import ldes.client.treenodesupplier.domain.valueobject.Ldes;
import ldes.client.treenodesupplier.domain.valueobject.TreeNodeStatus;
import ldes.client.treenodesupplier.repository.MemberRepository;
import ldes.client.treenodesupplier.repository.TreeNodeRecordRepository;

import java.util.Optional;

public class TreeNodeProcessor {

	private final TreeNodeRecordRepository treeNodeRecordRepository;
	private final MemberRepository memberRepository;
	private final TreeNodeFetcher treeNodeFetcher;
	private final boolean keepState;
	private final Ldes ldes;

	public TreeNodeProcessor(Ldes ldes,
			TreeNodeRecordRepository treeNodeRecordRepository,
			MemberRepository memberRepository,
			TreeNodeFetcher treeNodeFetcher, boolean keepState) {
		this.treeNodeRecordRepository = treeNodeRecordRepository;
		this.memberRepository = memberRepository;
		this.treeNodeFetcher = treeNodeFetcher;
		this.keepState = keepState;
		this.treeNodeRecordRepository.saveTreeNodeRecord(new TreeNodeRecord(ldes.getStartingNodeUrl()));
		this.ldes = ldes;
		Runtime.getRuntime().addShutdownHook(new Thread(this::destroyState));
	}

	private void processedTreeNode() {
		TreeNodeRecord treeNodeRecord = treeNodeRecordRepository
				.getOneTreeNodeRecordWithStatus(TreeNodeStatus.NOT_VISITED).orElseGet(
						() -> treeNodeRecordRepository.getOneTreeNodeRecordWithStatus(TreeNodeStatus.MUTABLE_AND_ACTIVE)
								.orElseThrow(() -> new RuntimeException(
										"No fragments to mutable or new fragments to process -> LDES ends.")));
		TreeNode treeNodeResponse = treeNodeFetcher
				.fetchTreeNode(ldes.createRequest(treeNodeRecord.getTreeNodeUrl()));
		treeNodeRecord.updateStatus(treeNodeResponse.getMutabilityStatus());
		treeNodeRecordRepository.saveTreeNodeRecord(treeNodeRecord);
		treeNodeResponse.getRelations()
				.stream()
				.filter(treeNodeId -> !treeNodeRecordRepository.existsById(treeNodeId))
				.map(TreeNodeRecord::new)
				.forEach(treeNodeRecordRepository::saveTreeNodeRecord);
		treeNodeResponse.getMembers()
				.stream()
				.map(treeMember -> new MemberRecord(treeMember.getMemberId(), treeMember.getModel()))
				.filter(member -> !memberRepository.isProcessed(member))
				.forEach(memberRepository::saveTreeMember);

	}

	public SuppliedMember getMember() {
		Optional<MemberRecord> unprocessedTreeMember = memberRepository.getUnprocessedTreeMember();
		while (unprocessedTreeMember.isEmpty()) {
			processedTreeNode();
			unprocessedTreeMember = memberRepository.getUnprocessedTreeMember();
		}
		MemberRecord treeMember = unprocessedTreeMember.get();
		SuppliedMember suppliedMember = treeMember.createSuppliedMember();
		treeMember.processedMemberRecord();
		memberRepository.saveTreeMember(treeMember);
		return suppliedMember;
	}

	public void destroyState() {
		if (!keepState) {
			memberRepository.destroyState();
			treeNodeRecordRepository.destroyState();
		}
	}
}
