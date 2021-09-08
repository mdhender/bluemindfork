/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.system.api;

import java.util.Collections;
import java.util.Map;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class CloneConfiguration {

	@BMApi(version = "3")
	public enum Mode {

		/**
		 * Keeps the clone loop running. {@link IInstallation#promoteLeader()} should be
		 * called on the tailing clone to trigger the promote process.
		 */
		TAIL,

		/**
		 * Once received events from upstream install are under a certain threshold,
		 * trigger {@link IInstallation#demoteLeader()} on the source installation.
		 */
		PROMOTE,

		/**
		 * eg. to create a pre-production copy
		 */
		FORK,
	}

	public String sourceInstallationId;

	public Map<String, String> uidToIpMapping = Collections.emptyMap();

	public Map<String, String> sysconfOverride = Collections.emptyMap();

	/**
	 * Defaults to {@link Mode#FORK}
	 */
	public Mode mode = Mode.FORK;

}
