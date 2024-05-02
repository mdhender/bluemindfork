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
package net.bluemind.imap.vt.parsing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import net.bluemind.imap.vt.ClientFault;
import net.bluemind.imap.vt.parsing.IncomingChunk.Atom;

public class BufHandler implements Handler<Buffer> {

	private static final Logger logger = LoggerFactory.getLogger(BufHandler.class);
	private final BlockingDeque<IncomingChunk> pending;
	private final RecordParser parser;
	private boolean text;
	private IncomingChunk current;

	public BufHandler(RecordParser rp, BlockingDeque<IncomingChunk> pending) {
		this.text = true;
		this.pending = pending;
		this.parser = rp;
		rp.handler(this);
		this.current = new IncomingChunk();
	}

	public void binary(int len) {
		this.text = false;
		parser.fixedSizeMode(len);
	}

	public void text() {
		this.text = true;
		parser.delimitedMode("\r\n");
	}

	@Override
	public void handle(Buffer event) {
		if (text) {
			String txt = event.toString(StandardCharsets.US_ASCII);
			Atom a = IncomingChunk.Atom.text(txt);
			current.add(a);
			@SuppressWarnings("deprecation")
			var buf = event.getByteBuf();
			int size = LiteralSize.of(buf);
			if (size > 0) {
				binary(size);
			} else {
				// no followup, last atom in chunk
				try {
					while (!pending.offer(current, 10, TimeUnit.SECONDS)) {
						logger.warn("Took 10sec to offer incoming chunk");
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				current = new IncomingChunk();
			}
		} else {
			try {
				current.add(IncomingChunk.Atom.binary(event.getBytes()));
				text();
			} catch (IOException e) {
				throw new ClientFault(e);
			}
		}
	}

}
