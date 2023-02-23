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

import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeCacertUids;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeRevocation;
import net.bluemind.smime.cacerts.persistence.SmimeRevocationStore;
import net.bluemind.smime.cacerts.service.IInCoreSmimeRevocation;
import net.bluemind.smime.cacerts.utils.CrlReader;
import net.bluemind.utils.CertificateUtils;

public class InCoreSmimeRevocation implements IInCoreSmimeRevocation {

	public static class Factory implements IServerSideServiceFactory<IInCoreSmimeRevocation> {

		@Override
		public Class<IInCoreSmimeRevocation> factoryClass() {
			return IInCoreSmimeRevocation.class;
		}

		@Override
		public IInCoreSmimeRevocation instance(BmContext context, String... params) throws ServerFault {
			String domainUid = params[0];
			String containerUid = ISmimeCacertUids.domainCreatedCerts(domainUid);
			DataSource ds = DataSourceRouter.get(context, containerUid);

			ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
			Container container = null;
			try {
				container = containerStore.get(containerUid);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
			if (container == null) {
				throw new ServerFault("container " + containerUid + " not found", ErrorCode.NOT_FOUND);
			}

			return new InCoreSmimeRevocation(context, ds, container);
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(InCoreSmimeRevocation.class);

	private BmContext context;
	private Container container;
	private SmimeRevocationStore smimeStore;
	private RBACManager rbacManager;

	public InCoreSmimeRevocation(BmContext bmContext, DataSource pool, Container container) {
		this.context = bmContext;
		this.container = container;

		this.smimeStore = new SmimeRevocationStore(pool, container);
		rbacManager = RBACManager.forContext(bmContext).forContainer(container);
	}

	@Override
	public void create(SmimeRevocation revocation, ItemValue<SmimeCacert> cacertItem) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		try {
			smimeStore.create(revocation, cacertItem);
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage(), ErrorCode.SQL_ERROR);
		}
	}

	@Override
	public SmimeRevocation get(String serialNumber, ItemValue<SmimeCacert> cacertItem) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		try {
			return smimeStore.getBySn(serialNumber, cacertItem);
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage(), ErrorCode.SQL_ERROR);
		}
	}

	@Override
	public List<ItemValue<SmimeCacert>> getByNextUpdateDate(Date update) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		List<SmimeRevocation> revocationsToUpdate = Collections.emptyList();
		try {
			revocationsToUpdate = smimeStore.getByNextUpdateDate(Timestamp.from(update.toInstant()));
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage(), ErrorCode.SQL_ERROR);
		}

		if (revocationsToUpdate.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> caUids = revocationsToUpdate.stream().map(r -> r.cacertItemUid).distinct().toList();
		List<ItemValue<SmimeCacert>> cacerts = context.provider().instance(ISmimeCACert.class, container.uid)
				.multipleGet(caUids);

		return cacerts;
	}

	@Override
	public void fetchRevocations(ItemValue<SmimeCacert> cacertItem) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		try {
			logger.info("fetch revocations list for S/MIME certificate {}", cacertItem.uid);
			X509Certificate caCert = (X509Certificate) CertificateUtils
					.generateX509Certificate(cacertItem.value.cert.getBytes());
			CrlReader crlRead = new CrlReader(context, caCert, cacertItem.uid);
			List<SmimeRevocation> revocations = crlRead.getRevocations();
			smimeStore.batchInsert(revocations, cacertItem);
		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		}
	}

	@Override
	public void refreshRevocations(ItemValue<SmimeCacert> cacertItem) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		try {
			logger.info("delete revocations list for S/MIME certificate {}", cacertItem.uid);
			smimeStore.delete(cacertItem);
		} catch (SQLException e) {
			throw new ServerFault(e.getMessage(), ErrorCode.SQL_ERROR);
		}

		fetchRevocations(cacertItem);
	}
}
