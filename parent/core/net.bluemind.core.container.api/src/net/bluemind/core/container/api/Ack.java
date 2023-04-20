/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.container.api;

import java.util.Date;

import net.bluemind.core.api.BMApi;

/**
 * This class represent an acknowledgement to a server side operation that
 * assigned a new version number to an item.
 *
 */
@BMApi(version = "3")
public class Ack {

	/**
	 * Server version allocated as a result of an operation
	 */
	public long version;

	/**
	 * Server date of the operation
	 */
	public Date timestamp;

	public Ack() {
		this(0L, new Date());
	}

	private Ack(long v, Date d) {
		this.version = v;
		this.timestamp = d;
	}

	public static Ack create(long v, Date opTime) {
		return new Ack(v, opTime);
	}

}
