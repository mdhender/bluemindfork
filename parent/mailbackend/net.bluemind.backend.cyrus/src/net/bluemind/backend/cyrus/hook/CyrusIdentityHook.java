package net.bluemind.backend.cyrus.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.user.api.UserMailIdentity;
import net.bluemind.user.hook.identity.IUserMailIdentityHook;

public class CyrusIdentityHook implements IUserMailIdentityHook {
	private static final Logger logger = LoggerFactory.getLogger(CyrusIdentityHook.class);

	@Override
	public void beforeCreate(BmContext context, String domainUid, String uid, UserMailIdentity identity) {
		// Do nothing
	}

	@Override
	public void beforeUpdate(BmContext context, String domainUid, String uid, UserMailIdentity update,
			UserMailIdentity previous) {
		// Do nothing
	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, UserMailIdentity previous) {
		// Do nothing
	}

	@Override
	public void onIdentityUpdated(BmContext context, String domainUid, String userUid, UserMailIdentity current,
			UserMailIdentity previous) throws ServerFault {
		if (current.isDefault && !current.displayname.equals(previous.displayname)) {
			refreshCyrusSieve(context, domainUid, userUid);
		}
	}

	@Override
	public void onIdentityDefault(BmContext context, String domainUid, String userUid, String id) {
		refreshCyrusSieve(context, domainUid, userUid);
	}

	private void refreshCyrusSieve(BmContext context, String domainUid, String userUid) {
		IMailboxes mailboxesService = context.su().provider().instance(IMailboxes.class, domainUid);
		MailFilter filter = mailboxesService.getMailboxFilter(userUid);
		if (filter != null) {
			mailboxesService.setMailboxFilter(userUid, filter);
		}
	}

}
