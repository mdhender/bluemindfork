/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.hps.auth.krb;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.proxy.http.ExternalCreds;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.IAuthProtocol;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.ISessionStore;
import net.bluemind.proxy.http.auth.api.SecurityConfig;

public class KrbProtocol implements IAuthProtocol {
	private static final Logger logger = LoggerFactory.getLogger(KrbProtocol.class);
	private Map<String, String> domainMappings;

	public KrbProtocol(Map<String, String> domainMappings) {
		this.domainMappings = domainMappings;
	}

	@Override
	public void proceed(AuthRequirements authState, ISessionStore ss, IAuthProvider prov, HttpServerRequest req) {
		String authHeader = req.headers().get("Authorization");

		if (authHeader == null) {
			req.response().setStatusCode(401);
			req.response().end();
			return;
		}
		List<String> forwadedFor = new ArrayList<>(req.headers().getAll("X-Forwarded-For"));
		forwadedFor.add(req.remoteAddress().host());

		String b64 = authHeader.substring(10);
		byte[] ticket = Base64.getDecoder().decode(b64);

		try {
			LoginContext lc = new LoginContext("ServicePrincipalLoginContext");
			lc.login();
			final GSSManager manager = GSSManager.getInstance();
			final PrivilegedExceptionAction<GSSCredential> action = new PrivilegedExceptionAction<GSSCredential>() {
				@Override
				public GSSCredential run() throws GSSException {
					return manager.createCredential(null, GSSCredential.DEFAULT_LIFETIME, new Oid("1.3.6.1.5.5.2"),
							GSSCredential.ACCEPT_ONLY);
				}
			};
			GSSCredential creds = Subject.doAs(lc.getSubject(), action);
			GSSContext gssContext = manager.createContext(creds);

			byte[] decoded = gssContext.acceptSecContext(ticket, 0, ticket.length);
			if (decoded != null) {
				GSSName src = gssContext.getSrcName();
				ExternalCreds ret = new ExternalCreds();
				String krbUserName = src.toString();
				int atIdx = krbUserName.indexOf('@');
				if (atIdx > 0) {
					String left = krbUserName.substring(0, atIdx);
					String dom = krbUserName.substring(atIdx + 1, krbUserName.length());
					if (domainMappings.containsKey(dom)) {
						krbUserName = left + "@" + domainMappings.get(dom);
					}
				}
				ret.setLoginAtDomain(krbUserName.toLowerCase());
				ret.setTicket(Base64.getEncoder().encodeToString(decoded));
				logger.info("Kerberos auth for user " + ret.getLoginAtDomain());

				// FIXME just to ease testing
				if ("thomas cataldo@willow.lan".equals(ret.getLoginAtDomain())) {
					ret.setLoginAtDomain("tom@willow.vmw");
				}

				prov.sessionId(ret, forwadedFor, new AsyncHandler<String>() {
					@Override
					public void success(String sid) {
						if (sid == null) {
							logger.error(
									"Error during kerberos auth, {} login not valid (not found/archived or not user)",
									ret.getLoginAtDomain());
							req.response().headers().add(HttpHeaders.LOCATION,
									String.format("/errors-pages/deniedAccess.html?login=%s", ret.getLoginAtDomain()));
							req.response().setStatusCode(302);
							req.response().end();
							return;
						}

						// get cookie...
						String proxySid = ss.newSession(sid, authState.protocol);

						logger.info("Got sid: {}, proxySid: {}", sid, proxySid);

						Cookie co = new DefaultCookie("BMHPS", proxySid);
						co.setPath("/");
						co.setHttpOnly(true);
						if (SecurityConfig.secureCookies) {
							co.setSecure(true);
						}

						Cookie bmPrivacyCookie = new DefaultCookie("BMPRIVACY", "true");
						bmPrivacyCookie.setPath("/");
						if (SecurityConfig.secureCookies) {
							bmPrivacyCookie.setSecure(true);
						}

						req.response().headers().add("Location", "/");
						req.response().setStatusCode(302);

						req.response().headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(co, bmPrivacyCookie));
						req.response().end();
					}

					@Override
					public void failure(Throwable e) {
						replyError(req);
					}

				});

			} else {
				replyError(req);
			}
		} catch (LoginException | GSSException | PrivilegedActionException e) {
			logger.error(e.getMessage(), e);
			replyError(req);
		}
	}

	private void replyError(HttpServerRequest req) {
		req.response().setStatusCode(500);
		req.response().end();
	}

	@Override
	public void logout(HttpServerRequest event) {
		HttpServerResponse resp = event.response();
		resp.headers().add("Location", "/");
		resp.setStatusCode(302);
		resp.end();
	}

	@Override
	public String getKind() {
		return "KRB";
	}
}
