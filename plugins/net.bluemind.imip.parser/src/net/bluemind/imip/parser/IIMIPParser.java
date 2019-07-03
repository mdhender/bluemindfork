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
package net.bluemind.imip.parser;

import org.apache.james.mime4j.dom.Message;

/**
 * <a href="http://tools.ietf.org/html/rfc6047">iMIP rfc</a>
 * 
 * @author tom
 * 
 */
public interface IIMIPParser {

	/**
	 * Returns null if message is not IMIP related
	 * 
	 * @param m
	 * @return
	 */
	IMIPInfos parse(Message m);

}
