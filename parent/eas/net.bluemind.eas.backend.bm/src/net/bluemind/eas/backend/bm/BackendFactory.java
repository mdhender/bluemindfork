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
package net.bluemind.eas.backend.bm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IBackendFactory;
import net.bluemind.eas.store.ISyncStorage;

public class BackendFactory implements IBackendFactory {

	private static final Logger logger = LoggerFactory.getLogger(BackendFactory.class);

	@Override
	public IBackend create(ISyncStorage storage) {
		logger.info("Creating backend with storage {}...", storage);
		return new BMBackend(storage);
	}

}
