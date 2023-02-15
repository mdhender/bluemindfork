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
package net.bluemind.smime.cacerts.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.smime.cacerts.api.ISmimeCRL;
import net.bluemind.smime.cacerts.api.RevocationResult;

public class SmimeCRLService implements ISmimeCRL {

	private BmContext bmContext;
	private RBACManager rbacManager;
	private final String domainUid;

	public SmimeCRLService(BmContext bmContext, String domainUid) {
		this.bmContext = bmContext;
		this.domainUid = domainUid;
		rbacManager = RBACManager.forContext(bmContext).forDomain(domainUid);
	}

	@Override
	public RevocationResult isRevoked(String serialNumber) throws ServerFault {
		// TODO implement
		return RevocationResult.notRevoked();
	}

}
