package net.bluemind.mailbox.hook;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;

public class DefaultMailboxHook implements IMailboxHook {
	@Override
	public void onMailboxCreated(BmContext context, String domainUid, ItemValue<Mailbox> value)
			throws ServerFault {
	}

	@Override
	public void onMailboxUpdated(BmContext context, String domainUid, ItemValue<Mailbox> previousValue,
			ItemValue<Mailbox> value) throws ServerFault {
	}

	@Override
	public void onMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> value)
			throws ServerFault {
	}

	@Override
	public void onMailFilterChanged(BmContext context, String domainUid, ItemValue<Mailbox> value, MailFilter filter)
			throws ServerFault {
	}

	@Override
	public void onDomainMailFilterChanged(BmContext context, String domainUid, MailFilter filter)
			throws ServerFault {
	}
}
