/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.sds.proxy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <pre>
 * {
 *  "mailbox" : "devenv.blue!user.tom.Sent",
 *  "guid" : "6b95fdddb326aa46a31cbdd0e0210406861ccc78",
 *  "uid" : 44
 * }
 * </pre>
 * 
 * @author tom
 *
 */
public class RawMapping {

	@JsonProperty("mailbox")
	public String cyrusMailbox;
	public String guid;
	public long uid;

}
