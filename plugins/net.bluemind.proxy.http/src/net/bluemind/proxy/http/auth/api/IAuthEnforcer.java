package net.bluemind.proxy.http.auth.api;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.impl.SessionStore.SidDataNotFound;

public interface IAuthEnforcer {

	public interface ISessionStore {
		String getSessionId(String cookieOrUrlHandle);

		String newSession(String providerSession, IAuthProtocol protocol);

		IAuthProtocol getProtocol(String sessionId);

		/**
		 * Is sessionId must be validated with core
		 * 
		 * @param sessionId
		 * @return true if core validation needed, false otherwise
		 * @throws SidDataNotFound if sessionSid not found
		 */
		boolean needCheck(String sessionId);

		void checked(String sessionId);
	}

	public interface IAuthProtocol {
		void proceed(AuthRequirements authState, ISessionStore ss, IAuthProvider provider, HttpServerRequest req);

		void logout(HttpServerRequest event);

		String getKind();
	}

	AuthRequirements enforce(ISessionStore checker, HttpServerRequest req);

	IAuthProtocol getProtocol();
}
