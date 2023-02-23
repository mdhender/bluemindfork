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

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.smime.cacerts.api.ISmimeRevocation;
import net.bluemind.smime.cacerts.api.RevocationResult;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeCertClient;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeRevocation;
import net.bluemind.smime.cacerts.persistence.SmimeRevocationStore;
import net.bluemind.smime.cacerts.service.IInCoreSmimeRevocation;

public class SmimeRevocationService implements ISmimeRevocation {

	private BmContext bmContext;
	private Container container;
	private RBACManager rbacManager;
	private final String domainUid;

	private SmimeRevocationStore storeService;

	public SmimeRevocationService(BmContext bmContext, DataSource pool, Container container) {
		this.bmContext = bmContext;
		this.container = container;
		this.domainUid = container.domainUid;

		storeService = new SmimeRevocationStore(pool, container);

		rbacManager = RBACManager.forContext(bmContext).forDomain(domainUid);
	}

	@Override
	public Set<RevocationResult> areRevoked(List<SmimeCertClient> clients) throws ServerFault {
		if (bmContext.getSecurityContext().isAnonymous()) {
			throw new ServerFault("User is not logged in", ErrorCode.PERMISSION_DENIED);
		}

		Set<RevocationResult> revokedList = clients.stream().map(c -> {
			try {
				SmimeRevocation byCertClient = storeService.getByCertClient(c);
				if (byCertClient == null) {
					byCertClient = (SmimeRevocation) SmimeRevocation.create(c.serialNumber, c.issuer);
				}
				return byCertClient;
			} catch (SQLException e) {
				throw new ServerFault(e.getMessage(), ErrorCode.SQL_ERROR);
			}
		}).distinct().map(r -> createRevocationResult(r)).collect(Collectors.toSet());

		return revokedList;
	}

	private RevocationResult createRevocationResult(SmimeRevocation revocation) {
		return revocation.revocationDate == null ? RevocationResult.notRevoked(revocation)
				: RevocationResult.revoked(revocation);
	}

	@Override
	public void refreshDomainRevocations() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		IServiceProvider provider = bmContext.provider();

		List<ItemValue<SmimeCacert>> cacertItems = null;
		cacertItems = provider.instance(ISmimeCACert.class, container.uid).all();

		IInCoreSmimeRevocation incoreService = provider.instance(IInCoreSmimeRevocation.class, container.domainUid);
		cacertItems.forEach(ca -> incoreService.refreshRevocations(ca));
	}

	@Override
	public void refreshRevocations(String cacertUid) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		ItemValue<SmimeCacert> cacert = bmContext.provider().instance(ISmimeCACert.class, container.uid)
				.getComplete(cacertUid);
		if (cacert == null) {
			throw new ServerFault(String.format("S/MIME cacert item %s not found", cacertUid));
		}

		bmContext.provider().instance(IInCoreSmimeRevocation.class, container.domainUid).refreshRevocations(cacert);
	}
	public List<SmimeRevocation> fetch(ItemValue<SmimeCacert> cacert) {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		try {
			return storeService.get(cacert);
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage(), ErrorCode.SQL_ERROR);
		}
	}

}
