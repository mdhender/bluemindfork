/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.server;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyActivateSieve;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyAnnotation;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyExpunge;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyMailbox;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyMessage;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyQuota;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyRename;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyReserve;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplySeen;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplySieve;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplySub;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyUnmailbox;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyUnquota;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyUnsieve;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyUnsub;
import net.bluemind.backend.cyrus.replication.server.cmd.ApplyUnuser;
import net.bluemind.backend.cyrus.replication.server.cmd.Authenticate;
import net.bluemind.backend.cyrus.replication.server.cmd.CommandResult;
import net.bluemind.backend.cyrus.replication.server.cmd.GetAnnotation;
import net.bluemind.backend.cyrus.replication.server.cmd.GetFetch;
import net.bluemind.backend.cyrus.replication.server.cmd.GetFullMailbox;
import net.bluemind.backend.cyrus.replication.server.cmd.GetMailboxes;
import net.bluemind.backend.cyrus.replication.server.cmd.GetMeta;
import net.bluemind.backend.cyrus.replication.server.cmd.GetUser;
import net.bluemind.backend.cyrus.replication.server.cmd.IAsyncReplicationCommand;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationState;
import net.bluemind.backend.cyrus.replication.server.state.StorageApiLink;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lib.vertx.utils.CircuitBreaker;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class ReplicationSession {

	private static final Logger logger = LoggerFactory.getLogger(ReplicationSession.class);

	public static final AtomicLong activeSessions = initMetric();

	private static final byte[] CRLF = "\r\n".getBytes();

	private static AtomicLong initMetric() {
		Registry registry = MetricsRegistry.get();
		IdFactory idf = new IdFactory("cyrus-replication", registry, ReplicationSession.class);
		return PolledMeter.using(registry).withId(idf.name("activeSessions")).monitorValue(new AtomicLong());
	}

	private final Vertx vertx;
	private final NetSocket client;
	private final ClientFramesHandler clientFramesParser;
	private final Splitter splitter;
	private final ReplicationState state;
	private final StorageApiLink storage;
	private final long startTime;
	private final CompletableFuture<Void> stopFuture;

	private List<IReplicationObserver> observers;

	private final CircuitBreaker<ReplicationSession> circuitBreaker;

	public ReplicationSession(Vertx vertx, NetSocket client, StorageApiLink storage,
			List<IReplicationObserver> observers) {
		this.vertx = vertx;
		this.storage = storage;
		this.state = new ReplicationState(vertx, storage);
		this.client = client;
		this.clientFramesParser = new ClientFramesHandler(vertx, client, this);
		this.splitter = Splitter.on(' ').omitEmptyStrings();
		this.startTime = System.currentTimeMillis();
		this.stopFuture = new CompletableFuture<>();
		this.observers = observers;
		logger.debug("Created for vertx {}", this.vertx);
		this.circuitBreaker = new CircuitBreaker<>("replication", session -> session.client.writeHandlerID());
	}

	public ReplicationState state() {
		return state;
	}

	public void start() {
		long sessNumber = activeSessions.incrementAndGet();
		logger.info("Starting session {} with {}", sessNumber, client.remoteAddress());
		client.handler(clientFramesParser);
		client.closeHandler(nothing -> {
			long currentSessions = activeSessions.decrementAndGet();
			storage.release();
			logger.info("Socket closed (active: {}) after {}ms.", currentSessions,
					System.currentTimeMillis() - startTime);
			downlink();
			stopFuture.complete(null);
		});
		client.exceptionHandler(t -> logger.error("session {}: {}", sessNumber, t.getMessage(), t));
		client.write(banner("SASL PLAIN", "STARTTLS", "X-REPLICATION"));
		uplink(storage.remoteIp());
	}

	private void uplink(String remoteIp) {
		JsonObject payload = new JsonObject();
		payload.put("master", remoteIp).put("status", "UP");
		VertxPlatform.eventBus().publish("mailreplica.uplink", payload);
	}

	private void downlink() {
		if (activeSessions.get() == 0) {
			JsonObject payload = new JsonObject();
			payload.put("status", "DOWN");
			VertxPlatform.eventBus().publish("mailreplica.uplink", payload);
			logger.info("DownLink notification.");
		}
	}

	private static interface Tag {
		String value();
	}

	private Buffer banner(Tag t, String... capabilities) {
		Buffer banner = Buffer.buffer();
		for (String capa : capabilities) {
			banner.appendString("* ").appendString(capa).appendString("\r\n");
		}
		String ver = Optional.ofNullable(Activator.version()).orElse("1.0.0");
		banner.appendString(t.value() + " OK syncsrv BlueMind sync server v" + ver + "\r\n");
		return banner;
	}

	private Buffer banner(String... capabilities) {
		return banner(() -> "*", capabilities);
	}

	public CompletableFuture<Void> stop() {
		if (!stopFuture.isDone()) {
			client.close();
		} else {
			logger.warn("Already stopped.");
		}
		return stopFuture;
	}

	private CompletableFuture<CommandResult> run(IAsyncReplicationCommand cmd, Token t, ReplicationFrame frame) {
		try {
			return cmd.doIt(this, t, frame).thenApply(result -> {
				circuitBreaker.noticeSuccess(this);
				return result;
			}).exceptionally(error -> {
				circuitBreaker.noticeError(this);
				return CommandResult.error(error);
			});
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return CompletableFuture.completedFuture(CommandResult.no(e.getMessage()));
		}
	}

	private CompletableFuture<Void> write(CommandResult r, ReplicationFrame frame) {
		if (r.hasFullResponse()) {
			String forLog = r.responseString();
			if (frame != null) {
				logger.info("REPL S: {}: {}", frame.frameId(), forLog);
			} else {
				logger.info("REPL S: {}", forLog);
			}
			return circuitBreaker.applyPromised(vertx, this, () -> write(r.responseBuffer()));
		} else {
			String res = r.responseString();
			String forLog = res;
			if (forLog.length() > 500) {
				forLog = forLog.substring(0, 200) + "... [truncated]";
			}
			if (frame != null) {
				logger.info("REPL S: {}: {}", frame.frameId(), forLog);
			} else {
				logger.info("REPL S: {}", forLog);
			}
			return circuitBreaker.applyPromised(vertx, this, () -> write(res));
		}
	}

	private CompletableFuture<Void> write(String s) {
		Buffer buf = Buffer.buffer(s).appendBytes(CRLF);
		return write(buf);
	}

	private CompletableFuture<Void> write(Buffer b) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		client.write(b);
		if (logger.isDebugEnabled()) {
			String asStr = b.toString();
			logger.debug("S:\n{}", asStr.substring(0, asStr.length() - 2));
		}
		if (client.writeQueueFull()) {
			logger.info("Draining...");
			client.drainHandler(v -> {
				logger.info("Drained.");
				ret.complete(null);
			});
		} else {
			ret.complete(null);
		}
		return ret;
	}

	public CompletableFuture<Void> processFrame(ReplicationFrame frame) {
		logger.info("REPL C: {}", frame.toString());
		// FIXME tokens are merged so we split a BIG thing
		Token t = frame.next();
		String cmdString = t.value();
		Iterator<String> verbTokens = splitter.split(cmdString).iterator();
		String verb = verbTokens.next();
		switch (verb) {
		case "C01":
			// imap replication attempt
			return write(CommandResult.bad("invalid"), frame);
		case "EXIT":
			return doExit(frame);
		case "RESTART":
			return doRestart(frame);
		case "NOOP":
			return doNoop(frame);
		case "STARTTLS":
			return doTLS(frame);
		case "AUTHENTICATE":
			return doAuthenticate(t, frame);
		case "GET":
			String getKind = verbTokens.next();
			return getDispatch(t, frame, getKind);
		case "APPLY":
			String applyKind = verbTokens.next();
			return applyDispatch(t, frame, applyKind);
		default:
			logger.warn("Unknown verb: '{}' in '{}'", verb, cmdString);
			return write(CommandResult.success("Yeah for " + verb), frame);
		}

	}

	public CompletableFuture<Void> doExit(ReplicationFrame frame) {
		return write(CommandResult.success("finished"), frame).thenCompose(written -> stop());
	}

	public CompletableFuture<Void> doRestart(ReplicationFrame frame) {
		return write(CommandResult.success("Restarting"), frame);
	}

	public CompletableFuture<Void> doNoop(ReplicationFrame frame) {
		return write(CommandResult.success("Noop completed"), frame);
	}

	private CompletableFuture<Void> getDispatch(Token t, ReplicationFrame frame, String itemKind) {
		switch (itemKind) {
		case "USER":
			return doGetUser(t, frame);
		case "META":
			return doGetMeta(t, frame);
		case "MAILBOXES":
			return doGetMailboxes(t, frame);
		case "FULLMAILBOX":
			return doGetFullMailbox(t, frame);
		case "ANNOTATION":
			return doGetAnnotation(t, frame);
		case "FETCH":
			return doGetFetch(t, frame);
		default:
			return write(CommandResult.no("for GET " + itemKind), frame);
		}
	}

	private CompletableFuture<Void> applyDispatch(Token verbToken, ReplicationFrame frame, String itemKind) {
		logger.debug("APPLY {}", itemKind);
		switch (itemKind) {
		case "MESSAGE":
			return doApplyMessage(verbToken, frame);
		case "MAILBOX":
			return doApplyMailbox(verbToken, frame);
		case "UNMAILBOX":
			return doApplyUnmailbox(verbToken, frame);
		case "UNUSER":
			return doApplyUnuser(verbToken, frame);
		case "RENAME":
			return doApplyRename(verbToken, frame);
		case "EXPUNGE":
			return doApplyExpunge(verbToken, frame);
		case "RESERVE":
			return doApplyReserve(verbToken, frame);
		case "SEEN":
			return doApplySeen(verbToken, frame);
		case "ANNOTATION":
			return doApplyAnnotation(verbToken, frame);
		case "QUOTA":
			return doApplyQuota(verbToken, frame);
		case "UNQUOTA":
			return doApplyUnquota(verbToken, frame);
		case "SIEVE":
			return doApplySieve(verbToken, frame);
		case "UNSIEVE":
			return doApplyUnsieve(verbToken, frame);
		case "ACTIVATE_SIEVE":
			return doApplyActivateSieve(verbToken, frame);
		case "SUB":
			return doApplySub(verbToken, frame);
		case "UNSUB":
			return doApplyUnsub(verbToken, frame);
		default:
			logger.error("Rejecting unsupported APPLY command {}", itemKind);
			return write(CommandResult.no("for APPLY " + itemKind), frame);
		}
	}

	public CompletableFuture<Void> doApplyMessage(Token t, ReplicationFrame frame) {
		return run(new ApplyMessage(observers), t, frame).thenCompose(commandResult -> {
			return write(commandResult, frame);
		});
	}

	public CompletableFuture<Void> doApplyMailbox(Token t, ReplicationFrame frame) {
		return run(new ApplyMailbox(observers), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyRename(Token t, ReplicationFrame frame) {
		return run(new ApplyRename(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyExpunge(Token t, ReplicationFrame frame) {
		return run(new ApplyExpunge(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyUnmailbox(Token t, ReplicationFrame frame) {
		return run(new ApplyUnmailbox(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyUnuser(Token t, ReplicationFrame frame) {
		return run(new ApplyUnuser(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyUnquota(Token t, ReplicationFrame frame) {
		return run(new ApplyUnquota(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyReserve(Token t, ReplicationFrame frame) {
		return run(new ApplyReserve(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplySeen(Token t, ReplicationFrame frame) {
		return run(new ApplySeen(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplySub(Token t, ReplicationFrame frame) {
		return run(new ApplySub(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyUnsub(Token t, ReplicationFrame frame) {
		return run(new ApplyUnsub(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyAnnotation(Token t, ReplicationFrame frame) {
		return run(new ApplyAnnotation(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplySieve(Token t, ReplicationFrame frame) {
		return run(new ApplySieve(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyActivateSieve(Token t, ReplicationFrame frame) {
		return run(new ApplyActivateSieve(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyUnsieve(Token t, ReplicationFrame frame) {
		return run(new ApplyUnsieve(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doApplyQuota(Token t, ReplicationFrame frame) {
		return run(new ApplyQuota(), t, frame).thenCompose(cmdResult -> write(cmdResult, frame));
	}

	public CompletableFuture<Void> doTLS(ReplicationFrame frame) {
		return write(CommandResult.no("TLS"), frame);
	}

	public CompletableFuture<Void> doAuthenticate(Token t, ReplicationFrame frame) {
		return run(new Authenticate(), t, frame).thenCompose(result -> write(result, frame));
	}

	public CompletableFuture<Void> doGetUser(Token t, ReplicationFrame frame) {
		return run(new GetUser(), t, frame).thenCompose(result -> {
			return write(result, frame);
		});
	}

	public CompletableFuture<Void> doGetMeta(Token t, ReplicationFrame frame) {
		return run(new GetMeta(), t, frame).thenCompose(result -> {
			return write(result, frame);
		});
	}

	/**
	 * GET MAILBOXES (ex2016.vmw!user.tom ex2016.vmw!user.nico vagrant.vmw!user.jdoe
	 * vagrant.vmw!user.jdoe.Sent vagrant.vmw!user.jdoe.Outbox
	 * vagrant.vmw!user.jdoe.Trash vagrant.vmw!user.jdoe.Drafts
	 * vagrant.vmw!user.jdoe.Junk vagrant.vmw!user.janedoe
	 * vagrant.vmw!user.janedoe.Sent vagrant.vmw!user.janedoe.Outbox
	 * vagrant.vmw!user.janedoe.Trash vagrant.vmw!user.janedoe.Drafts
	 * vagrant.vmw!user.janedoe.Junk
	 * vagrant.vmw!domino^res^a2aae8f869e638e3c1257cc30023a0bd_at_domino^res
	 * vagrant.vmw!domino^res^a2aae8f869e638e3c1257cc30023a0bd_at_domino^res.Sent
	 * vagrant.vmw!domino^room^a8177012dabce540c1257cd0004bbb34_at_domino^res
	 * vagrant.vmw!domino^room^a8177012dabce540c1257cd0004bbb34_at_domino^res.Sent
	 * vagrant.vmw!domino^room^00c86c6c2095945ec1257cc20052c6fa_at_domino^res
	 * vagrant.vmw!domino^room^00c86c6c2095945ec1257cc20052c6fa_at_domino^res.Sent
	 * vagrant.vmw!user.admin ex2016.vmw!user.nico.Drafts ex2016.vmw!user.nico.Junk
	 * ex2016.vmw!user.nico.Outbox ex2016.vmw!user.nico.Sent
	 * ex2016.vmw!user.nico.Trash ex2016.vmw!user.sylvain
	 * ex2016.vmw!user.sylvain.Sent ex2016.vmw!user.sylvain.Outbox
	 * ex2016.vmw!user.sylvain.Trash ex2016.vmw!user.sylvain.Drafts
	 * ex2016.vmw!user.sylvain.Junk ex2016.vmw!user.admin
	 * ex2016.vmw!user.admin.Drafts ex2016.vmw!user.admin.Junk
	 * ex2016.vmw!user.admin.Outbox ex2016.vmw!user.admin.Sent
	 * ex2016.vmw!user.admin.Trash ex2016.vmw!user.sga ex2016.vmw!user.sga.Drafts
	 * ex2016.vmw!user.sga.Junk ex2016.vmw!user.sga.Outbox ex2016.vmw!user.sga.Sent
	 * ex2016.vmw!user.sga.Trash ex2016.vmw!tom ex2016.vmw!user.tom.Drafts
	 * ex2016.vmw!DELETED.user.tom.Drafts.5863AA42 ex2016.vmw!user.tom.Junk
	 * ex2016.vmw!DELETED.user.tom.Junk.5863AA42 ex2016.vmw!user.tom.Outbox
	 * ex2016.vmw!DELETED.user.tom.Outbox.5863AA42 ex2016.vmw!user.tom.Sent
	 * ex2016.vmw!DELETED.user.tom.Sent.5863AA42 ex2016.vmw!user.tom.Trash
	 * ex2016.vmw!DELETED.user.tom.Trash.5863AA42)
	 * 
	 * @param t
	 * @param frame
	 * @return
	 */
	public CompletableFuture<Void> doGetMailboxes(Token t, ReplicationFrame frame) {
		return run(new GetMailboxes(), t, frame).thenCompose(result -> write(result, frame));
	}

	/**
	 * GET FULLMAILBOX vagrant.vmw!user.admin
	 * 
	 * * MAILBOX %(UNIQUEID 5596488a5890990f MBOXNAME vagrant.vmw!user.admin
	 * LAST_UID 3 HIGHESTMODSEQ 40 RECENTUID 3 RECENTTIME 1486035958 LAST_APPENDDATE
	 * 1485871655 POP3_LAST_LOGIN 0 UIDVALIDITY 1485871375 PARTITION vagrant_vmw ACL
	 * "admin@vagrant.vmw lrswipkxtecda admin0 lrswipkxtecda " OPTIONS P SYNC_CRC
	 * 3635576038 RECORD ( %(UID 1 MODSEQ 36 LAST_UPDATED 1486035945 FLAGS (\Seen)
	 * INTERNALDATE 1485871611 SIZE 1281 GUID
	 * 065ba5d1f867100b0da8622f75ff7760d26c5a83) %(UID 2 MODSEQ 39 LAST_UPDATED
	 * 1486035950 FLAGS (\Flagged) INTERNALDATE 1485871635 SIZE 35189 GUID
	 * 1a8b394b6827ac783ad40bb0c293ce5822b9e0cf) %(UID 3 MODSEQ 40 LAST_UPDATED
	 * 1486035958 FLAGS (\Seen) INTERNALDATE 1485871655 SIZE 4376698 GUID
	 * 8f6a929e05aeee58dcf2e8289320572ee99a0a82)))
	 * 
	 * OK success
	 * 
	 * 
	 * @param t
	 * @param frame
	 * @return
	 */
	public CompletableFuture<Void> doGetFullMailbox(Token t, ReplicationFrame frame) {
		return run(new GetFullMailbox(), t, frame).thenCompose(result -> write(result, frame));
	}

	/**
	 * GET ANNOTATION
	 * vagrant.vmw!domino^room^a8177012dabce540c1257cd0004bbb34_at_domino^res
	 * 
	 * @param t
	 * @param frame
	 * @return
	 */
	public CompletableFuture<Void> doGetAnnotation(Token t, ReplicationFrame frame) {
		return run(new GetAnnotation(), t, frame).thenCompose(result -> write(result, frame));
	}

	/**
	 * GET FETCH %(MBOXNAME fws.fr!user.dani PARTITION bm-master__fws_fr UNIQUEID
	 * k3j4ly1rzg13gtex5nwmo3r5 GUID 3935e077b8a883b05105e1984166542c3ab2cdab UID
	 * 13238)
	 * 
	 * @param t
	 * @param frame
	 * @return
	 */
	public CompletableFuture<Void> doGetFetch(Token t, ReplicationFrame frame) {
		return run(new GetFetch(), t, frame).thenCompose(result -> write(result, frame));
	}

	public String remoteIp() {
		return storage.remoteIp();
	}

}
