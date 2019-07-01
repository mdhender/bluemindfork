/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.system.service.internal;

import net.bluemind.system.service.RegisteredExternalSystem;

public class TestExternalSystem1 extends RegisteredExternalSystem {

	public TestExternalSystem1() {
		super("TestSystem1", "System 1", AuthKind.SIMPLE_CREDENTIALS);
	}

	@Override
	public byte[] getLogo() {
		return null;
	}

	@Override
	public boolean handles(String userAccountIdentifier) {
		return userAccountIdentifier.startsWith(super.identifier);
	}

}
