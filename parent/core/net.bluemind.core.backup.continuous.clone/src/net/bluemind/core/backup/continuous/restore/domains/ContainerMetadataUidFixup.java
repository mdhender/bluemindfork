/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.core.backup.continuous.restore.domains;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.ContainerMetadata;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.IDtoPreProcessor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.IServiceProvider;

public class ContainerMetadataUidFixup implements IDtoPreProcessor<ContainerMetadata> {

	private RestoreState state;

	public ContainerMetadataUidFixup(RestoreState state) {
		this.state = state;
	}

	@Override
	public VersionnedItem<ContainerMetadata> fixup(RestoreLogger log, IServiceProvider target, RecordKey k,
			VersionnedItem<ContainerMetadata> v) {
		v.uid = state.uidAlias(v.uid);
		v.value.contDesc.uid = state.uidAlias(v.value.contDesc.uid);
		if (v.value.acls != null) {
			for (AccessControlEntry ace : v.value.acls) {
				ace.subject = state.uidAlias(ace.subject);
			}
		}
		return v;
	}

}
