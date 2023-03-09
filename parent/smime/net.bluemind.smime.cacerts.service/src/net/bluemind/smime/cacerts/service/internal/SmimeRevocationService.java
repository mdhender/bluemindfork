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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.smime.cacerts.api.ISmimeRevocation;
import net.bluemind.smime.cacerts.api.RevocationResult;

public class SmimeRevocationService implements ISmimeRevocation {

	private BmContext bmContext;
	private RBACManager rbacManager;
	private final String domainUid;

	public SmimeRevocationService(BmContext bmContext, String domainUid) {
		this.bmContext = bmContext;
		this.domainUid = domainUid;
		rbacManager = RBACManager.forContext(bmContext).forDomain(domainUid);
	}

	@Override
	public Set<RevocationResult> isRevoked(List<String> serialNumber) throws ServerFault {
		// TODO implement
		return serialNumber.stream().map(RevocationResult::notRevoked).collect(Collectors.toSet());
	}

}
