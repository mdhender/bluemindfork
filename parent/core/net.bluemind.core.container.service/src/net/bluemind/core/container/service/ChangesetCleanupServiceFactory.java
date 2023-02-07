package net.bluemind.core.container.service;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.ChangesetCleanupService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class ChangesetCleanupServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IChangesetCleanup> {

	@Override
	public Class<IChangesetCleanup> factoryClass() {
		return IChangesetCleanup.class;
	}

	@Override
	public IChangesetCleanup instance(BmContext context, String... params) {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}

		String serverUid = params[0];
		DataSource ds = context.getMailboxDataSource(serverUid);
		return new ChangesetCleanupService(ds, serverUid);
	}

}
