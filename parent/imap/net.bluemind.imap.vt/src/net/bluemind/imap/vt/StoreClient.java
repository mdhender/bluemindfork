/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.imap.vt;

import static io.vertx.core.buffer.Buffer.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import net.bluemind.imap.vt.cmd.AppendCommand;
import net.bluemind.imap.vt.cmd.CommandContext;
import net.bluemind.imap.vt.cmd.ExpungeCommand;
import net.bluemind.imap.vt.cmd.FetchEmlCommand;
import net.bluemind.imap.vt.cmd.FetchEmlPartCommand;
import net.bluemind.imap.vt.cmd.IdleCommand;
import net.bluemind.imap.vt.cmd.ListCommand;
import net.bluemind.imap.vt.cmd.LoginCommand;
import net.bluemind.imap.vt.cmd.NoopCommand;
import net.bluemind.imap.vt.cmd.SelectCommand;
import net.bluemind.imap.vt.cmd.UidCopyCommand;
import net.bluemind.imap.vt.cmd.UidExpungeCommand;
import net.bluemind.imap.vt.cmd.UidFetchHeadersCommand;
import net.bluemind.imap.vt.cmd.UidStoreCommand;
import net.bluemind.imap.vt.dto.FetchedChunk;
import net.bluemind.imap.vt.dto.IdleContext;
import net.bluemind.imap.vt.dto.IdleListener;
import net.bluemind.imap.vt.dto.ListResult;
import net.bluemind.imap.vt.dto.Mode;
import net.bluemind.imap.vt.dto.UidFetched;
import net.bluemind.imap.vt.parsing.BufHandler;
import net.bluemind.imap.vt.parsing.IncomingChunk;

public class StoreClient implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(StoreClient.class);

	private final Socket sock;
	private final OutputStream out;
	private final BlockingDeque<IncomingChunk> pending;
	private final TagProducer tagProd;

	private volatile boolean stopped;
	private String login;
	private byte[] pass;

	private final CommandContext bundle;

	private static final int CHUNK_SIZE = 8192;

	public StoreClient(String host, int port, String login, String pass) throws IOException {
		Objects.requireNonNull(host, "host cannot be null");
		Objects.requireNonNull(login, "login cannot be null");
		Objects.requireNonNull(pass, "pass cannot be null");
		this.sock = new Socket();
		sock.connect(new InetSocketAddress(host, port));
		this.pending = new LinkedBlockingDeque<>(16);
		this.out = sock.getOutputStream();
		this.tagProd = new TagProducer();

		RecordParser rp = RecordParser.newDelimited("\r\n");
		Thread.ofVirtual().name("virtual:imap-client-read").start(() -> {
			BufHandler bh = new BufHandler(rp, pending);
			logger.debug("Parser created {}", bh);

			byte[] inc = new byte[CHUNK_SIZE];
			ByteBuf asBuf = Unpooled.wrappedBuffer(inc);
			try {
				InputStream in = sock.getInputStream();
				while (!stopped) {
					int read = in.read(inc, 0, CHUNK_SIZE);
					if (read == -1) {
						break;
					}
					asBuf.readerIndex(0).writerIndex(read);
					@SuppressWarnings("deprecation")
					Buffer asVxBuf = buffer(asBuf);
					rp.handle(asVxBuf);
				}
			} catch (SocketException se) {
				if (!stopped) {
					throw new ClientFault(se);
				} else {
					logger.info("Expected close of {}", sock);
				}
			} catch (IOException e) {
				throw new ClientFault(e);
			}

		});
		this.login = login;
		this.pass = pass.getBytes();
		this.bundle = new CommandContext(tagProd, out, pending);
	}

	public void close() throws IOException {
		stopped = true;
		sock.close();
	}

	public boolean login() throws IOException {
		return new LoginCommand(bundle, login, pass).execute().booleanValue();
	}

	public boolean noop() throws IOException {
		return new NoopCommand(bundle).execute().booleanValue();
	}

	public boolean select(String fn) throws IOException {
		return new SelectCommand(bundle, fn).execute().booleanValue();
	}

	public ListResult list(String ref, String pattern) throws IOException {
		return new ListCommand(bundle, ref, pattern).execute();
	}

	public FetchedChunk uidFetchMessage(int uid) throws IOException {
		return new FetchEmlCommand(bundle, uid).execute();
	}

	public FetchedChunk uidFetchPart(int uid, String address) throws IOException {
		return new FetchEmlPartCommand(bundle, uid, address).execute();
	}

	public List<UidFetched> uidFetchHeaders(String idSet, String... headers) throws IOException {
		return new UidFetchHeadersCommand(bundle, idSet, headers).execute();
	}

	public int append(String folder, ByteBuf eml) throws IOException {
		return new AppendCommand(bundle, folder, eml).execute().intValue();
	}

	public boolean uidStore(String idSet, Mode m, String... flags) throws IOException {
		return new UidStoreCommand(bundle, idSet, m, flags).execute().booleanValue();
	}

	public Map<Integer, Integer> uidCopy(String destFolder, int... uids) throws IOException {
		return uidCopy(destFolder, Arrays.stream(uids).mapToObj(Integer::toString).collect(Collectors.joining(",")));
	}

	public Map<Integer, Integer> uidCopy(String destFolder, String idSet) throws IOException {
		return new UidCopyCommand(bundle, destFolder, idSet).execute();
	}

	public boolean expunge() throws IOException {
		return new ExpungeCommand(bundle).execute().booleanValue();
	}

	public boolean uidExpunge(int... uids) throws IOException {
		return uidExpunge(Arrays.stream(uids).mapToObj(Integer::toString).collect(Collectors.joining(",")));
	}

	public boolean uidExpunge(String idSet) throws IOException {
		return new UidExpungeCommand(bundle, idSet).execute().booleanValue();
	}

	public IdleContext idle(IdleListener listener) throws IOException {
		return new IdleCommand(bundle, listener).execute();
	}

}
