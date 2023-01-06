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
package net.bluemind.sds.sync.service;

import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.sds.sync.api.ISdsSync;
import net.bluemind.sds.sync.service.internal.queue.SdsSyncQueue;
import net.bluemind.sds.sync.service.internal.stream.CQSdsSyncReadStream;

public class SdsSyncService implements ISdsSync {
	private final RBACManager rbacManager;
	private static final long LAST_INDEX_QUEUE_EMPTY = -1;

	public static class SdsSyncServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<ISdsSync> {
		@Override
		public Class<ISdsSync> factoryClass() {
			return ISdsSync.class;
		}

		@Override
		public ISdsSync instance(BmContext context, String... params) throws ServerFault {
			return new SdsSyncService(context);
		}
	}

	public SdsSyncService(BmContext context) {
		rbacManager = new RBACManager(context);
	}

	@Override
	public Stream sync(long fromIndex) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_SYSTEM_MANAGER);
		return new CQSdsSyncReadStream(fromIndex);
	}

	@Override
	public long count(long fromIndex) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_SYSTEM_MANAGER);
		try (SdsSyncQueue q = new SdsSyncQueue()) {
			var lastIndex = q.queue().lastIndex();
			if (lastIndex == LAST_INDEX_QUEUE_EMPTY) {
				return 0L;
			}
			var startIndex = fromIndex > 0L ? fromIndex : q.queue().firstIndex();
			return q.queue().countExcerpts(startIndex, lastIndex);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

}