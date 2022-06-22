/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.replication.server.cmd;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.protocol.parsing.JsUtils;
import net.bluemind.backend.cyrus.replication.protocol.parsing.ParenObjectParser;
import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.cyrus.replication.server.state.DtoConverters;
import net.bluemind.backend.cyrus.replication.server.state.MailboxFolder;
import net.bluemind.backend.cyrus.replication.server.state.MboxRecord;
import net.bluemind.backend.cyrus.replication.server.state.MboxRecord.MessageRecordBuilder;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationState;
import net.bluemind.backend.mail.replica.api.MailApiAnnotations;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecordAnnotation;
import net.bluemind.lib.vertx.VertxPlatform;

/**
 * APPLY MAILBOX %(UNIQUEID 002647c6582c5f46 MBOXNAME ex2016.vmw!user.nico
 * SYNC_CRC 2950253899 SYNC_CRC_ANNOT 0 LAST_UID 7 HIGHESTMODSEQ 25 RECENTUID 7
 * RECENTTIME 1481733869 LAST_APPENDDATE 1481733763 POP3_LAST_LOGIN 0
 * POP3_SHOW_AFTER 0 UIDVALIDITY 1479302982 PARTITION ex2016_vmw ACL "admin0
 * lrswipkxtecda 147CAED2-F9AA-4B66-984D-94109FD4DBDE@ex2016.vmw lrsp
 * 526BE6B3-281B-4B78-BCCC-CBB7606CF2DA@ex2016.vmw lrswipkxtecda nico@ex2016.vmw
 * lrswipkxtecda " OPTIONS P RECORD (%(UID 2 MODSEQ 20 LAST_UPDATED 1482931800
 * FLAGS () INTERNALDATE 1481703222 SIZE 6454 GUID
 * c32c8d6e83553db2d9905f19f65c918d896416fa) %(UID 3 MODSEQ 21 LAST_UPDATED
 * 1482931800 FLAGS () INTERNALDATE 1481707761 SIZE 2230 GUID
 * 7e4088434bca20ee5a8d822dd136da4339d9ca12) %(UID 4 MODSEQ 22 LAST_UPDATED
 * 1482931800 FLAGS () INTERNALDATE 1481711078 SIZE 1341 GUID
 * 8eceb45707ebc5a40032f1cd62a6da2f8acd4e92) %(UID 5 MODSEQ 23 LAST_UPDATED
 * 1482931800 FLAGS () INTERNALDATE 1481715552 SIZE 7136 GUID
 * b2d40dd7fc4a67016d951899edc41205d7f8beea) %(UID 6 MODSEQ 24 LAST_UPDATED
 * 1482931800 FLAGS (\Seen) INTERNALDATE 1481719363 SIZE 6879 GUID
 * f1b6bb899366de8b97446ec17a18ccf419c64756) %(UID 7 MODSEQ 25 LAST_UPDATED
 * 1482931800 FLAGS (\Seen) INTERNALDATE 1481733763 SIZE 2199 GUID
 * be5be50f875ae0b710469f58aecf2f8b6c01eb56)))
 * 
 *
 */
public class ApplyMailbox implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(ApplyMailbox.class);
	private final List<IReplicationObserver> observers;

	public ApplyMailbox(List<IReplicationObserver> observers) {
		this.observers = observers;
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		String withVerb = t.value();
		String mboxAndContent = withVerb.substring("APPLY MAILBOX ".length());
		ParenObjectParser parser = ParenObjectParser.create();
		JsonObject parsed = parser.parse(mboxAndContent).asObject();
		ReplicationState state = session.state();
		MailboxFolder folder = MailboxFolder.of(parsed);

		String partition = Token.atomOrValue(parsed.getString("PARTITION"));
		if ("default".equals(partition)) {
			logger.warn("Skip ApplyMailbox on '{}' partition", partition);
			CompletableFuture<CommandResult> ret = new CompletableFuture<>();
			ret.complete(CommandResult.success());
			return ret;
		}

		return state.registerFolder(folder).thenCompose(v1 -> {
			JsonArray emails = parsed.getJsonArray("RECORD");
			int len = emails.size();

			final List<MailboxRecord> mboxState = new LinkedList<>();
			for (int i = 0; i < len; i++) {
				JsonObject mailRecord = emails.getJsonObject(i);
				String guid = mailRecord.getString("GUID");
				long recordUid = Long.parseLong(mailRecord.getString("UID"));
				MessageRecordBuilder builder = MboxRecord.builder();
				builder.body(guid);
				builder.uid(recordUid);
				builder.modseq(Long.parseLong(mailRecord.getString("MODSEQ")));
				builder.internalDate(Long.parseLong(mailRecord.getString("INTERNALDATE")));
				builder.lastUpdated(Long.parseLong(mailRecord.getString("LAST_UPDATED")));
				builder.flags(JsUtils.asList(mailRecord.getJsonArray("FLAGS"), (String in) -> in));
				if (mailRecord.containsKey("ANNOTATIONS")) {
					builder.annotations(JsUtils.asList(mailRecord.getJsonArray("ANNOTATIONS"), (JsonObject obj) -> {
						MailboxRecordAnnotation mra = MailboxRecordAnnotation.of(obj);
						mra.value = Token.atomOrValue(mra.value);
						if (MailApiAnnotations.MSG_META.equals(mra.entry)) {
							VertxPlatform.eventBus().publish(MailApiAnnotations.MSG_ANNOTATION_BUS_TOPIC, mra.value);
						}
						return mra;
					}));
				}
				mboxState.add(DtoConverters.from(builder.build()));
			}
			if (logger.isDebugEnabled()) {
				logger.debug("[{}] Updating {} record(s)...", folder.getUniqueId(), mboxState.size());
			}
			return state.updateRecords(folder.getUniqueId(), mboxState);
		}).thenApply(v -> {
			observers.stream().forEach(obs -> obs.onApplyMailbox(folder.getUniqueId(), folder.getLastUid()));
			return CommandResult.success();
		});
	}

}
