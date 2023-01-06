/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.dataprotect.sdsspool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.github.luben.zstd.RecyclingBufferPool;
import com.github.luben.zstd.ZstdInputStream;

import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.MgetRequest;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsError;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.dto.TierMoveRequest;
import net.bluemind.sds.dto.TierMoveResponse;
import net.bluemind.sds.store.ISdsSyncStore;

public class SdsDataProtectSpool implements ISdsSyncStore {
	public static final Path DEFAULT_PATH = Paths.get("/var/backups/bluemind/sds-spool/spool");
	private final Path root;

	public SdsDataProtectSpool() {
		this(DEFAULT_PATH);
	}

	public SdsDataProtectSpool(Path root) {
		this.root = root;
	}

	public Path path() {
		return root;
	}

	public Path livePath(String guid) {
		return root.resolve(String.valueOf(guid.charAt(0))).resolve(String.valueOf(guid.charAt(1)))
				.resolve(guid + ".zst");
	}

	private void get(String guid, OutputStream out) throws IOException {
		Path inpath = livePath(guid);
		try (InputStream rawin = Files.newInputStream(inpath);
				ZstdInputStream in = new ZstdInputStream(rawin, RecyclingBufferPool.INSTANCE)) {
			in.transferTo(out);
		}
	}

	private void getRaw(String guid, OutputStream out) throws IOException {
		Path inpath = livePath(guid);
		try (InputStream rawin = Files.newInputStream(inpath)) {
			rawin.transferTo(out);
		}
	}

	@Override
	public ExistResponse exists(ExistRequest req) {
		return ExistResponse.from(Files.exists(livePath(req.guid)));
	}

	@Override
	public SdsResponse upload(PutRequest req) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SdsResponse download(GetRequest req) {
		try (var out = Files.newOutputStream(Paths.get(req.filename))) {
			get(req.guid, out);
		} catch (IOException ie) {
			SdsResponse sr = new SdsResponse();
			sr.error = new SdsError(req.guid + " not found: " + ie.getMessage());
			return sr;
		}
		return SdsResponse.UNTAGGED_OK;
	}

	@Override
	public SdsResponse downloadRaw(GetRequest req) {
		try (var out = Files.newOutputStream(Paths.get(req.filename))) {
			getRaw(req.guid, out);
		} catch (IOException ie) {
			SdsResponse sr = new SdsResponse();
			sr.error = new SdsError(req.guid + " not found: " + ie.getMessage());
			return sr;
		}
		return SdsResponse.UNTAGGED_OK;
	}

	@Override
	public SdsResponse downloads(MgetRequest mget) {
		List<SdsResponse> errors = mget.transfers.stream().map(tx -> GetRequest.of(mget.mailbox, tx.guid, tx.filename))
				.parallel().map(this::download).filter(resp -> !resp.succeeded()).toList();
		if (!errors.isEmpty()) {
			SdsResponse error = new SdsResponse();
			var errMsg = new StringBuilder();
			for (var err : errors) {
				errMsg.append(err.error.toString());
			}
			error.error = new SdsError(errMsg.toString());
			return error;
		}
		return SdsResponse.UNTAGGED_OK;
	}

	@Override
	public SdsResponse delete(DeleteRequest req) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TierMoveResponse tierMove(TierMoveRequest tierMoveRequest) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
		// ok
	}
}
