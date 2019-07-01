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
package net.bluemind.customproperties.service.tests;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.customproperties.api.ICustomProperties;

public class CustomPropertiesHttpTests extends CustomPropertiesTests {

	@Override
	protected ICustomProperties getService() throws ServerFault {
		IServiceProvider provider = ClientSideServiceProvider.getProvider("http://localhost:8090", null);

		return provider.instance(ICustomProperties.class);
	}

}
