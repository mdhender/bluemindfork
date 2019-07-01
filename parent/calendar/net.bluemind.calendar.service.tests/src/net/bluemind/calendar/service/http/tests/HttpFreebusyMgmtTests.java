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
package net.bluemind.calendar.service.http.tests;

import net.bluemind.calendar.api.IFreebusyMgmt;
import net.bluemind.calendar.service.tests.FreebusyMgmtTests;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;

public class HttpFreebusyMgmtTests extends FreebusyMgmtTests {

	@Override
	public IFreebusyMgmt getService(SecurityContext sc) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", sc.getSessionId())
				.instance(IFreebusyMgmt.class, container.uid);
	}

}
