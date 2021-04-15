package net.bluemind.mailbox.service.hook;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.ISessionDeletionListener;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;

public class EmptyTrashLogoutHook implements ISessionDeletionListener {

	private static final Logger logger = LoggerFactory.getLogger(EmptyTrashLogoutHook.class);

	@Override
	public void deleted(String identity, String sid, SecurityContext securityContext) {
		if (securityContext.isInteractive()) {
			String domainUid = securityContext.getContainerUid();
			String userUid = securityContext.getSubject();
			String userLogin = getUserLogin(securityContext);
			Map<String, String> userSettings = getUserSettings(securityContext, userUid);
			boolean logoutPurge = Boolean.parseBoolean(userSettings.getOrDefault("logout_purge", "false"));
			if (userLogin == null || !logoutPurge) {
				return;
			}
			emptyTrash(securityContext, domainUid, userLogin);
			logger.info("Trash folder cleared on logout for {}@{}", userUid, domainUid);
		}
	}

	private String getUserLogin(SecurityContext securityContext) {
		ItemValue<User> userItem = ServerSideServiceProvider.getProvider(securityContext)
				.instance(IUser.class, securityContext.getContainerUid()).getComplete(securityContext.getSubject());
		if (userItem == null) {
			logger.error("User not found");
			return null;
		}
		return userItem.value.login;
	}

	private Map<String, String> getUserSettings(SecurityContext securityContext, String userUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, securityContext.getContainerUid()).get(userUid);
	}

	private void emptyTrash(SecurityContext securityContext, String domainUid, String userLogin) {
		String root = "user." + userLogin.replace('.', '^');
		String partition = domainUid.replace('.', '_');
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(securityContext);
		IMailboxFolders mailboxFoldersService = provider.instance(IMailboxFolders.class, partition, root);
		ItemValue<MailboxFolder> trashItemValue = mailboxFoldersService.byName("Trash");
		mailboxFoldersService.emptyFolder(trashItemValue.internalId);
	}

}
