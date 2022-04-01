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
package net.bluemind.core.backup.continuous.api;

public class CloneDefaults {

	private CloneDefaults() {
	}

	public static final String MARKER_FILE_PATH = "/etc/bm/continuous.clone"; // NOSONAR
	public static final String FORK_MARKER_PATH = "/etc/bm/continuous.fork"; // NOSONAR

	public static final String TARGET_MCAST_ID = "/etc/bm/mcast.id.clone"; // NOSONAR

	public static final String CLONE_STATE_PATH = "/etc/bm/clone.state.json"; // NOSONAR

}
