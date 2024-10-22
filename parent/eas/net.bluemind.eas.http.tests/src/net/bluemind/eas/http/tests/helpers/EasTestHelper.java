/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.eas.http.tests.helpers;

import net.bluemind.eas.client.OPClient;

public class EasTestHelper<T> {

	public static final String SHARED_SEPARATOR = "__";
	public static final String SERVER_ID_SEPARATOR = ":";

	protected final OPClient client;

	public EasTestHelper(OPClient client) {
		this.client = client;
	}

	@SuppressWarnings("unchecked")
	public T execute(Runnable op) {
		op.run();
		return (T) this;
	}

}
