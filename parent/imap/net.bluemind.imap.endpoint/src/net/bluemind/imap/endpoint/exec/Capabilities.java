/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.exec;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Capabilities {

	private String all;

	public Capabilities() {
		this.all = Arrays.asList(//
				"IMAP4rev1", //
				"LITERAL+", //
				"ID", //
				"RIGHTS=kxten", //
				"QUOTA", //
				"UIDPLUS", //
				"XLIST", //
				"SPECIAL-USE", //
				"IDLE", //
				"BM-ROCKS" //
		).stream().collect(Collectors.joining(" "));
	}

	public String all() {
		return all;
	}

}
