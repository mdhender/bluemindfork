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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import net.bluemind.imap.endpoint.EndpointRuntimeException;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;
import net.bluemind.imap.endpoint.cmd.RawCommandAnalyzer;
import net.bluemind.imap.endpoint.cmd.RawImapCommand;

public class ImapCommandHandler implements Handler<RawImapCommand> {

	private static final Logger logger = LoggerFactory.getLogger(ImapCommandHandler.class);

	private final RawCommandAnalyzer anal;

	private final ImapContext ctx;

	public ImapCommandHandler(ImapContext ctx) {
		this.ctx = ctx;
		this.anal = new RawCommandAnalyzer();
	}

	@Override
	public void handle(RawImapCommand event) {
		logger.info("C: {}", event);
		try {
			analyze(event);
		} catch (EndpointRuntimeException ere) {
			ctx.write(event.tag() + " BAD " + ere.getMessage() + "\r\n");
		}
	}

	private void analyze(RawImapCommand event) {
		AnalyzedCommand analyzedCmd = anal.analyze(event);
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
		Stopwatch chrono = Stopwatch.createStarted();
		ctx.vertx().executeBlocking((Promise<Void> prom) -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Processing {}", analyzedCmd);
			}
			proc.process(analyzedCmd, ctx, ar -> {
				if (ar.failed()) {
					prom.fail(ar.cause());
				} else {
					prom.complete();
				}
			});
		}, true, (AsyncResult<Void> ar) -> {
			if (ar.failed()) {
				logger.error("Command {} processing failed", analyzedCmd, ar.cause());
			}
			long ms = chrono.elapsed(TimeUnit.MILLISECONDS);
			logger.info("[{}] {} execution took {}ms.", ctx, proc, ms);
		});
	}

	public void close() {
		// nexus is closed by ImapSession
	}

}
