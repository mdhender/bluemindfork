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
package net.bluemind.milter;

import net.bluemind.config.Token;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.network.topology.Topology;

public class ClientContext implements IClientContext {

	private final ItemValue<Domain> senderDomain;
	private final ClientSideServiceProvider prov;

	public ClientContext(ItemValue<Domain> domain) {
		this.senderDomain = domain;
		String coreHost = Topology.get().core().value.address();
		this.prov = ClientSideServiceProvider.getProvider("http://" + coreHost + ":8090", Token.admin0());
	}

	public IServiceProvider provider() {
		return prov;
	}

	@Override
	public ItemValue<Domain> getSenderDomain() {
		return senderDomain;
	}

}
