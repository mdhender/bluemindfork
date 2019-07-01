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
package net.bluemind.backend.mail.replica.api;

import java.math.BigInteger;

import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class QuotaRoot {

	/**
	 * eg. ex2016.vmw!user.tom
	 */
	public String root;

	/**
	 * Limit in KB
	 */
	public int limit;

	public QuotaRoot() {
	}

	public QuotaRoot(String root, int limit) {
		this.root = root;
		this.limit = limit;
	}

	public static QuotaRoot of(JsonObject js) {
		String limitStr = js.getString("LIMIT");
		BigInteger bi = new BigInteger(limitStr);
		int limitKb = 0;
		try {
			limitKb = bi.intValueExact();
		} catch (ArithmeticException ae) {
			// APPLY QUOTA %(ROOT ex2016.vmw!user.tom LIMIT 18446744073709551615)
		}
		return new QuotaRoot(js.getString("ROOT"), limitKb);
	}

	public String toParenObjectString() {
		return "%(ROOT " + root + " LIMIT " + limit + ")";
	}

}
