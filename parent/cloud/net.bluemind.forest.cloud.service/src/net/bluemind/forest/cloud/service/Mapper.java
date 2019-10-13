/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.forest.cloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Mapper {

	private static final ObjectMapper om = createMapper();

	public static ObjectMapper get() {
		return om;
	}

	private Mapper() {
	}

	private static ObjectMapper createMapper() {
		ObjectMapper creating = new ObjectMapper();
		return creating;
	}
}
