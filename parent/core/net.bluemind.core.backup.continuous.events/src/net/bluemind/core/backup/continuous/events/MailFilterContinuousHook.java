package net.bluemind.core.backup.continuous.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.dto.MailboxMailFilter;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.hook.DefaultMailboxHook;

public class MailFilterContinuousHook extends DefaultMailboxHook {

	private static final Logger logger = LoggerFactory.getLogger(MailFilterContinuousHook.class);

	@Override
	public void onMailFilterChanged(BmContext context, String domainUid, ItemValue<Mailbox> mailbox, MailFilter filter)
			throws ServerFault {
		ContainerDescriptor metaDesc = ContainerDescriptor.create(mailbox.uid + "_at_" + domainUid + "_mailfilter",
				mailbox.uid + " mailfilter", mailbox.uid, "mailfilters", domainUid, true);
		MailboxMailFilter mailboxFilter = new MailboxMailFilter(mailbox.uid, false, filter);
		ItemValue<MailboxMailFilter> iv = ItemValue.create(mailbox.uid, mailboxFilter);
		iv.internalId = iv.uid.hashCode();
		DefaultBackupStore.store().<MailboxMailFilter>forContainer(metaDesc).store(iv);
		logger.info("Saved mailfilter for {}", mailbox.uid);
	}

	@Override
	public void onDomainMailFilterChanged(BmContext context, String domainUid, MailFilter filter) throws ServerFault {
		ContainerDescriptor metaDesc = ContainerDescriptor.create(domainUid + "_mailfilter", domainUid + " mailfilter",
				domainUid, "mailfilters", domainUid, true);
		MailboxMailFilter mailboxFilter = new MailboxMailFilter(domainUid, true, filter);
		ItemValue<MailboxMailFilter> iv = ItemValue.create(domainUid, mailboxFilter);
		iv.internalId = iv.uid.hashCode();
		DefaultBackupStore.store().<MailboxMailFilter>forContainer(metaDesc).store(iv);
		logger.info("Saved mailfilter for {}", domainUid);
	}

}
