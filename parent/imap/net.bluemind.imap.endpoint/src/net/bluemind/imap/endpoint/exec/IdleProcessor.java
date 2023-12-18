/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.exec;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.SessionState;
import net.bluemind.imap.endpoint.cmd.IdleCommand;
import net.bluemind.imap.endpoint.driver.ImapIdSet;
import net.bluemind.imap.endpoint.driver.MailPart;
import net.bluemind.imap.endpoint.driver.MailPartBuilder;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.driver.SelectedMessage;
import net.bluemind.imap.endpoint.locks.ISequenceCheckpoint;
import net.bluemind.imap.endpoint.locks.MailboxSequenceLocks;
import net.bluemind.imap.endpoint.locks.MailboxSequenceLocks.MailboxSeqLock;
import net.bluemind.imap.endpoint.locks.MailboxSequenceLocks.OpCompletionListener;
import net.bluemind.lib.vertx.Result;

public class IdleProcessor extends AuthenticatedCommandProcessor<IdleCommand> {

	private static final Logger logger = LoggerFactory.getLogger(IdleProcessor.class);

	private final ISequenceCheckpoint idleCheckpointer = new ISequenceCheckpoint() {
	};
	private final List<MailPart> parts = List.of(MailPartBuilder.named("FLAGS"), MailPartBuilder.named("UID"));

	@Override
	public void checkedOperation(IdleCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		ctx.idlingTag(command.raw().tag());
		ctx.state(SessionState.IDLING);
		MailboxConnection mailbox = ctx.mailbox();
		try {
			logger.info("Monitoring {}", ctx.selected());
			ctx.write("+ idling\r\n");
			onChange(command, ctx, mailbox, new SelectedMessage[0]).whenComplete((v, ex) -> {
				if (ex != null) {
					ctx.write(command.raw().tag() + " NO unknown error: " + ex.getMessage() + "\r\n");
					completed.handle(Result.fail(ex));
				} else {
					mailbox.idleMonitor(ctx.selected(),
							(SelectedMessage[] changed) -> onChange(command, ctx, mailbox, changed));
					completed.handle(Result.success());
				}
			});
		} catch (Exception e) {
			ctx.write(command.raw().tag() + " NO unknown error: " + e.getMessage() + "\r\n");
			completed.handle(Result.fail(e));
		}
	}

	private CompletableFuture<Void> onChange(IdleCommand command, ImapContext ctx, MailboxConnection mailbox,
			SelectedMessage[] changed) {
		MailboxSeqLock lockSupport = MailboxSequenceLocks.forMailbox(ctx.selected());
		return lockSupport.withReadLock(ctx.vertxContext, IdleProcessor.this)
				.thenCompose(lockedOp -> lockedCheckpoint(command, ctx, mailbox, changed, lockedOp));
	}

	@SuppressWarnings("deprecation")
	private CompletableFuture<Void> lockedCheckpoint(IdleCommand command, ImapContext ctx, MailboxConnection mailbox,
			SelectedMessage[] changed, OpCompletionListener lockedOp) {
		CompletableFuture<Void> cpComplete = new CompletableFuture<>();
		ctx.vertxContext.executeBlocking((Promise<Void> prom) -> idleCheckpoint(command, ctx, mailbox, changed, prom),
				true, ar -> {
					if (ar.failed()) {
						logger.error("{} idle error", command.raw().tag(), ar.cause());
					}
					ctx.vertxContext.runOnContext(v -> {
						lockedOp.complete();
						cpComplete.complete(null);
					});
				});
		return cpComplete;
	}

	private void idleCheckpoint(IdleCommand command, ImapContext ctx, MailboxConnection mailbox,
			SelectedMessage[] changed, Promise<Void> prom) {
		StringBuilder sb = new StringBuilder();
		idleCheckpointer.checkpointSequences(logger, "idle", sb, ctx);
		SelectedFolder live = ctx.selected();
		ctx.write(sb.toString());
		logger.debug("idle checkpoint for {}", live);
		if (changed.length > 0) {
			FetchedItemStream fetchStream = new FetchedItemStream(ctx, command.raw().tag() + " idle", parts);
			ImapIdSet changeSet = ImapIdSet.uids(
					Arrays.stream(changed).map(sm -> Long.toString(sm.imapUid())).collect(Collectors.joining(",")));
			mailbox.fetch(live, changeSet, parts, fetchStream).whenComplete((done, ex) -> {
				if (ex != null) {
					prom.fail(ex);
				} else {
					prom.complete();
				}
			});
		} else {
			prom.complete();
		}
	}

	@Override
	public Class<IdleCommand> handledType() {
		return IdleCommand.class;
	}

}
