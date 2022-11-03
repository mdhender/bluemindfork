package net.bluemind.delivery.lmtp;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.delivery.lmtp.common.IDeliveryContext;
import net.bluemind.delivery.lmtp.common.IMailboxLookup;

public class DeliveryContext implements IDeliveryContext {

	private final IServiceProvider provider;
	private final IMailboxLookup lookup;

	public DeliveryContext(IServiceProvider provider, IMailboxLookup lookup) {
		this.provider = provider;
		this.lookup = lookup;
	}

	@Override
	public IServiceProvider provider() {
		return provider;
	}

	@Override
	public IMailboxLookup mailboxLookup() {
		return lookup;
	}
}
