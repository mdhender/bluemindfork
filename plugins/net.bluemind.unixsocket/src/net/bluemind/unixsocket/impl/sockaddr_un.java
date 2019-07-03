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
package net.bluemind.unixsocket.impl;

import com.sun.jna.Structure;

/**
 * A Unix domain socket address is represented in the following structure:
 * 
 * <code>
 * #define UNIX_PATH_MAX 108
 * 
 * struct sockaddr_un { 
 *   sa_family_t sun_family; 
 *   char sun_path[UNIX_PATH_MAX];
 * };
 * </code>
 * 
 * sun_family always contains AF_UNIX.
 * 
 * Look man unix(7) for more infos
 */
public class sockaddr_un extends Structure {
	public short sun_family;
	public byte[] sun_path;

	public sockaddr_un(short family, byte[] path) {
		sun_family = family;
		sun_path = path;
		allocateMemory();
	}
}
