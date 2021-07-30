package net.bluemind.core.backup.continuous.restore.domains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.client.ReplMailbox;
import net.bluemind.backend.cyrus.replication.client.SyncClientOIO;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.config.Token;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.mbox.UidDatalocMapping.Replica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.Server;

public class RestoreReplicatedMailboxes extends RestoreReplicated implements RestoreDomainType {

	private static final Logger logger = LoggerFactory.getLogger(RestoreReplicatedMailboxes.class);

	private static final ValueReader<ItemValue<MailboxReplica>> mrReader = JsonUtils
			.reader(new TypeReference<ItemValue<MailboxReplica>>() {
			});

	private final IServerTaskMonitor monitor;
	private final ItemValue<Domain> domain;
	private final RestoreState state;

	public RestoreReplicatedMailboxes(IServerTaskMonitor monitor, ItemValue<Domain> domain, RestoreState state) {
		this.monitor = monitor;
		this.domain = domain;
		this.state = state;
	}

	public String type() {
		return "replicated_mailboxes";
	}

	public void restore(DataElement de) {
		if (de.payload.length == 0) {
			return;
		}
		String ownerUid = de.key.owner.split("/")[0];
		ItemValue<Mailbox> mbox = state.getMailbox(ownerUid);
		ItemValue<Server> imap = state.getServer(mbox.value.dataLocation);
		CyrusPartition partition = CyrusPartition.forServerAndDomain(imap, domain.uid);
		ItemValue<MailboxReplica> replica = mrReader.read(new String(de.payload));

		Replica repl = state.storeReplica(domain, mbox, replica, partition);
		ReplMailbox replicatedMbox = buildReplicatedMailbox(repl);

		try (SyncClientOIO syncClient = new SyncClientOIO(imap.value.address(), 2502)) {
			syncClient.authenticate("admin0", Token.admin0());
			String syncResponse = syncClient.applyMailbox(replicatedMbox);
			logger.info("APPLY MAILBOX aka {} => {}", replica.uid, syncResponse);
		} catch (Exception e) {
			e.printStackTrace();
			monitor.log("ERROR: " + e.getMessage());
		}
	}
}
