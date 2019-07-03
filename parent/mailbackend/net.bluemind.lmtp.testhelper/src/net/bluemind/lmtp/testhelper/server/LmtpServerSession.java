/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.lmtp.testhelper.server;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.field.address.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.parsetools.RecordParser;

import net.bluemind.lmtp.testhelper.common.WriteSupport;
import net.bluemind.lmtp.testhelper.model.FakeMailbox;
import net.bluemind.lmtp.testhelper.model.FakeMailbox.State;
import net.bluemind.lmtp.testhelper.model.MailboxesModel;
import net.bluemind.lmtp.testhelper.model.MockServerStats;

public class LmtpServerSession {

	private NetSocket sock;
	private Vertx vertx;
	private RecordParser recordParser;
	private WriteSupport writeSupport;
	private static final Logger logger = LoggerFactory.getLogger(LmtpServerSession.class);

	private static enum ParseState {
		Cmd, Data
	}

	private ParseState expectedContent;

	private Queue<FakeMailbox> validRecipients = new LinkedList<>();

	public LmtpServerSession(Vertx vertx, NetSocket sock) {
		this.vertx = vertx;
		this.sock = sock;
		this.writeSupport = new WriteSupport(sock);
		logger.debug("Starting with vertx {}", this.vertx);
	}

	public void start() {
		this.recordParser = RecordParser.newDelimited("\r\n", buf -> {
			switch (expectedContent) {
			case Cmd:
				rcvCommand(buf);
				break;
			case Data:
				rcvData(buf);
				break;
			}
		});
		expectedContent = ParseState.Cmd;
		sock.dataHandler(buf -> {
			logger.debug("{} C: {}", expectedContent, buf);
			recordParser.handle(buf);
		});
		sock.closeHandler(v -> {
			stop();
		});
		MockServerStats.get().connect();
		writeSupport.writeWithCRLF(MockServer.BANNER);
	}

	private void rcvData(Buffer buf) {
		logger.info("C[{}]: {}bytes.", expectedContent, buf.length());
		recordParser.delimitedMode("\r\n");
		expectedContent = ParseState.Cmd;
		CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
		FakeMailbox fm = null;
		while ((fm = validRecipients.poll()) != null) {
			final FakeMailbox defMb = fm;
			chain = chain.thenCompose(v -> {
				if (defMb.state == State.AfterDataError) {
					return writeSupport.writeWithCRLF("451 4.3.0 System I/O error");
				} else {
					return writeSupport.writeWithCRLF("250 2.1.5 Ok for " + defMb.email);
				}
			});
		}

	}

	private void rcvCommand(Buffer buf) {
		String cmd = buf.toString();
		logger.info("C[{}]: {}", expectedContent, cmd);
		String verb = cmd.toLowerCase();
		int space = cmd.indexOf(' ');
		if (space > 0) {
			verb = verb.substring(0, space);
		}
		switch (verb) {
		case "lhlo":
			doLHLO();
			break;
		case "mail":
			doMAIL(cmd);
			break;
		case "rcpt":
			doRCPT(cmd);
			break;
		case "data":
			doDATA();
			break;
		case "rset":
			doRSET();
			break;
		case "quit":
			doQUIT();
			break;
		default:
			writeSupport.writeWithCRLF("451 4.3.0 Unsupported verb " + verb);
		}
	}

	private void doQUIT() {
		writeSupport.writeWithCRLF("221 2.0.0 bye").thenAccept(v -> sock.close());
	}

	private void doRSET() {
		validRecipients.clear();
		writeSupport.writeWithCRLF("250 2.0.0 ok");
	}

	private void doDATA() {
		String resp = "354 go ahead";
		boolean switchDelim = true;
		if (validRecipients.isEmpty()) {
			resp = "503 5.5.1 No recipients";
			switchDelim = false;
		}
		final String defResp = resp;
		final boolean defDelimSwich = switchDelim;
		writeSupport.writeWithCRLF(defResp).thenAccept(v -> {
			logger.info("S: {}, SWITCH DELIM", defResp);
			if (defDelimSwich) {
				recordParser.delimitedMode("\r\n.\r\n");
				expectedContent = ParseState.Data;
			}
		});
	}

	private void doRCPT(String cmd) {
		String resp = "250 2.1.0 ok";
		String senderPart = cmd.substring("RCPT TO:".length());
		try {
			Mailbox parsed = AddressBuilder.DEFAULT.parseMailbox(senderPart, DecodeMonitor.SILENT);
			String asEmail = parsed.getAddress();
			Optional<FakeMailbox> mbox = MailboxesModel.get().mailbox(asEmail);
			logger.info("existing recipient {} ? {}", asEmail, mbox.isPresent());
			if (mbox.isPresent()) {
				switch (mbox.get().state) {
				case Fucked:
					resp = "451 4.3.0 System I/O fuckage";
					break;
				case OverQuota:
					resp = "452 4.2.2 Over Quota";
					break;
				default:
					validRecipients.add(mbox.get());
					break;
				}
			} else {
				resp = "550-Mailbox unknown.\r\n550 5.1.1 User unknown";
			}
		} catch (ParseException e) {
			logger.error("Error parsing '{}': {}", senderPart, e.getMessage());
			resp = "501 5.5.4 Syntax error in parameters";
		}
		final String definiteResp = resp;
		writeSupport.writeWithCRLF(definiteResp).thenAccept(v -> {
			logger.info("S: {}", definiteResp);
		});
	}

	private void doMAIL(String cmd) {
		String resp = "250 2.1.0 ok";
		String senderPart = cmd.substring("MAIL FROM:".length());
		int sizeIdx = senderPart.indexOf(" SIZE=");
		if (sizeIdx > 0) {
			int size = Integer.parseInt(senderPart.substring(sizeIdx + " SIZE=".length()));
			logger.info("Estimated msg size is {}byte(s)", size);
			senderPart = senderPart.substring(0, sizeIdx);
		}
		logger.info("senderPart: {}", senderPart);
		try {
			if ("<>".equals(senderPart)) {
				// this is ok
			} else {
				Mailbox parsed = AddressBuilder.DEFAULT.parseMailbox(senderPart, DecodeMonitor.SILENT);
				String asEmail = parsed.getAddress();
				boolean valid = MailboxesModel.get().isValidSender(asEmail);
				logger.info("valid sender {} ? {}", asEmail, valid ? "YEAH" : "NEIN");
				if (!valid) {
					resp = "455 Invalid Sender";
				}
			}
		} catch (ParseException e) {
			resp = "501 5.5.4 Syntax error in parameters";
		}
		final String definiteResp = resp;
		writeSupport.writeWithCRLF(definiteResp).thenAccept(v -> {
			logger.info("S: {}", definiteResp);
		});
	}

	private void doLHLO() {
		String capas = capabilities("mock", "8BITMIME", "ENHANCEDSTATUSCODES", "PIPELINING", "SIZE", "AUTH EXTERNAL",
				"IGNOREQUOTA");
		writeSupport.writeWithCRLF(capas).thenAccept(v -> {
			logger.info("S: {}", capas);
		});
	}

	private String capabilities(String host, String... capas) {
		StringBuilder sb = new StringBuilder("250-" + host);

		for (int i = 0; i < capas.length - 1; i++) {
			sb.append("\r\n").append("250-").append(capas[i]);
		}
		sb.append("\r\n250 ").append(capas[capas.length - 1]);
		return sb.toString();

	}

	public void stop() {
		MockServer.closeEvent();
		logger.info("Socket {} closed.", sock.writeHandlerID());
	}

}
