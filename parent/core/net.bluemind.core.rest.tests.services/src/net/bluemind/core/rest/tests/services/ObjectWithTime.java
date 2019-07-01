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

import net.bluemind.core.api.date.BmDateTime;

public class ObjectWithTime {

	public String subject;
	public BmDateTime date1;
	public String post;
	public BmDateTime date2;

	public BmDateTime dt;

	@Override
	public String toString() {
		return "subject: " + subject + ", ts1:" + date1.toString() + ", tz1: " + date1.timezone + ", ts2:"
				+ date2.toString() + ", tz2: " + date1.timezone + ", post: " + post;
	}

}
