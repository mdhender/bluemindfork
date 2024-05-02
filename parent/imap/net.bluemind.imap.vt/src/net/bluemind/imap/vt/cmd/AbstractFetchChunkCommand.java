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

import java.util.List;
import java.util.Optional;

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vt.dto.FetchedChunk;
import net.bluemind.imap.vt.parsing.IncomingChunk;
import net.bluemind.imap.vt.parsing.IncomingChunk.Atom;

public abstract class AbstractFetchChunkCommand extends TaggedCommand<FetchedChunk> {

	private final int uid;
	private final String section;

	protected AbstractFetchChunkCommand(CommandContext ctx, int uid, String section) {
		super(ctx);
		this.uid = uid;
		this.section = section;
	}

	@Override
	protected void buildCommand(Buffer b) {
		b.appendString("UID FETCH " + uid + " (BODY.PEEK[" + section + "])");
	}

	@Override
	protected FetchedChunk processChunks(List<IncomingChunk> chunks) {
		if (chunks.getLast().isOk()) {
			Optional<IncomingChunk> fetchResp = chunks.stream()
					.filter(ic -> ic.pieces().getFirst().txt().contains(" FETCH (")).findFirst();
			return fetchResp.map(this::asFetch).orElseGet(FetchedChunk::empty);

		} else {
			return FetchedChunk.empty();
		}
	}

	@SuppressWarnings("resource")
	private FetchedChunk asFetch(IncomingChunk ic) {
		return ic.pieces().stream().filter(a -> a.bin() != null).map(Atom::bin).findAny().map(FetchedChunk::new)
				.orElseGet(FetchedChunk::empty);
	}

}
