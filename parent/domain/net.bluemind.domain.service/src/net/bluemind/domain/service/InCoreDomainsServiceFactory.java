package net.bluemind.domain.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IInCoreDomains;

public class InCoreDomainsServiceFactory extends DomainsServiceCommonFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IInCoreDomains> {

	@Override
	public Class<IInCoreDomains> factoryClass() {
		return IInCoreDomains.class;
	}

	@Override
	public IInCoreDomains instance(BmContext context, String... params) throws ServerFault {
		return instanceImpl(context);
	}
}
