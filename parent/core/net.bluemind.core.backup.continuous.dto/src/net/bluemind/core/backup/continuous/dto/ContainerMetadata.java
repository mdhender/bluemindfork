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
package net.bluemind.core.backup.continuous.dto;

import java.util.List;
import java.util.Map;

import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;

public class ContainerMetadata {

	public BaseContainerDescriptor contDesc;

	public List<AccessControlEntry> acls;

	public Map<String, String> settings;

	public static ContainerMetadata forAclsAndSettings(BaseContainerDescriptor bcd, List<AccessControlEntry> acls,
			Map<String, String> settings) {
		ContainerMetadata metadata = new ContainerMetadata();
		metadata.acls = acls;
		metadata.settings = settings;
		metadata.contDesc = bcd;
		return metadata;
	}

}
