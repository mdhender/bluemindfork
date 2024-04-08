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

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.StopProcessingException;
import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;
import net.bluemind.imap.endpoint.cmd.RawCommandAnalyzer;
import net.bluemind.imap.endpoint.cmd.RawImapCommand;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.locks.ISequenceReader;
import net.bluemind.imap.endpoint.locks.ISequenceWriter;
import net.bluemind.imap.endpoint.locks.MailboxSequenceLocks;
import net.bluemind.imap.endpoint.locks.MailboxSequenceLocks.MailboxSeqLock;
import net.bluemind.imap.endpoint.locks.MailboxSequenceLocks.OpCompletionListener;

public class ImapCommandHandler implements Handler<RawImapCommand> {

	private static final Logger logger = LoggerFactory.getLogger(ImapCommandHandler.class);

	private final RawCommandAnalyzer anal;
	private final ImapContext ctx;
	private CompletableFuture<?> parentOp;

	public ImapCommandHandler(ImapContext ctx) {
		this.ctx = ctx;
		this.anal = new RawCommandAnalyzer();
		this.parentOp = null;
	}

	@Override
	public void handle(RawImapCommand event) {
		ctx.clientCommand(event);
		try {
			analyze(event);
		} catch (Exception ere) {
			logger.error("server error during analyze", ere);
			ctx.write(event.tag() + " BAD " + ere.getMessage() + "\r\n");
		}
	}

	private void analyze(RawImapCommand event) {
		AnalyzedCommand analyzedCmd = anal.analyze(ctx, event);
		if (analyzedCmd == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Command '{}' is not analyzed.", event.cmd());
			}
			ctx.write(event.tag() + " BAD Missing analyzer\r\n");
		} else {
			CommandProcessor<?> proc = Processors.get(analyzedCmd.getClass());
			if (proc == null) {
				if (logger.isWarnEnabled()) {
					logger.warn("Command '{}' has no processor.", event.cmd());
				}
				ctx.write(event.tag() + " BAD Missing processor for command\r\n");
			} else {
				processCommand(proc, analyzedCmd);
			}
		}
	}

	private void processCommand(CommandProcessor<?> proc, AnalyzedCommand analyzedCmd) {
		// we chain the promises to ensure correct execution ordering with async
		// processors (eg. Fetch). Completion is done on vertx context so "isDone" can't
		// change while testing it here.
		if (parentOp == null || parentOp.isDone()) {
			parentOp = blockingCall(proc, analyzedCmd);
		} else {
			parentOp = parentOp.thenCompose(v -> blockingCall(proc, analyzedCmd));
		}
	}

	private CompletableFuture<Void> blockingCall(CommandProcessor<?> proc, AnalyzedCommand analyzedCmd) {
		String lockReason = proc.toString() + "{" + analyzedCmd.raw().tag() + "}";
		return grabRequiredLock(proc, analyzedCmd, lockReason)
				.thenCompose(opListener -> blockingCallLocked(opListener, proc, analyzedCmd))
				.thenAccept(OpCompletionListener::complete);
	}

	@SuppressWarnings("deprecation")
	private CompletableFuture<OpCompletionListener> blockingCallLocked(OpCompletionListener op,
			CommandProcessor<?> proc, AnalyzedCommand analyzedCmd) {
		// we need the deprecated form of execute blocking as processors invoke blocking
		// API calls but will complete in an asynchronous way
		CompletableFuture<OpCompletionListener> comp = new CompletableFuture<>();

		ctx.vertxContext.executeBlocking((Promise<Void> prom) -> {
			Stopwatch chrono = Stopwatch.createStarted();
			proc.process(analyzedCmd, ctx, over -> {
				if (over.failed()) {
					prom.fail(over.cause());
				} else {
					prom.complete();
					long ms = chrono.elapsed(TimeUnit.MILLISECONDS);
					logger.info("[{}] {} {} execution took {}ms.", ctx, analyzedCmd.raw().tag(), proc, ms);
				}
			});
		}, true, ar -> {
			if (ar.failed()) {
				Throwable t = ar.cause();
				if (t instanceof CompletionException ce) {
					t = ce.getCause();
				}
				if (t instanceof StopProcessingException stop) {
					ctx.vertxContext.runOnContext(v -> {
						// unlock & stop processing further commands
						op.complete();
						logger.info("Abort processing after {}", stop.getMessage());
						comp.completeExceptionally(stop);

					});
					return;
				}
				if (!(t instanceof ClosedChannelException)) {
					logger.error("Command {} - {} processing failed", analyzedCmd.raw().tag(), proc, t);
				}
			}
			ctx.vertxContext.runOnContext(v -> comp.complete(op));
		});
		return comp;
	}

	private CompletableFuture<OpCompletionListener> grabRequiredLock(CommandProcessor<?> proc, AnalyzedCommand cmd,
			Object reason) {
		if (proc instanceof ISequenceWriter writer) {
			// copy command writes something in folder B but reads from A. We would need 2
			// locks...
			SelectedFolder written = writer.modifiedFolder(cmd, ctx);
			MailboxSeqLock locks = MailboxSequenceLocks.forMailbox(written);
			return locks.withWriteLock(ctx.vertxContext, reason);
		}
		if (proc instanceof ISequenceReader reader) {
			SelectedFolder read = reader.readFolder(cmd, ctx);
			MailboxSeqLock locks = MailboxSequenceLocks.forMailbox(read);
			return locks.withReadLock(ctx.vertxContext, reason);
		}

		return CompletableFuture.completedFuture(() -> {
		});
	}

	public void close() {
		// nexus is closed by ImapSession
	}

}
