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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.videoconferencing.api;

import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.acl.AccessControlEntry;

@BMApi(version = "3")
public class VideoConferencingResourceDescriptor {

	public String label;
	public String provider;
	public List<AccessControlEntry> acls;

	public static VideoConferencingResourceDescriptor create(String label, String provider,
			List<AccessControlEntry> acls) {
		VideoConferencingResourceDescriptor desc = new VideoConferencingResourceDescriptor();
		desc.label = label;
		desc.provider = provider;
		desc.acls = acls;
		return desc;
	}
}
