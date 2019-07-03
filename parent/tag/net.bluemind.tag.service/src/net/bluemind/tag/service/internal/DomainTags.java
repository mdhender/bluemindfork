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
package net.bluemind.tag.service.internal;

import javax.sql.DataSource;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.role.api.BasicRoles;

public class DomainTags extends Tags {

	public DomainTags(BmContext context, DataSource ds, Container container) {
		super(context, ds, container);
	}

	@Override
	protected void checkRead() {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());
	}

	@Override
	protected void checkWrite() {
		RBACManager.forContext(context).forDomain(container.domainUid).check(BasicRoles.ROLE_ADMIN, Verb.Write.name());
	}

}
