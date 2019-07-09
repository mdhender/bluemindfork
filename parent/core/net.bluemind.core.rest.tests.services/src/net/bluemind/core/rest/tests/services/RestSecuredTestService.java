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
package net.bluemind.core.rest.tests.services;

import net.bluemind.core.context.SecurityContext;

public class RestSecuredTestService implements IRestSecuredTestService {

	public RestSecuredTestService(SecurityContext securityContext) {
	}

	@Override
	public String helloMaster() {
		System.err.println("sir, yes sir !");
		return "sir, yes sir !";
	}

	@Override
	public String helloSlave() {
		return "slave..";
	}

	@Override
	public String helloSimple() {
		return "hello";
	}
}