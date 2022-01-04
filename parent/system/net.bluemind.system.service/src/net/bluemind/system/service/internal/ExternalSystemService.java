/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.service.internal;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.ConnectionTestStatus;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.IExternalSystem;
import net.bluemind.system.service.ExternalSystemsRegistry;
import net.bluemind.user.api.UserAccount;

public class ExternalSystemService implements IExternalSystem {

	private final String domain;

	public ExternalSystemService(BmContext context) {
		this.domain = context.getSecurityContext().getContainerUid();
	}

	@Override
	public List<ExternalSystem> getExternalSystems() throws ServerFault {
		return ExternalSystemsRegistry.getExternalSystems();
	}

	@Override
	public ExternalSystem getExternalSystem(String systemIdentifier) throws ServerFault {
		return ExternalSystemsRegistry.getExternalSystem(systemIdentifier);
	}

	@Override
	public byte[] getLogo(String systemIdentifier) throws ServerFault {
		return ExternalSystemsRegistry.getLogo(systemIdentifier);
	}

	@Override
	public ConnectionTestStatus testConnection(String systemIdentifier, UserAccount account) {
		return ExternalSystemsRegistry.testConnection(domain, systemIdentifier, account);
	}

}
