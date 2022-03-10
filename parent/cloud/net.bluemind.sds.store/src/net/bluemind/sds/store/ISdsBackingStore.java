/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.sds.store;

import java.util.concurrent.CompletableFuture;

import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.MgetRequest;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsError;
import net.bluemind.sds.dto.SdsResponse;

public interface ISdsBackingStore {

	CompletableFuture<ExistResponse> exists(ExistRequest req);

	CompletableFuture<SdsResponse> upload(PutRequest req);

	CompletableFuture<SdsResponse> download(GetRequest req);

	default CompletableFuture<SdsResponse> downloads(MgetRequest req) {
		int len = req.transfers.size();
		CompletableFuture<?>[] futures = new CompletableFuture[len];
		GetRequest[] asGet = req.transfers.stream().map(tx -> GetRequest.of(req.mailbox, tx.guid, tx.filename))
				.toArray(GetRequest[]::new);
		for (int i = 0; i < len; i++) {
			final int slot = i;
			futures[slot] = download(asGet[slot]);
		}
		return CompletableFuture.allOf(futures).thenApply(v -> SdsResponse.UNTAGGED_OK).exceptionally(ex -> {
			SdsResponse error = new SdsResponse();
			error.error = new SdsError(ex.getMessage());
			return error;
		});
	}

	CompletableFuture<SdsResponse> delete(DeleteRequest req);

	public void close();
}
