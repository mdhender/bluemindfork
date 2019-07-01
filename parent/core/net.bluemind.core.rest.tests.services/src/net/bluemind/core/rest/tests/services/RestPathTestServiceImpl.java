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

public class RestPathTestServiceImpl implements IRestPathTestService {

	private String param1;
	private String param2;

	public RestPathTestServiceImpl(String param1, String param2) {
		this.param1 = param1;
		this.param2 = param2;
	}

	@Override
	public String goodMorning(String post) {
		return "[" + param1 + "][" + param2 + "]good morning " + post;
	}

}
