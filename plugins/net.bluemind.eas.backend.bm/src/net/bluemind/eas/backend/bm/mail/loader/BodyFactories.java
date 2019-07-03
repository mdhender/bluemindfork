/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.eas.backend.bm.mail.loader;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.storage.StorageBodyFactory;

import net.bluemind.mime4j.common.OffloadedBodyFactory;

public final class BodyFactories {

	public static final BodyFactory DISCARD_BODIES = discard();
	public static final BodyFactory OFFLOADED_BODIES = offload();

	private static BodyFactory discard() {
		DiscardBodyStorageProvider discard = new DiscardBodyStorageProvider();
		StorageBodyFactory sbf = new StorageBodyFactory(discard, DecodeMonitor.SILENT);
		return sbf;
	}

	private static BodyFactory offload() {
		return new OffloadedBodyFactory();
	}

}
