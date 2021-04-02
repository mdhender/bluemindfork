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
package net.bluemind.group.service.internal;

import java.util.HashMap;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.group.api.GroupSearchQuery;

public class GroupSearchQuerySanitizer implements ISanitizer<GroupSearchQuery> {

	public static class Factory implements ISanitizerFactory<GroupSearchQuery> {

		@Override
		public Class<GroupSearchQuery> support() {
			return GroupSearchQuery.class;
		}

		@Override
		public ISanitizer<GroupSearchQuery> create(BmContext context, Container container) {
			return new GroupSearchQuerySanitizer();
		}

	}

	@Override
	public void create(GroupSearchQuery obj) throws ServerFault {
		this.sanitize(obj);
	}

	@Override
	public void update(GroupSearchQuery current, GroupSearchQuery obj) throws ServerFault {
		this.sanitize(obj);
	}

	public void sanitize(GroupSearchQuery q) {
		if (q.properties == null) {
			q.properties = new HashMap<>();
		}
	}
}
