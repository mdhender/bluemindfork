package net.bluemind.central.reverse.proxy.vertx;

public class Auth {
	public String login;
	public String password;
	public boolean requireAuthentication;

	private Auth(String login, String password) {
		this.login = login;
		this.password = password;
		this.requireAuthentication = password != null;
	}

	public static Auth create(String login, String password) {
		return new Auth(login, password);
	}

	public static Auth create(String login) {
		return new Auth(login, null);
	}
}
