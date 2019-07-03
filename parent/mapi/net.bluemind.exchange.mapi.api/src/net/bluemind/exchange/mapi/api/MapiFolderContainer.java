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
package net.bluemind.exchange.mapi.api;

public class MapiFolderContainer {

	public static final String TYPE = "mapi_folder";
	public static final String OUTLOOK_MAPI_FOLDER_PREFIX = "OUTLOOK-";

	public static String getIdentifier(String mapiKind, String localReplicaGuid) {
		return "mapi:" + mapiKind + ":" + localReplicaGuid;
	}

	public static String mapiKind(String uid) {
		int secOne = uid.indexOf(':', 5);
		return uid.substring(5, secOne);
	}

	public static boolean isLocalizedFolder(String uid) {
		return !uid.startsWith("mapi:" + OUTLOOK_MAPI_FOLDER_PREFIX);
	}

}
