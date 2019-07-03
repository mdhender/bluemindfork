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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lmtp.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.net.NetSocket;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;
import com.netflix.spectator.api.patterns.PolledMeter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.lmtp.backend.DeliveredVersion;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.lmtp.backend.LmtpEnvelope;
import net.bluemind.lmtp.backend.LmtpReply;
import net.bluemind.lmtp.impl.busmessages.DeliveredMailMessage;
import net.bluemind.lmtp.impl.busmessages.MailMessage;
import net.bluemind.lmtp.parser.LmtpRequestHandler;
import net.bluemind.lmtp.parser.LmtpRequestParser;
import net.bluemind.lmtp.parser.LmtpResponseHandler;
import net.bluemind.lmtp.parser.LmtpResponseParser;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class LmtpSessionProxy implements LmtpRequestHandler, LmtpResponseHandler {

	private static final Logger logger = LoggerFactory.getLogger(LmtpSessionProxy.class);

	private static final DeliveryStats stats = new DeliveryStats();

	private static enum State {
		Normal, Data
	}

	private final static ByteBuf CRLF_DOT_CRLF = Unpooled.wrappedBuffer("\r\n.\r\n".getBytes());

	private LmtpConfig config;

	protected LmtpEnvelope mEnvelope;

	private String lhloArg;

	private String remoteAddress;

	private Queue<LmtpAddress> toValidate = new LinkedList<>();
	private Queue<LmtpAddress> toDeliver = new LinkedList<>();

	private Queue<String> commandQueue = new LinkedList<>();

	private NetSocket backend;
	private NetSocket client;

	private EventBus eventBus;

	private LmtpRequestParser lmtpRequestParser;

	private State state = State.Normal;

	private final String sid;

	private final Registry registry;
	private final IdFactory idFactory;
	private final static AtomicInteger numConnections = PolledMeter.using(MetricsRegistry.get())
			.withId(new IdFactory(MetricsRegistry.get(), LmtpSessionProxy.class).name("activeConnections"))
			.monitorValue(new AtomicInteger(0));

	public LmtpSessionProxy(Registry reg, EventBus eventBus, NetSocket client, NetSocket backend, LmtpConfig config) {
		this.registry = reg;
		this.idFactory = new IdFactory(reg, this);

		this.eventBus = eventBus;
		this.remoteAddress = client.remoteAddress().getAddress().toString();

		this.client = client;
		this.backend = backend;

		mEnvelope = new LmtpEnvelope();
		this.config = config;
		this.sid = String.format("[%s:%s]", client.writeHandlerID(), backend.writeHandlerID());
	}

	/**
	 * start session/connection proxy
	 */
	public void start() {
		numConnections.incrementAndGet();
		registry.counter(idFactory.name("connectionCount")).increment();
		Timer timer = registry.timer(idFactory.name("sessionDuration"));
		final long start = registry.clock().monotonicTime();

		commandQueue.add("BANNER");

		lmtpRequestParser = new LmtpRequestParser(client.writeHandlerID(), this);

		client.dataHandler(lmtpRequestParser);
		backend.dataHandler(new LmtpResponseParser(backend.writeHandlerID(), this));

		client.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				numConnections.decrementAndGet();
				final long end = registry.clock().monotonicTime();
				timer.record(end - start, TimeUnit.NANOSECONDS);
				backend.close();
			}
		});

		backend.closeHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				client.close();
			}
		});

		backend.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				client.close();
			}
		});

		client.closeHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				backend.close();
			}

		});

		client.resume();
		backend.resume();

	}

	@Override
	public void handleUnknow(String cmd, String params) {
		commandQueue.add(cmd);
		logger.warn("{} unknown lmtp cmd {}", sid, cmd);
		forwardCmd(cmd, params);
	}

	@Override
	public void handleLHLO(String arg) {
		commandQueue.add("LHLO");
		this.lhloArg = arg;
		forwardCmd("LHLO", arg);
	}

	@Override
	public void handleMAIL(String arg) {
		commandQueue.add("MAIL");

		if (arg == null || arg.length() == 0) {
			// woot ?
			return;
		}

		LmtpAddress addr = new LmtpAddress(arg, new String[] { "BODY", "SIZE" }, null);

		mEnvelope.setSender(addr);
		forwardCmd("MAIL", arg);
	}

	@Override
	public void handleRSET(String params) {
		commandQueue.add("RSET");
		forwardCmd("RSET", params);
	}

	@Override
	public void handleRCPT(String arg) {
		commandQueue.add("RCPT");
		if (arg == null || arg.length() < 3) {
			logger.warn("no parameter to rcpt to");
			return;
		}

		String recp = arg.substring(3);
		LmtpAddress addr = new LmtpAddress(recp, null, config.getRecipientDelimiter());
		toValidate.add(addr);
		forwardCmd("RCPT", arg);
	}

	@Override
	public void handleNOOP() {
		commandQueue.add("NOOP");
		forwardCmd("NOOP", null);
	}

	@Override
	public void handleQUIT() {
		commandQueue.add("QUIT");
		forwardCmd("QUIT", null);
	}

	@Override
	public void handleVRFY(String params) {
		commandQueue.add("VRFY");
		forwardCmd("VRFY", null);
	}

	@Override
	public void handleDATA(String params) {
		commandQueue.add("DATA");
		pause();
		forwardCmd("DATA", null);
	}

	private void pause() {
		logger.debug("pause client");
		lmtpRequestParser.pause();
		client.pause();
	}

	private void resume() {
		logger.debug("resume client");
		client.resume();
		lmtpRequestParser.resume();
	}

	@Override
	public void handleDataBuffer(Buffer ori) {
		final ByteBuf data = ori.getByteBuf();
		final Buffer header = new Buffer(getAdditionalHeaders());

		registry.distributionSummary(idFactory.name("emailSize")).record(data.readableBytes());

		if (toValidate.size() != 0) {
			logger.warn("rcpt should be empty at this stage");
		}

		toValidate = new LinkedList<>();

		pause();
		// waiting for DATA response

		// temporary add recipients (without dv) (used by IMIPFilter)
		for (LmtpAddress tmp : toDeliver) {
			mEnvelope.addRecipient(tmp);
		}

		eventBus.send("lmtp.filters", new MailMessage(mEnvelope, data), new Handler<Message<MailMessage>>() {

			@Override
			public void handle(Message<MailMessage> event) {
				// remove recipients
				// recipients will be added on delivered response
				mEnvelope.getRecipients().clear();
				ByteBuf finalDataBuffer = Unpooled.wrappedBuffer(header.getByteBuf(), event.body().getData(),
						CRLF_DOT_CRLF);

				Buffer buf = new Buffer(finalDataBuffer);
				logger.debug("send data body  (size: {}) to backend ", buf.length());
				backend.write(buf);

				if (backend.writeQueueFull()) {
					logger.debug("backend socket is full, put a drainHandler");
					backend.drainHandler(new Handler<Void>() {

						@Override
						public void handle(Void event) {
							logger.debug("ready to recieve more command from client");
						}

					});
				} else {
					logger.debug("ready to recieve more command from client");
				}

			}
		});

	}

	/*
	 * Generates the <tt>Return-Path</tt> and <tt>Received</tt> headers for the
	 * current incoming message.
	 */
	protected String getAdditionalHeaders() {
		StringBuilder headers = new StringBuilder();

		// Assemble Return-Path header
		if (mEnvelope.hasSender()) {
			String sender = mEnvelope.getSender().getEmailAddress();
			if (sender != null && sender.trim().length() > 0) {
				headers.append(String.format("Return-Path: %s\r\n", sender));
			}
		}

		// Assemble Received header
		String localHostname = "unknown";
		String timestamp = new MailDateFormat().format(new Date());
		String name = "Received: ";
		String value = String.format("from %s (LHLO %s) by %s with LMTP; %s", remoteAddress, lhloArg, localHostname,
				timestamp);
		headers.append(name);
		headers.append(MimeUtility.fold(name.length(), value));
		headers.append("\r\n");

		return headers.toString();
	}

	private void reset() {
		mEnvelope = new LmtpEnvelope();
		toDeliver.clear();
		toValidate.clear();
		state = State.Normal;
		lmtpRequestParser.reset();
	}

	@Override
	public void handleResponse(LmtpResponse resp) {
		logger.debug("response (State:{}) {} {} {}", state, resp.getCode(), resp.getResponseMessage(), resp.getLines());
		switch (state) {
		case Normal:
			handleNormalResponse(resp);
			break;
		case Data:
			handleDataResponse(resp);
			break;
		}
		forwardResponse(resp);
	}

	private void handleDataResponse(LmtpResponse resp) {
		// delivery response
		LmtpAddress value = toDeliver.poll();
		if (value == null) {
			logger.warn("strange, not more toDeliver available for {} {}", resp.getCode(), resp.getResponseMessage());
		} else if (resp.getCode() == 250) {
			logger.debug("{} delivery ok for {} ({} {})", sid, value.getEmailAddress(), resp.getCode(),
					resp.getResponseMessage());

			value.setDeliveredVersion(new DeliveredVersion(value.getEmailAddress()));
			value.setDeliveryStatus(LmtpReply.DELIVERY_OK);
			mEnvelope.addRecipient(value);
		} else {
			LmtpReply reply = LmtpReply.adapt(resp);
			logger.warn("{} delivery failed for {}: {} {}", sid, value.getEmailAddress(), resp.getCode(),
					resp.getResponseMessage());
			value.setDeliveryStatus(reply == null ? LmtpReply.TEMPORARY_FAILURE : reply);
			mEnvelope.addRecipient(value);
		}

		if (toDeliver.size() == 0) {
			deliveryDone();
			reset();
			resume();
		}
	}

	private void handleNormalResponse(LmtpResponse resp) {

		String cmd = commandQueue.poll();
		if ("LHLO".equals(cmd) || "QUIT".equals(cmd) || "VRFY".equals(cmd) || "NOOP".equals(cmd)
				|| "MAIL".equals(cmd)) {
			logger.debug("response for CMD {} response {} {}", cmd, resp.getCode(), resp.getResponseMessage());
		} else if ("RSET".equals(cmd)) {
			reset();
		} else if ("RCPT".equals(cmd)) {
			logger.trace("response for CMD {} response {} {}", cmd, resp.getCode(), resp.getResponseMessage());
			LmtpAddress value = toValidate.poll();
			if (resp.getCode() == 250) {
				logger.debug("recipient {} is valid ", value.getEmailAddress());
				toDeliver.add(value);
			} else {
				registry.counter(idFactory.name("deliveries").withTag("status", "ko")).increment();
				logger.error("{} recipient {} is invalid ({})", sid, value.getEmailAddress(),
						resp.getResponseMessage());
			}
		} else if ("DATA".equals(cmd)) {
			logger.debug("response for CMD {} response {} {}", cmd, resp.getCode(), resp.getResponseMessage());
			if (resp.getCode() == 354) {
				logger.debug("go head !");
				state = State.Data;
				lmtpRequestParser.setState(LmtpRequestParser.State.Data);
				resume();
			} else {
				state = State.Normal;
				lmtpRequestParser.setState(LmtpRequestParser.State.Cmd);
				logger.warn("{} DATA error {} {}", sid, resp.getCode(), resp.getResponseMessage());
				reset();
				resume();
			}
		} else if ("BANNER".equals(cmd)) {
			logger.debug("Received banner {}", resp.getResponseMessage());
		} else {
			logger.warn("{} response for unknown command {} response: {} {}", sid, cmd, resp.getCode(),
					resp.getResponseMessage());
		}
	}

	private void deliveryDone() {
		logger.debug("delivery done, call IDoneAction(s), reciptients {}", mEnvelope.getRecipients());
		// de-duplicate versions
		Set<DeliveredVersion> alreadyDelivred = new HashSet<>();

		registry.distributionSummary(idFactory.name("emailRecipients")).record(mEnvelope.getRecipients().size());
		for (LmtpAddress recipient : mEnvelope.getRecipients()) {
			LmtpReply reply = recipient.getDeliveryStatus();

			if (logger.isDebugEnabled()) {
				logger.debug(recipient.getEmailAddress() + " return status: " + reply.toString());
			}

			String rmail = recipient.getEmailAddress();
			if (reply.success()) {
				DeliveredVersion dv = recipient.getDeliveredVersion();
				registry.counter(idFactory.name("deliveries").withTag("status", "ok")).increment();
				if (alreadyDelivred.contains(dv)) {
					registry.counter(idFactory.name("deliveries").withTag("status", "duplicate")).increment();
					logger.warn("{} mail already delivred {} that's should not happen", sid, dv.getMbox());
					continue;
				}
				alreadyDelivred.add(dv);
				logger.info("{} [{}] delivered to {} (for {})", sid, mEnvelope.getId(), dv.getMbox(), rmail);

				stats.newSuccess();
			} else {
				registry.counter(idFactory.name("deliveries").withTag("status", "ko")).increment();
				logger.error("{} [{}] {}: {}", sid, mEnvelope.getId(), rmail, reply);
				stats.newFailure();
			}
		}
		eventBus.send(LmtpDoneActionVerticle.ADDR, new DeliveredMailMessage(mEnvelope));
	}

	private void forwardCmd(String cmd, String params) {

		Buffer cmdBuf = new Buffer();
		if (params == null || params.length() == 0) {
			cmdBuf.appendString(cmd);
			cmdBuf.appendString("\r\n");
		} else {
			cmdBuf.appendString(cmd);
			cmdBuf.appendString(" ");
			cmdBuf.appendString(params);
			cmdBuf.appendString("\r\n");
		}

		logger.trace("forward to backend {} {}", cmd, params);
		backend.write(cmdBuf);
	}

	private void forwardResponse(LmtpResponse resp) {
		Buffer respBuffer = new Buffer();
		for (String line : resp.getLines()) {
			respBuffer.appendString(line);
			respBuffer.appendString("\r\n");
		}
		respBuffer.appendString(resp.getCode().toString());
		respBuffer.appendString(" ");
		respBuffer.appendString(resp.getResponseMessage());
		respBuffer.appendString("\r\n");

		logger.trace("forward to client {} {} {}", resp.getCode(), resp.getResponseMessage(), resp.getLines());
		client.write(respBuffer);
	}
}
