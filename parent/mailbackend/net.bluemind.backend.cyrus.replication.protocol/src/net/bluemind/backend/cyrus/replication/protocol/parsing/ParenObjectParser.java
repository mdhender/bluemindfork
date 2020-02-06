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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.replication.protocol.parsing;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Parses cyrus paren-based object notation into a json object tree.
 * 
 * <pre>
 * %(key1 value1 ... keyN valueN)
 * </pre>
 * 
 * is an object
 * 
 * <pre>
 * (value1 ... valueN)
 * </pre>
 * 
 * is an array
 * 
 * Values in an object can be an array, an object, a string (<em>foo</em>)or a
 * qstring (<em>"foo"</em>).
 * 
 * Values in an array can be an object, a string <em>blabla</em> or a qstring
 * <em>"blabla"</em>
 *
 */
public interface ParenObjectParser {

	public static ParenObjectParser create() {
		return new ZeroCopyParenObjectParser();
	}

	/**
	 * Returns a {@link JsonObject} or a {@link JsonArray}
	 * 
	 * @param s the cyrus paren-based object notation
	 * @return the parsed json element
	 */
	public JsonElement parse(String s);

}
