/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.tests;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.ContainerSettingsKeys;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class DbMailboxRecordsServiceNoFastSortTests extends DbMailboxRecordsServiceTests {

	@Override
	protected void hookBeforeSort() {
		try {
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class);
			var containersApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);
			containersApi.all(ContainerQuery.ownerAndType(userUid, "mailbox_records")).stream().forEach(cd -> {
				var containerMgmtApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IContainerManagement.class, cd.uid);
				containerMgmtApi.setSetting(ContainerSettingsKeys.mailbox_record_fast_sort_enabled.name(), "false");
			});

			try (var con = JdbcTestHelper.getInstance().getMailboxDataDataSource().getConnection()) {
				try (var st = con.createStatement()) {
					st.execute("TRUNCATE s_mailbox_record");
				}
			}
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}
}
