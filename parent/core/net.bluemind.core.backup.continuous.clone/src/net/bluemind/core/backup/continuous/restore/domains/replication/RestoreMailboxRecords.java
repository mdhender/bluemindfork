package net.bluemind.core.backup.continuous.restore.domains.replication;

import java.io.IOException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.backend.cyrus.replication.client.ReplMailbox;
import net.bluemind.backend.cyrus.replication.client.SyncClientOIO;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.service.internal.MailboxRecordItemCache;
import net.bluemind.config.Token;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.restore.CloneException;
import net.bluemind.core.backup.continuous.restore.domains.RestoreDomainType;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.backup.continuous.restore.mbox.MsgBodyTask;
import net.bluemind.core.backup.continuous.restore.mbox.UidDatalocMapping.Replica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.server.api.Server;

public class RestoreMailboxRecords extends RestoreReplicated implements RestoreDomainType {

	private final ValueReader<ItemValue<MailboxRecord>> recReader = JsonUtils
			.reader(new TypeReference<ItemValue<MailboxRecord>>() {
			});

	private final RestoreLogger log;
	private final ISdsSyncStore sdsStore;
	private final RestoreState state;

	public RestoreMailboxRecords(RestoreLogger log, ISdsSyncStore sdsStore, RestoreState state) {
		this.log = log;
		this.sdsStore = sdsStore;
		this.state = state;
	}

	@Override
	public String type() {
		return IMailReplicaUids.MAILBOX_RECORDS;
	}

	@Override
	public void restore(RecordKey key, String payload) {
		String uniqueId = IMailReplicaUids.getUniqueId(key.uid);
		Replica repl = state.getReplica(uniqueId);
		if (repl == null) {
			log.filter(type(), key);
			return;
		}

		ItemValue<Server> server = state.getServer(repl.part.serverUid);
		String ip = (server != null) ? server.value.ip : "127.0.0.1";
		try (SyncClientOIO sync = new SyncClientOIO(ip, 2502)) {
			sync.authenticate("admin0", Token.admin0());
			log.applyMailbox(type(), key);
			ItemValue<MailboxRecord> rec = recReader.read(payload);
			MailboxRecordItemCache.store(uniqueId, rec);
			MsgBodyTask bodyTask = new MsgBodyTask(sdsStore, sync, repl);
			try {
				int len = bodyTask.run(log.monitor(), rec.value.messageBody);
				state.storeBodySize(rec.value.messageBody, len);
			} catch (Exception e) {
				throw new CloneException(e);
			}
			repl.appliedUid = Math.max(repl.appliedUid, rec.value.imapUid);
			repl.appliedModseq = Math.max(repl.appliedModseq, rec.value.modSeq);

			ReplMailbox replicatedMbox = buildReplicatedMailbox(repl);

			// %(UID 1 MODSEQ 305 LAST_UPDATED 1619172582 FLAGS () INTERNALDATE 1619169573
			// SIZE 54 GUID 3a6785fe8081d403c6721ae8637c0016db7963f8)
			StringBuilder recordsBuffer = new StringBuilder();
			recordsBuffer.append("%(");
			recordsBuffer.append("UID ").append(rec.value.imapUid);
			recordsBuffer.append(" MODSEQ ").append(rec.value.modSeq);
			recordsBuffer.append(" LAST_UPDATED ").append(rec.value.lastUpdated.getTime() / 1000);
			recordsBuffer.append(" FLAGS (").append(flags(rec)).append(")");
			recordsBuffer.append(" INTERNALDATE ").append(rec.value.internalDate.getTime() / 1000);
			recordsBuffer.append(" SIZE ").append(state.getBodySize(rec.value.messageBody));
			recordsBuffer.append(" GUID " + rec.value.messageBody).append(")");

			String recBuf = recordsBuffer.toString();
			String syncResponse = sync.applyMailbox(replicatedMbox, recBuf);
			if (!syncResponse.startsWith("OK")) {
				String message = String.format("Failed to apply mailbox. syncResponse:%s, command:%s", syncResponse,
						replicatedMbox.applyMailboxCommand(recBuf));
				throw new CloneException(message);
			}
		} catch (IOException e) {
			throw new CloneException(e);
		}

	}

	private String flags(ItemValue<MailboxRecord> rec) {
		return rec.value.flags.stream().map(itf -> itf.flag).collect(Collectors.joining(" "));
	}

}
