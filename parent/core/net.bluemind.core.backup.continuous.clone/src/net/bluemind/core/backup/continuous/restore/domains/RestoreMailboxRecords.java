package net.bluemind.core.backup.continuous.restore.domains;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.backend.cyrus.replication.client.ReplMailbox;
import net.bluemind.backend.cyrus.replication.client.SyncClientOIO;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.service.internal.MailboxRecordItemCache;
import net.bluemind.config.Token;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.CloneException;
import net.bluemind.core.backup.continuous.restore.mbox.MsgBodyTask;
import net.bluemind.core.backup.continuous.restore.mbox.UidDatalocMapping.Replica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.server.api.Server;

public class RestoreMailboxRecords extends RestoreReplicated implements RestoreDomainType {

	private static final Logger logger = LoggerFactory.getLogger(RestoreMailboxRecords.class);

	private final ValueReader<ItemValue<MailboxRecord>> recReader = JsonUtils
			.reader(new TypeReference<ItemValue<MailboxRecord>>() {
			});

	private final IServerTaskMonitor monitor;
	private final ISdsSyncStore sdsStore;
	private final RestoreState state;

	public RestoreMailboxRecords(IServerTaskMonitor monitor, ISdsSyncStore sdsStore, RestoreState state) {
		this.monitor = monitor;
		this.sdsStore = sdsStore;
		this.state = state;
	}

	public String type() {
		return IMailReplicaUids.MAILBOX_RECORDS;
	}

	public void restore(DataElement de) {
		monitor.log("Processing mailbox record:\n" + de.key + "\n" + new String(de.payload));

		String uniqueId = IMailReplicaUids.getUniqueId(de.key.uid);
		Replica repl = state.getReplica(uniqueId);
		if (repl == null) {
			monitor.log("No replica for " + de.key.uid);
			return;
		}

		ItemValue<Server> server = state.getServer(repl.part.serverUid);
		String ip = (server != null) ? server.value.ip : "127.0.0.1";
		try (SyncClientOIO sync = new SyncClientOIO(ip, 2502)) {
			sync.authenticate("admin0", Token.admin0());
			ItemValue<MailboxRecord> rec = recReader.read(new String(de.payload));
			MailboxRecordItemCache.store(de.key.owner + rec.value.messageBody, rec.item());
			if (!state.containsBody(rec.value.messageBody)) {
				MsgBodyTask bodyTask = new MsgBodyTask(sdsStore, sync, repl);
				try {
					int len = bodyTask.run(monitor, rec.value.messageBody);
					state.storeBodySize(rec.value.messageBody, len);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}

			ReplMailbox replicatedMbox = buildReplicatedMailbox(repl);

			// %(UID 1 MODSEQ 305 LAST_UPDATED 1619172582 FLAGS () INTERNALDATE 1619169573
			// SIZE 54 GUID 3a6785fe8081d403c6721ae8637c0016db7963f8)
			StringBuilder recordsBuffer = new StringBuilder();
			recordsBuffer.append("%(");
			recordsBuffer.append("UID ").append(rec.value.imapUid);
			recordsBuffer.append(" MODSEQ ").append(rec.value.modSeq);
			recordsBuffer.append(" LAST_UPDATED ").append(rec.value.lastUpdated.getTime() / 1000);
			recordsBuffer.append(" FLAGS ()");
			recordsBuffer.append(" INTERNALDATE ").append(rec.value.internalDate.getTime() / 1000);
			recordsBuffer.append(" SIZE ").append(state.getBodySize(rec.value.messageBody));
			recordsBuffer.append(" GUID " + rec.value.messageBody).append(")");

			String syncResponse = sync.applyMailbox(replicatedMbox, recordsBuffer.toString());
//			logger.info("APPLY MAILBOX {} => {}", repl.mbox.uid, syncResponse);
			monitor.log("APPLY MAILBOX aka " + repl.mbox.uid + " => " + syncResponse);
		} catch (IOException e) {
			monitor.log("ERROR: " + e.getMessage());
			throw new CloneException(e);
		}

	}

}
