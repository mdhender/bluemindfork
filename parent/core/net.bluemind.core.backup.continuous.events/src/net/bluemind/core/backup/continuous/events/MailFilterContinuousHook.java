package net.bluemind.core.backup.continuous.events;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.dto.MailboxMailFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.hook.DefaultMailboxHook;

public class MailFilterContinuousHook extends DefaultMailboxHook
		implements ContinuousContenairization<MailboxMailFilter> {

	@Override
	public String type() {
		return "mailfilters";
	}

	@Override
	public void onMailFilterChanged(BmContext context, String domainUid, ItemValue<Mailbox> mailbox, MailFilter filter)
			throws ServerFault {
		MailboxMailFilter mailboxFilter = new MailboxMailFilter(mailbox.uid, false, filter);
		save(domainUid, mailbox.uid, mailbox.uid, mailboxFilter, true);
	}

	@Override
	public void onDomainMailFilterChanged(BmContext context, String domainUid, MailFilter filter) throws ServerFault {
		MailboxMailFilter mailboxFilter = new MailboxMailFilter(domainUid, true, filter);
		save(domainUid, domainUid, domainUid, mailboxFilter, false);
	}

}
