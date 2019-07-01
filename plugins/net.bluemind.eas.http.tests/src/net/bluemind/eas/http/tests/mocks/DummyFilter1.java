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
package net.bluemind.eas.http.tests.mocks;

import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestFilter;

public class DummyFilter1 implements IEasRequestFilter {

	public static boolean executed;

	@Override
	public void filter(AuthenticatedEASQuery query, FilterChain next) {
		executed = true;
		System.out.println(" FILTER " + getClass().getName());
		next.filter(query);
	}

	@Override
	public int priority() {
		return 1;
	}

	@Override
	public void filter(AuthorizedDeviceQuery query, FilterChain next) {
		next.filter(query);
	}

}
