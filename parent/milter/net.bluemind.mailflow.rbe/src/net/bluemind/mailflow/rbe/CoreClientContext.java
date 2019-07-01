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
package net.bluemind.mailflow.rbe;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;

public class CoreClientContext implements IClientContext {

	private final ItemValue<Domain> senderDomain;

	public CoreClientContext(ItemValue<Domain> senderDomain) {
		this.senderDomain = senderDomain;
	}

	@Override
	public IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	@Override
	public ItemValue<Domain> getSenderDomain() {
		return senderDomain;
	}

}
