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
package net.bluemind.imap.vt.cmd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.Field;

import com.google.common.base.Splitter;

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vt.dto.UidFetched;
import net.bluemind.imap.vt.parsing.ImapDateParser;
import net.bluemind.imap.vt.parsing.IncomingChunk;
import net.bluemind.jna.utils.OffHeapTemporaryFile;

public class UidFetchHeadersCommand extends TaggedCommand<List<UidFetched>> {

	private final String idSet;
	private final String[] headers;

	public UidFetchHeadersCommand(CommandContext ctx, String idSet, String... headers) {
		super(ctx);
		this.idSet = idSet;
		this.headers = Arrays.stream(headers).map(s -> s.toLowerCase()).toArray(String[]::new);
	}

	@Override
	protected void buildCommand(Buffer b) {
		b.appendString("UID FETCH " + idSet + " (INTERNALDATE FLAGS");
		if (headers != null && headers.length > 0) {
			b.appendString(" BODY.PEEK[HEADER.FIELDS ")
					.appendString(Arrays.stream(headers).collect(Collectors.joining(" ", "(", ")]")));
		}
		b.appendString(")");
	}

	private static final Pattern fetched = Pattern
			.compile("\\* \\d+ FETCH \\(UID (\\d+) INTERNALDATE \"([^\"]+)\" FLAGS \\(([^\\)]*)\\).*");
	private static final Splitter FL_SPLIT = Splitter.on(' ').omitEmptyStrings();

	@Override
	protected List<UidFetched> processChunks(List<IncomingChunk> chunks) throws IOException {
		List<UidFetched> res = new ArrayList<>(chunks.size());
		var last = chunks.getLast();
		for (var ic : chunks) {
			if (ic == last) {
				break;
			}
			String t = ic.pieces().getFirst().txt();
			Matcher m = fetched.matcher(t);
			if (m.find()) {
				int uid = Integer.parseInt(m.group(1));
				Date internalDate = ImapDateParser.readDateTime(m.group(2));
				Set<String> flags = FL_SPLIT.splitToStream(m.group(3)).collect(Collectors.toSet());
				Map<String, String> parsedHeader = new HashMap<>();
				if (headers != null && headers.length > 0) {
					OffHeapTemporaryFile headerBlob = ic.pieces().get(1).bin();
					try (var in = headerBlob.openForReading()) {
						Header header = new DefaultMessageBuilder().parseHeader(in);
						for (String h : headers) {
							Field f = header.getField(h);
							if (f != null) {
								parsedHeader.put(h, f.getBody());
							} else {
								parsedHeader.put(h, null);
							}
						}
					}
				}
				res.add(new UidFetched(uid, parsedHeader, internalDate.getTime(), flags));
			}
		}
		return res;
	}

}
