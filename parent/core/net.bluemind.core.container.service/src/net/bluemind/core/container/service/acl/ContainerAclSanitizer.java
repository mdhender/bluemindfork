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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.container.service.acl;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class ContainerAclSanitizer implements ISanitizer<ContainerAcl> {

	public static class ContainerAclSanitizerFactory implements ISanitizerFactory<ContainerAcl> {

		@Override
		public Class<ContainerAcl> support() {
			return ContainerAcl.class;
		}

		@Override
		public ISanitizer<ContainerAcl> create(BmContext context, Container container) {
			return new ContainerAclSanitizer(context, container);
		}

	}

	private BmContext context;
	private Container container;

	public ContainerAclSanitizer(BmContext context, Container container) {
		this.context = context;
		this.container = container;
	}

	@Override
	public void create(ContainerAcl obj) {
	}

	@Override
	public void update(ContainerAcl current, ContainerAcl obj) {
		boolean isMailboxAcl = container.type.equals(IMailboxAclUids.TYPE);
		boolean isMailshare = MailshareAclSanitize.isMailshare(context, container.domainUid, container.owner);
		if (isMailboxAcl && isMailshare) {
			new MailshareAclSanitize(current, obj).sanitize();
		}
	}

}