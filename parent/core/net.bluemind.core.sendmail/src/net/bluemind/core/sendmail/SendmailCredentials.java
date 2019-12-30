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
}
