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
package net.bluemind.core.backup.continuous.restore;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy.ExpectedBehaviour;
import net.bluemind.core.backup.continuous.RecordKey.Operation;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class CrpEntriesReader implements IOwnerChecker {
	private final ILiveStream crpStream;
	private final Set<String> knownOwners;
	private final Logger logger = LoggerFactory.getLogger(CrpEntriesReader.class);

	private record SimpleItemValue(String uid) {
	}

	private static ValueReader<SimpleItemValue> reader = JsonUtils.reader(new TypeReference<SimpleItemValue>() {
	});

	public CrpEntriesReader(ILiveStream crpStream) {
		this.crpStream = crpStream;
		this.knownOwners = ConcurrentHashMap.newKeySet(65535);
		this.knownOwners.add("system");
	}

	public boolean isKnown(String owner) {
		if (owner == null) {
			return true;
		}
		return knownOwners.contains(owner);
	}

	/**
	 * Returns a completable future which completes after all the entries where
	 * read. The stream continues afterward
	 * 
	 * @return
	 */
	public CompletableFuture<Void> continuousReader() {
		CompletableFuture<Void> fut = new CompletableFuture<>();
		new Thread(null, () -> { //
			crpStream.subscribe(null, de -> {
				if (de.key.type.equals("dir")) {
					Operation op = Operation.of(de.key);
					SimpleItemValue iv = reader.read(de.payload);
					if (Operation.CREATE.equals(op)) {
						knownOwners.add(iv.uid);
					} else if (Operation.DELETE.equals(op)) {
						knownOwners.remove(iv.uid);
					}
				}
			}, starved -> {
				fut.complete(null);
				logger.info("crp directory: {} owners available", knownOwners.size());
				return ExpectedBehaviour.RETRY;
			});
		}, "clone-restore-crp-reader").start();
		return fut;
	}
}
