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
package net.bluemind.imap.vertx;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.imap.vertx.IConnectionSupport.INetworkCon;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.cmd.AppendCommandHelper;
import net.bluemind.imap.vertx.cmd.AppendResponse;
import net.bluemind.imap.vertx.cmd.SelectResponse;
import net.bluemind.imap.vertx.parsing.AppendPayloadBuilder;
import net.bluemind.imap.vertx.parsing.BannerPayloadBuilder;
import net.bluemind.imap.vertx.parsing.ImapChunkProcessor;
import net.bluemind.imap.vertx.parsing.ImapChunker;
import net.bluemind.imap.vertx.parsing.SelectPayloadBuilder;
import net.bluemind.imap.vertx.parsing.StreamSinkProcessor;
import net.bluemind.imap.vertx.parsing.TaggedResponseProcessor;
import net.bluemind.imap.vertx.parsing.VoidTaggedResponseProcessor;
import net.bluemind.imap.vertx.stream.Base64Decoder;
import net.bluemind.imap.vertx.stream.QuotedPrintableDecoder;
import net.bluemind.imap.vertx.stream.WriteToRead;
import net.bluemind.lib.jutf7.UTF7Converter;

public class VXStoreClient implements IAsyncStoreClient {

	private static final Logger logger = LoggerFactory.getLogger(VXStoreClient.class);

	private final String login;
	private final String password;
	private int tags = 0;
	private String selected;
	private ImapChunkProcessor packetProc;
	private Optional<INetworkCon> sock;
	private final String host;
	private final int port;
	private final IConnectionSupport conSupport;

	public VXStoreClient(IConnectionSupport conSupport, String host, int port, String login, String password) {
		this.login = login;
		this.password = password;
		this.conSupport = conSupport;
		this.host = host;
		this.port = port;
	}

	private String tagged(String com) {
		return "V" + (++tags) + " " + com + "\r\n";
	}

	public CompletableFuture<ImapResponseStatus<Void>> login() {
		String cmd = tagged("LOGIN " + login + " \"" + password + "\"");

		this.sock = Optional.empty();

		logger.info("Connecting to {}:{}...", host, port);
		this.packetProc = new ImapChunkProcessor();
		TaggedResponseProcessor<String> banner = new TaggedResponseProcessor<>(new BannerPayloadBuilder());
		packetProc.setDelegate(banner);
		VoidTaggedResponseProcessor loginProc = new VoidTaggedResponseProcessor();

		conSupport.connect(port, host, ar -> {
			if (ar.failed()) {
				loginProc.future().completeExceptionally(ar.cause());
			} else {
				INetworkCon nc = ar.result();
				sock = Optional.of(nc);
				ImapChunker chunker = new ImapChunker(nc.read());
				Pump.pump(chunker, packetProc).start();
				banner.future().thenAccept(bannerResp -> {
					packetProc.setDelegate(null);
					packetProc.setDelegate(loginProc);
					logger.info("IMAP connection setup is complete {}", bannerResp.result);
					nc.write(cmd);
				}).exceptionally(t -> {
					loginProc.future().completeExceptionally(t);
					return null;
				});
			}
		});

		return loginProc.future().thenApply(ir -> {
			packetProc.setDelegate(null);
			return ir;
		});
	}

	private static final CompletableFuture<ImapResponseStatus<SelectResponse>> SELECTED = CompletableFuture
			.completedFuture(new ImapResponseStatus<SelectResponse>(Status.Ok, new SelectResponse()));

	@Override
	public CompletableFuture<ImapResponseStatus<SelectResponse>> select(String mailbox) {
		if (selected != null && selected.equals(mailbox)) {
			return SELECTED;
		}
		String quotedUtf7 = UTF7Converter.encode(mailbox);
		TaggedResponseProcessor<SelectResponse> tagged = new TaggedResponseProcessor<>(new SelectPayloadBuilder());
		String cmd = tagged("SELECT \"" + quotedUtf7 + "\"");
		sock.ifPresent(ns -> {
			retryableSelect(tagged, cmd, ns);
		});
		return tagged.future().thenApply((ImapResponseStatus<SelectResponse> msg) -> {
			if (msg.status != Status.Ok) {
				logger.warn("Selection failed, cmd was: SELECT \"{}\"", quotedUtf7);
				selected = null;
			} else {
				selected = mailbox;
			}
			packetProc.setDelegate(null);
			return msg;
		});
	}

