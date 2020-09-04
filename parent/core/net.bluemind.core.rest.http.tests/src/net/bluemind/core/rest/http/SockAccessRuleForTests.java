/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.core.rest.http;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IEventBusAccessRule;

public class SockAccessRuleForTests implements IEventBusAccessRule {

	@Override
	public boolean match(String path) {
		return path.startsWith("sockjs.tests");
	}

	@Override
	public boolean authorize(BmContext context, String path) {
		return true;
	}

}
