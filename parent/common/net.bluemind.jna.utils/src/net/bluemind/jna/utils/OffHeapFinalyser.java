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
package net.bluemind.jna.utils;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OffHeapFinalyser extends PhantomReference<OffHeapTemporaryFile> {

	private final OneTimeClose cleaner;
	private static final Logger logger = LoggerFactory.getLogger(OffHeapFinalyser.class);

	public OffHeapFinalyser(OneTimeClose cleaner, OffHeapTemporaryFile referent,
			ReferenceQueue<? super OffHeapTemporaryFile> q) {
		super(referent, q);
		this.cleaner = cleaner;
	}

	/**
	 * @return true if we had to free resources
	 */
	public boolean finalizeResources() {
		if (!cleaner.isClosed()) {
			logger.warn("user failed to explicitely close {}, reclaiming automagically", cleaner.name());
			cleaner.run();
			return true;
		}
		return false;
	}

	public boolean isGone() {
		return cleaner.isClosed();
	}

}
