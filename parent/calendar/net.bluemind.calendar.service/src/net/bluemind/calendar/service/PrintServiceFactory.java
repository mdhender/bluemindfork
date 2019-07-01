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
package net.bluemind.calendar.service;

import net.bluemind.calendar.api.IPrint;
import net.bluemind.calendar.service.internal.PrintService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class PrintServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IPrint> {

	private IPrint getService(BmContext context) throws ServerFault {

		IPrint service = new PrintService(context);

		return service;
	}

	@Override
	public Class<IPrint> factoryClass() {
		return IPrint.class;
	}

	@Override
	public IPrint instance(BmContext context, String... params) throws ServerFault {
		return getService(context);
	}

}
