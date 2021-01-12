package net.bluemind.mailbox.service.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.provider.LogoutHook;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class EmptyTrashLogoutHook implements LogoutHook {

	private static final Logger logger = LoggerFactory.getLogger(EmptyTrashLogoutHook.class);

	public void beforeLogout(SecurityContext securityContext, AuthUser user) {
		boolean logoutPurge = Boolean.parseBoolean(user.settings.getOrDefault("logout_purge", "false"));
		if (!securityContext.isInteractive() || !logoutPurge) {
			return;
		}
		String root = "user." + user.value.login.replace('.', '^');
		String partition = user.domainUid.replace('.', '_');
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(securityContext);
		IMailboxFolders mailboxFoldersService = provider.instance(IMailboxFolders.class, partition, root);
		ItemValue<MailboxFolder> trashItemValue = mailboxFoldersService.byName("Trash");

		mailboxFoldersService.emptyFolder(trashItemValue.internalId);
		logger.info("Trash folder cleared on logout for {}@{}", user.uid, user.domainUid);
	}

}
