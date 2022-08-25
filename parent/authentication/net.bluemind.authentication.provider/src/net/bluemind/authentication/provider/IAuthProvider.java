package net.bluemind.authentication.provider;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.User;

public interface IAuthProvider {
	public interface IAuthContext {
		public SecurityContext getSecurityContext();

		public ItemValue<Domain> getDomain();

		public ItemValue<User> getUser();

		public String getRealUserLogin();

		public String getUserPassword();
	}

	public enum AuthResult {
		YES, EXPIRED, NO, UNKNOWN, ARCHIVED
	}

	int priority();

	AuthResult check(IAuthContext authContext) throws ServerFault;
}
