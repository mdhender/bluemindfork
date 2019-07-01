package net.bluemind.proxy.http.auth.api;

import org.vertx.java.core.http.HttpServerRequest;

import net.bluemind.proxy.http.IAuthProvider;

public interface IAuthEnforcer {

	public interface ISessionStore {
		String getSessionId(String cookieOrUrlHandle);

		String newSession(String providerSession, IAuthProtocol protocol);

		IAuthProtocol getProtocol(String sessionId);
	}

	public interface IAuthProtocol {
		void proceed(AuthRequirements authState, ISessionStore ss, IAuthProvider provider, HttpServerRequest req);

		void logout(HttpServerRequest event);
	}

	AuthRequirements enforce(ISessionStore checker, HttpServerRequest req);

}
