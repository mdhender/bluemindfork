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
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxes;

public class DomainApis {

	public final ItemValue<Domain> domain;
	public final IMailboxes mailboxesApi;
	public final IDirectory dirApi;

	public DomainApis(ItemValue<Domain> domain, IMailboxes mailboxesApi, IDirectory dirApi) {
		this.domain = domain;
		this.mailboxesApi = mailboxesApi;
		this.dirApi = dirApi;
	}

}
