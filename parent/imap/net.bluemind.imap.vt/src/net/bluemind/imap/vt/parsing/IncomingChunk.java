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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.bluemind.jna.utils.MemfdSupport;
import net.bluemind.jna.utils.OffHeapTemporaryFile;

public class IncomingChunk {

	private static final AtomicLong alloc = new AtomicLong();
	private List<Atom> pieces;

	public static record Atom(String txt, OffHeapTemporaryFile bin) {
		public static Atom text(String txt) {
			return new Atom(txt, null);
		}

		public static Atom binary(byte[] bytes) throws IOException {
			OffHeapTemporaryFile memfd = MemfdSupport.newOffHeapTemporaryFile("imap-lit-" + alloc.incrementAndGet());
			try (OutputStream out = memfd.openForWriting()) {
				out.write(bytes);
			}
			return new Atom(null, memfd);
		}
	}

	public List<Atom> pieces() {
		return pieces;
	}

	public IncomingChunk() {
		this.pieces = new ArrayList<>(1);
	}

	public void add(Atom a) {
		pieces.add(a);
	}

	@Override
	public String toString() {
		return "Chunk" + pieces.stream().map(Atom::toString).collect(Collectors.joining(", ", "{", "}"));
	}

	public boolean tagged(String tag) {
		return pieces.getFirst().txt.startsWith(tag);
	}

	private static final Pattern statusPattern = Pattern.compile("^[^\s]+\s([^\s]+)\s");

	public boolean isOk() {
		String t = pieces.getFirst().txt;
		Matcher m = statusPattern.matcher(t);
		if (!m.find()) {
			return false;
		}
		String status = m.group(1);
		return "ok".equalsIgnoreCase(status);
	}

}
