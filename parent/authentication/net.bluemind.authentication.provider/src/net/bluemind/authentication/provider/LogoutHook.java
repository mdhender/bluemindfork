package net.bluemind.authentication.provider;

import net.bluemind.authentication.api.AuthUser;
import net.bluemind.core.context.SecurityContext;

public interface LogoutHook {

	void beforeLogout(SecurityContext securityContext, AuthUser user);

}
