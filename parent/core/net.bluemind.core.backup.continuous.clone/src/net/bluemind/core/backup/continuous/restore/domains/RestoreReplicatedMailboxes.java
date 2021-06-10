package net.bluemind.core.backup.continuous.restore.domains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.config.Token;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.syncclient.SyncClientOIO;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.Server;

public class RestoreReplicatedMailboxes {

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

	public void restore(DataElement de) {
		if (de.payload.length == 0) {
			return;
		}
		String ownerUid = de.key.owner.split("/")[0];
		ItemValue<Mailbox> mbox = state.getMailbox(ownerUid);
		ItemValue<Server> imap = state.getServer(mbox.value.dataLocation);
		CyrusPartition partition = CyrusPartition.forServerAndDomain(imap, domain.uid);
		ItemValue<MailboxReplica> replica = mrReader.read(new String(de.payload));

//		ordered.put(replica.uid, replica);
		state.storeReplica(domain, mbox, replica, partition);

		try (SyncClientOIO sc = new SyncClientOIO(imap.value.address(), 2502)) {
			sc.authenticate("admin0", Token.admin0());

			String box = cyrusMbox(domain, mbox, replica);
			String cmd = "APPLY MAILBOX %(UNIQUEID " + replica.uid + " MBOXNAME \"" + box + "\" ";
			cmd += "SYNC_CRC 0 SYNC_CRC_ANNOT 0 LAST_UID 0 HIGHESTMODSEQ " + replica.value.highestModSeq
					+ " RECENTUID 0 ";
			cmd += "RECENTTIME 0 LAST_APPENDDATE 0 POP3_LAST_LOGIN 0 POP3_SHOW_AFTER 0 UIDVALIDITY "
					+ replica.value.uidValidity + " ";
			cmd += "PARTITION " + partition.name + " ";
			cmd += "ACL \"" + mbox.value.name + "@" + domain.uid + " lrswipkxtecdan \" ";
			cmd += "OPTIONS PS RECORD ())\r\n";
			String syncRes = sc.run(cmd);
			logger.info("APPLY MAILBOX {} aka {} => {}", replica.uid, syncRes);

		} catch (Exception e) {
			e.printStackTrace();
			monitor.log("ERROR: " + e.getMessage());
		}
	}

	private String cyrusMbox(ItemValue<Domain> domain, ItemValue<Mailbox> box, ItemValue<MailboxReplica> repl) {
		String fn = repl.value.fullName;
		if (box.value.type.sharedNs) {
			if (fn.equals(box.value.name)) {
				fn = "";
			} else {
				fn = "." + UTF7Converter.encode(fn.replace('.', '^').replace('/', '.'));
			}
		} else {
			if (fn.equals("INBOX")) {
				fn = "";
			} else {
				fn = "." + UTF7Converter.encode(fn.replace('.', '^').replace('/', '.'));
			}
		}
		return domain.uid + "!" + box.value.type.nsPrefix + box.value.name.replace('.', '^') + fn;
	}
}
