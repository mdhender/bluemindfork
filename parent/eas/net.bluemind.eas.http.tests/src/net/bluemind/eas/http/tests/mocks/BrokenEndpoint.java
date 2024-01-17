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

import java.util.Collection;

import com.google.common.collect.ImmutableList;

import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestEndpoint;

public class BrokenEndpoint implements IEasRequestEndpoint {

	@Override
	public void handle(final AuthorizedDeviceQuery event) {
		throw new NullPointerException("Broken endpoint");
	}

	@Override
	public Collection<String> supportedCommands() {
		return ImmutableList.of("Broken");
	}

	@Override
	public boolean acceptsVersion(double protocolVersion) {
		return true;
	}
}