	private void retryableSelect(TaggedResponseProcessor<SelectResponse> tagged, String cmd, INetworkCon ns) {
		try {
			packetProc.setDelegate(tagged);
			ns.write(cmd);
		} catch (ConcurrentModificationException cme) {
			logger.warn("Command in progress ({}), retry in 10ms.", cme.getMessage());
			conSupport.vertx().setTimer(10, tid -> retryableSelect(tagged, cmd, ns));
		}
	}

	public enum Decoder {
		NONE, B64, QP;

		public WriteStream<Buffer> withDelegate(WriteStream<Buffer> buf) {
			switch (this) {
			case B64:
				return new Base64Decoder(buf);
			case QP:
				return new QuotedPrintableDecoder(buf);
			default:
				return buf;
			}
		}

		public static Decoder fromEncoding(String encoding) {
			if (encoding == null) {
				return NONE;
			}

			switch (encoding.toLowerCase()) {
			case "base64":
				return B64;
			case "quoted-printable":
				return QP;
			default:
				return NONE;
			}
		}

	}

	@Override
	public CompletableFuture<Void> fetch(long uid, String part, WriteStream<Buffer> target, Decoder dec) {
		StreamSinkProcessor proc = new StreamSinkProcessor(selected, uid, part, dec.withDelegate(target));
		String cmd = tagged("UID FETCH " + uid + " (UID BODY.PEEK[" + part + "])");
		sock.ifPresent(ns -> retryableFetch(proc, cmd, ns));
		return proc.future().thenApply(r -> {
			packetProc.setDelegate(null);
			return r;
		});
	}

	private void retryableFetch(StreamSinkProcessor proc, String cmd, INetworkCon ns) {
		try {
			packetProc.setDelegate(proc);
			ns.write(cmd);
		} catch (ConcurrentModificationException cme) {
			logger.warn("Command in progress ({}), retry in 10ms.", cme.getMessage());
			conSupport.vertx().setTimer(10, tid -> retryableFetch(proc, cmd, ns));
		}
	}

	@Override
	public ReadStream<Buffer> fetch(long uid, String part, Decoder dec) {
		WriteToRead<Buffer> convert = new WriteToRead<>(conSupport.vertx());
		fetch(uid, part, convert, dec);
		return convert;
	}

	@Override
	public CompletableFuture<ImapResponseStatus<AppendResponse>> append(String mailbox, Date receivedDate,
			Collection<String> flags, int streamSize, ReadStream<Buffer> eml) {

		StringBuilder sb = new StringBuilder("V").append(++tags);
		sb.append(" APPEND \"").append(UTF7Converter.encode(mailbox)).append("\" ");
		AppendCommandHelper.flags(sb, flags);
		AppendCommandHelper.deliveryDate(sb, receivedDate);
		sb.append("{").append(streamSize).append("+}\r\n");
		String cmd = sb.toString();

		TaggedResponseProcessor<AppendResponse> proc = new TaggedResponseProcessor<>(new AppendPayloadBuilder());
		sock.ifPresent(ns -> {
			packetProc.setDelegate(proc);
			ns.write(cmd);
			eml.pipe().endOnComplete(false).to(ns.write(), comp -> ns.write("\r\n"));
			eml.resume();
		});
		return proc.future().thenApply(r -> {
			packetProc.setDelegate(null);
			return r;
		});
	}

	@Override
	public CompletableFuture<Void> close() {
		if (!sock.isPresent()) {
			logger.warn("Missing sock {} for closing {}", sock, this);
			return CompletableFuture.completedFuture(null);
		}
		CompletableFuture<Void> ret = new CompletableFuture<>();
		sock.ifPresent(s -> s.close(ar -> {
			if (!ar.succeeded()) {
				logger.error("error closing {}", ar.cause());
			}
			ret.complete(null);
			sock = Optional.empty();
		}));
		return ret;
	}

	@Override
	public boolean isClosed() {
		return !sock.isPresent();
	}

}
