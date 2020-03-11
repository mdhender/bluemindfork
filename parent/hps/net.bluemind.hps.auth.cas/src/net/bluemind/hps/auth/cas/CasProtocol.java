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
package net.bluemind.hps.auth.cas;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.base.Strings;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
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
import net.bluemind.utils.DOMUtils;

public class CasProtocol implements IAuthProtocol {
	private static final Logger logger = LoggerFactory.getLogger(CasProtocol.class);

	private String casURL;
	private String casDomain;
	private String callbackURL;
	private HttpClient httpClient;
	private String baseUri;

	public CasProtocol(HttpClient client, String casURL, String baseUri, String casDomain, String callbackURL) {
		this.httpClient = client;
		this.casURL = casURL;
		this.casDomain = casDomain;
		this.callbackURL = callbackURL;
		this.baseUri = baseUri;
	}

	@Override
	public void proceed(AuthRequirements authState, ISessionStore ss, IAuthProvider prov, HttpServerRequest req) {
		if (req.params().get("ticket") == null) {
			if (req.path().endsWith("bluemind_sso_logout")
					&& !Strings.isNullOrEmpty(req.headers().get(HttpHeaders.REFERER))
					&& req.headers().get(HttpHeaders.REFERER).toLowerCase().equals(getCasLogoutUrl())) {
				HttpServerResponse resp = req.response();
				resp.setStatusCode(200);
				resp.end();
				return;
			}

			redirectToCasServer(req);
		} else {
			// validate cas ticket
			validateTicket(req, authState.protocol, prov, ss);
		}
	}

	private void validateTicket(HttpServerRequest req, IAuthProtocol protocol, IAuthProvider prov, ISessionStore ss) {
		List<String> forwadedFor = new ArrayList<>(req.headers().getAll("X-Forwarded-For"));
		forwadedFor.add(req.remoteAddress().host());

		String ticket = req.params().get("ticket");
		String validationURI = baseUri + "serviceValidate?service=" + callbackTo(req) + "&ticket=" + ticket;

		logger.info("validate CAS ticket {} : {}", ticket, validationURI);
		HttpClientRequest casReq = httpClient.get(validationURI, res -> {
			logger.info("receive resp {}", res.statusCode());
			if (res.statusCode() >= 400) {
				logger.error("error during cas ticket validation {} : {}", res.statusCode(), res.statusMessage());
				replyError(req);
				return;
			}

			res.exceptionHandler(event -> {
				logger.error("error during cas ticket validation ", event);
				replyError(req);
			});

			res.bodyHandler(body -> validationUriResponseBody(req, protocol, prov, ss, forwadedFor, ticket, body));
		});

		casReq.exceptionHandler(e -> {
			logger.error("error during cas auth", e);
			req.response().setStatusCode(500);
			req.response().end();
		});

		casReq.end();

	}

	private void validationUriResponseBody(HttpServerRequest req, IAuthProtocol protocol, IAuthProvider prov,
			ISessionStore ss, List<String> forwadedFor, String ticket, Buffer body) {
		Optional<ExternalCreds> optionalCreds = validateCasTicket(ticket, body);
		if (optionalCreds.isPresent()) {
			ExternalCreds creds = optionalCreds.get();

			logger.info("Create session for {}", creds.getLoginAtDomain());
			prov.sessionId(creds, forwadedFor, new AsyncHandler<String>() {
				@Override
				public void success(String sid) {
					if (sid == null) {
						logger.error("Error during cas auth, {} login not valid (not found/archived or not user)",
								creds.getLoginAtDomain());
						req.response().headers().add(HttpHeaders.LOCATION,
								String.format("/errors-pages/deniedAccess.html?login=%s", creds.getLoginAtDomain()));
						req.response().setStatusCode(302);
						req.response().end();
						return;
					}

					// get cookie...
					String proxySid = ss.newSession(sid, protocol);

					logger.info("Got sid: {}, proxySid: {}", sid, proxySid);

					Cookie co = new DefaultCookie("BMHPS", proxySid);
					co.setPath("/");
					co.setHttpOnly(true);
					if (SecurityConfig.secureCookies) {
						co.setSecure(true);
					}
					req.response().headers().add(HttpHeaders.LOCATION, "/");
					req.response().setStatusCode(302);

					req.response().headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(co));
					req.response().end();
				}

				@Override
				public void failure(Throwable e) {
					logger.error(String.format("error during cas auth for user %s", creds.getLoginAtDomain()), e);
					req.response().setStatusCode(500);
					req.response().end();
				}

			});
		} else {
			logger.error("error during cas auth, no creds, redirect to login");
			redirectToCasServer(req);
		}
	}

	private void replyError(HttpServerRequest req) {
		logger.error("error during cas auth");
		req.response().setStatusCode(500);
		req.response().end();
	}

	private Optional<ExternalCreds> validateCasTicket(String ticket, Buffer responseData) {
		logger.debug("[CAS] Debug : \n {}", responseData);

		InputStream body = new ByteArrayInputStream(responseData.getBytes());
		// Parse XML response to find if the authentication was successful
		Document document = null;
		try {
			document = DOMUtils.parse(body);
		} catch (SAXException | IOException | ParserConfigurationException | FactoryConfigurationError e1) {
			throw new RuntimeException(e1);
		}

		Element status = DOMUtils.getUniqueElement(document.getDocumentElement(), "cas:authenticationSuccess");
		if (status != null) {
			String userName = DOMUtils.getUniqueElement(document.getDocumentElement(), "cas:user").getTextContent();
			if (!Strings.isNullOrEmpty(userName)) {
				// OK we've got an user
				logger.info("[CAS] Ticket validation successful for user : " + userName);

				ExternalCreds creds = new ExternalCreds();
				creds.setTicket(ticket);
				if (userName.contains("@")) {
					creds.setLoginAtDomain(userName.toLowerCase());
				} else {
					creds.setLoginAtDomain(String.format("%s@%s", userName.toLowerCase(), casDomain.toLowerCase()));
				}

				return Optional.of(creds);
			}
		} else {
			logger.warn("Missing status in XML from CAS:\n{}", responseData);
		}

		return Optional.empty();

	}

	private void redirectToCasServer(HttpServerRequest req) {
		// Only works with CAS authentication for now
		String location = casURL + "login?service=";

		location += callbackTo(req);
		req.response().headers().add(HttpHeaders.LOCATION, location);
		req.response().setStatusCode(302);
		req.response().end();
	}

	private String callbackTo(HttpServerRequest req) {

		String callbackTo = callbackURL;

		if (!req.path().startsWith("/")) {
			callbackTo = callbackTo + "/";
		} else {

			callbackTo = callbackTo + req.path();
		}

		try {
			return URLEncoder.encode(callbackTo, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void logout(HttpServerRequest event) {
		if (!Strings.isNullOrEmpty(event.headers().get(HttpHeaders.REFERER))
				&& event.headers().get(HttpHeaders.REFERER).toLowerCase().equals(getCasLogoutUrl())) {
			HttpServerResponse resp = event.response();
			resp.setStatusCode(200);
			resp.end();
			return;
		}

		HttpServerResponse resp = event.response();
		resp.headers().add(HttpHeaders.LOCATION, getCasLogoutUrl());
		resp.setStatusCode(302);
		resp.end();
	}

	private String getCasLogoutUrl() {
		return String.format("%slogout", casURL).toLowerCase();
	}
}
