package net.bluemind.backend.postfix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.postfix.internal.maps.events.EventProducer;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.hook.DefaultMailboxHook;

public class MailboxHook extends DefaultMailboxHook {
	private static final Logger logger = LoggerFactory.getLogger(MailboxHook.class);

	@Override
	public void onMailboxCreated(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		EventProducer.dirtyMaps();
	}

	@Override
	public void onMailboxUpdated(BmContext context, String domainUid, ItemValue<Mailbox> previous,
			ItemValue<Mailbox> current) throws ServerFault {
		if (previous.value.equals(current.value)) {
			logger.debug("no changes for mailbox {} ", previous.uid);
			return;
		}

		EventProducer.dirtyMaps();
	}

	@Override
	public void onMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		// Delete are done on dir.entry.deleted event
	}
}
