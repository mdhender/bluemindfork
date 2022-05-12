package net.bluemind.core.backup.continuous.restore.domains.replication;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.client.ReplMailbox;
import net.bluemind.backend.cyrus.replication.client.SyncClientOIO;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.RecordKey.Operation;
import net.bluemind.core.backup.continuous.restore.CloneException;
import net.bluemind.core.backup.continuous.restore.domains.RestoreDomainType;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.backup.continuous.restore.mbox.UidDatalocMapping.Replica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.Server;

public class RestoreReplicatedMailboxes extends RestoreReplicated implements RestoreDomainType {

	private static final ValueReader<ItemValue<MailboxReplica>> mrReader = JsonUtils
			.reader(new TypeReference<ItemValue<MailboxReplica>>() {
			});

	private final RestoreLogger log;
	private final ItemValue<Domain> domain;
	private final RestoreState state;

	public RestoreReplicatedMailboxes(RestoreLogger log, ItemValue<Domain> domain, RestoreState state) {
		this.log = log;
		this.domain = domain;
		this.state = state;
	}

	@Override
	public String type() {
		return IMailReplicaUids.REPLICATED_MBOXES;
	}

	@Override
	public void restore(RecordKey key, String payload) {
		if (Operation.isDelete(key)) {
			log.filter(type(), key);
			return;
		}

		String ownerUid = key.owner.split("/")[0];
		ItemValue<Mailbox> mbox = state.getMailbox(ownerUid);
		if (mbox == null) {
			log.skip(type(), key, payload);
			return;
		}
		ItemValue<Server> imap = state.getServer(mbox.value.dataLocation);
		CyrusPartition partition = CyrusPartition.forServerAndDomain(imap, domain.uid);
		ItemValue<MailboxReplica> replica = mrReader.read(payload);

		Replica repl = state.storeReplica(domain, mbox, replica, partition);
		ReplMailbox replicatedMbox = buildReplicatedMailbox(repl);

		try (SyncClientOIO syncClient = SyncClientPools.getClient(imap.value.address(), 2502)) {
			log.applyMailbox(type(), key);
			String syncResponse = syncClient.applyMailbox(replicatedMbox);
			if (!syncResponse.startsWith("OK")) {
				String message = String.format("Failed to apply mailbox. syncResponse:%s, command:%s", syncResponse,
						replicatedMbox.applyMailboxCommand());
				throw new CloneException(message);
			}
		} catch (IOException e) {
			throw new CloneException(e);
		}
	}
}
