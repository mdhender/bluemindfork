package net.bluemind.core.backup.continuous.restore.domains;

import net.bluemind.backend.cyrus.internal.CyrusUniqueIds;
import net.bluemind.backend.cyrus.replication.client.ReplMailbox;
import net.bluemind.core.backup.continuous.restore.mbox.UidDatalocMapping.Replica;
import net.bluemind.imap.Acl;

public class RestoreReplicated {

	protected ReplMailbox buildReplicatedMailbox(Replica replica) {
		ReplMailbox.Builder builder = ReplMailbox.builder();
		String mailboxName = replica.mbox.value.name;
		String folderName = replica.folder.value.fullName;
		boolean isUser = !replica.mbox.value.type.sharedNs;
		builder.mailboxName(mailboxName)//
				.domainUid(replica.dom.uid)//
				.mailboxUid(replica.mbox.uid)//
				.partition(replica.part)//
				.acl("admin0", Acl.ALL);
		boolean isRoot = (isUser && "INBOX".equals(folderName)) || (!isUser && mailboxName.equals(folderName));
		if (isRoot) {
			builder.root();
		}
		if (!isUser) {
			builder.sharedNs().folderName(folderName);
			builder.acl("anyone", Acl.POST);
			builder.uniqueId(CyrusUniqueIds.forMailbox(replica.dom.uid, replica.mbox, folderName));
		} else {
			builder.folderName(folderName);
			builder.acl(replica.mbox.value.name + "@" + replica.dom.uid, Acl.ALL);
			builder.uniqueId(CyrusUniqueIds.forMailbox(replica.dom.uid, replica.mbox, folderName));
		}
		return builder.build();
	}

}
