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
package net.bluemind.user.persistence.security;

import net.bluemind.core.api.fault.ServerFault;

public interface Hash {

	public abstract String create(String plaintext) throws ServerFault;

	public abstract boolean validate(String plaintext, String hash) throws ServerFault;

	boolean matchesAlgorithm(String password);

	public default boolean needsUpgrade(String hash) {
		return true;
	}

}
