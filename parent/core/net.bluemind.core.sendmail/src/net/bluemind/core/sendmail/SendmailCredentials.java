package net.bluemind.core.sendmail;

import net.bluemind.config.Token;

public class SendmailCredentials {

	public final String loginAtDomain;
	public final String authKey;

	private SendmailCredentials(String loginAtDomain, String authKey) {
		this.loginAtDomain = loginAtDomain;
		this.authKey = authKey;
	}

	public static SendmailCredentials as(String loginAtDomain, String authKey) {
		return new SendmailCredentials(loginAtDomain, authKey);
	}

	public static SendmailCredentials asAdmin0() {
		return new SendmailCredentials("admin0@global.virt", Token.admin0());
	}

	private boolean isAdminO() {
		return this.equals(asAdmin0());
	}

	public boolean notAdminAndNotCurrentUser(String fromEmail) {
		return !isAdminO() && !loginAtDomain.equals(fromEmail);

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loginAtDomain == null) ? 0 : loginAtDomain.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SendmailCredentials other = (SendmailCredentials) obj;
		if (loginAtDomain == null) {
			if (other.loginAtDomain != null)
				return false;
		} else if (!loginAtDomain.equals(other.loginAtDomain))
			return false;
		return true;
	}

}
