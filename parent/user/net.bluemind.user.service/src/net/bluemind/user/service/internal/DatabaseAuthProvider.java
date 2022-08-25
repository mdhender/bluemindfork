package net.bluemind.user.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.IUser;

public class DatabaseAuthProvider implements IAuthProvider {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseAuthProvider.class);

	@Override
	public AuthResult check(IAuthContext authContext) throws ServerFault {
		ItemValue<Domain> domain = authContext.getDomain();
		String login = authContext.getRealUserLogin();
		if (domain == null || login == null) {
			return AuthResult.UNKNOWN;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("check {}@{} with password {}", login, domain.value.name, authContext.getUserPassword());
		}

		UserService userService = (UserService) ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domain.uid);

		PasswordInfo passwordInfo = userService.getPasswordInfo(login, authContext.getUserPassword());

		if (!passwordInfo.passwordOk) {
			return AuthResult.NO;
		}

		if (passwordInfo.passwordUpdateNeeded) {
			return AuthResult.EXPIRED;
		}

		DirEntry userAsEntry = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDirectory.class, domain.uid).findByEntryUid(passwordInfo.userUid);

		if (userAsEntry.archived) {
			return AuthResult.ARCHIVED;
		}

		return AuthResult.YES;
	}

	@Override
	public int priority() {
		return Integer.MIN_VALUE;
	}

}
