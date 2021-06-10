package net.bluemind.core.backup.continuous.restore.domains;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.config.Token;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.CloneException;
import net.bluemind.core.backup.continuous.restore.mbox.MsgBodyTask;
import net.bluemind.core.backup.continuous.restore.mbox.UidDatalocMapping.Replica;
import net.bluemind.core.backup.continuous.syncclient.SyncClientOIO;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.server.api.Server;

public class RestoreMailboxRecords {

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

	public void restore(DataElement de) {
		monitor.log("Restoring a mailbox record: " + de);

		String uniqueId = IMailReplicaUids.getUniqueId(de.key.uid);
		Replica repl = state.getReplica(uniqueId);
		if (repl == null) {
			monitor.log("No replica for " + de.key.uid);
			return;
		}

		SyncClientOIO sync;
		try {
			ItemValue<Server> server = state.getServer(repl.part.serverUid);
			String ip = (server != null) ? server.value.ip : "127.0.0.1";
			sync = new SyncClientOIO(ip, 2502);
			sync.authenticate("admin0", Token.admin0());
		} catch (Exception e) {
			throw new CloneException(e);
		}
		ItemValue<MailboxRecord> rec = recReader.read(new String(de.payload));
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

		sync(sync, repl, recordsBuffer.toString());

	}

	private void sync(SyncClientOIO sync, Replica repl, String recStr) throws CloneException {
		try {
			StringBuilder cmd = new StringBuilder(repl.cmdPrefix);
			cmd.append(recStr);
			cmd.append("))\r\n");
			String complete = cmd.toString();
			String syncRes = sync.run(complete);
			monitor.log("Apply 1 record => " + syncRes);
			if (!syncRes.startsWith("OK ")) {
				monitor.log(complete + " failed, abort !!!");
			}
		} catch (IOException e) {
			throw new CloneException(e);
		}
	}

}
