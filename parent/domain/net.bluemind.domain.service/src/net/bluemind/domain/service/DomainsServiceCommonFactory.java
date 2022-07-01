package net.bluemind.domain.service;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.service.internal.DomainsService;

public abstract class DomainsServiceCommonFactory {

	protected DomainsService instanceImpl(BmContext context) throws ServerFault {

		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(DomainsContainerIdentifier.getIdentifier());
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("container " + DomainsContainerIdentifier.getIdentifier() + " not found");
		}
		return new DomainsService(context, container);
	}
}
