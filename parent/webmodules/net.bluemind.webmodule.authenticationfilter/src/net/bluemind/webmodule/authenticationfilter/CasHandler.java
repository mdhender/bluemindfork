/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.webmodule.authenticationfilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.base.Strings;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.utils.DOMUtils;
import net.bluemind.webmodule.authenticationfilter.internal.DomainsHelper;
import net.bluemind.webmodule.authenticationfilter.internal.ExternalCreds;

public class CasHandler extends AbstractAuthHandler implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(CasHandler.class);

	private static class RequestInfo {
		public final HttpServerRequest request;

		public final String domainUid;
		public final String ticket;
		public final String casUrl;

		public static RequestInfo build(HttpServerRequest request) {
			String domainUid = DomainsHelper.getDomainUid(request)
					.orElseThrow(() -> new ServerFault("No domain found for URL: " + request.host()));

			String ticket = request.params().get("ticket");
			String casURL = Optional
					.ofNullable(MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS).get(domainUid))
					.map(ds -> ds.get(AuthDomainProperties.CAS_URL.name()))
					.orElseThrow(() -> new ServerFault("CAS disabled or not configured for domain: " + domainUid));

			return new RequestInfo(request, domainUid, ticket, casURL);
		}

		private RequestInfo(HttpServerRequest request, String domainUid, String ticket, String casUrl) {
			this.request = request;
			this.domainUid = domainUid;
			this.ticket = ticket;
			this.casUrl = casUrl;
		}

		public List<String> getForwardFor() {
			List<String> forwadedFor = new ArrayList<>(request.headers().getAll("X-Forwarded-For"));
			forwadedFor.add(request.remoteAddress().host());
			return forwadedFor;
		}

		public String getValidationUri() {
			return casUrl + "serviceValidate?service=" + callbackTo(request) + "&ticket=" + ticket;
		}

		private String callbackTo(HttpServerRequest request) {
			try {
				return URLEncoder.encode(Optional.ofNullable(request.headers().get("X-Forwarded-Proto")).orElse("https")
						+ "://" + request.host() //
						+ (!request.path().startsWith("/") ? "/" : "") //
						+ request.path(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void handle(HttpServerRequest request) {
		if (Strings.isNullOrEmpty(request.params().get("ticket"))) {
			logger.error("Handle CAS authentication, but no ticket found!");
			request.response().setStatusCode(500);
			request.response().end();
			return;
		}

		try {
			RequestInfo requestInfo = RequestInfo.build(request);
			URI uri = new URI(requestInfo.getValidationUri());

			if (logger.isDebugEnabled()) {
				logger.debug("[{}] Validating CAS ticket on {}", requestInfo.ticket, requestInfo.getValidationUri());
			}

			initHttpClient(uri).request(HttpMethod.GET, uri.getRawPath() + "?" + uri.getRawQuery())
					.onFailure(t -> error(request, t)) //
					.onSuccess(reqHandler -> reqHandler.send() //
							.onFailure(t -> error(request, t))
							.onSuccess(response -> response.bodyHandler(body -> validateToken(requestInfo, body))));
		} catch (Exception e) {
			error(request, e);
			logger.error(e.getMessage(), e);
			request.response().end();
		}
	}

	private void validateToken(RequestInfo requestInfo, Buffer body) {
		try {
			Document document = net.bluemind.utils.DOMUtils.parse(new ByteArrayInputStream(body.getBytes()));

			validateCasTicket(requestInfo, document).ifPresentOrElse(creds -> createSession(requestInfo.request,
					new AuthProvider(vertx), requestInfo.getForwardFor(), creds, "/"), () -> {
						if (!requestInfo.request.isEnded()) {
							requestInfo.request.response().setStatusCode(500).end();
						}
					});
		} catch (SAXException | IOException | ParserConfigurationException | FactoryConfigurationError e) {
			logger.error("[{}] Invalid CAS ticket validation response: {}", requestInfo.ticket,
					new String(body.getBytes()), e);
			requestInfo.request.response().setStatusCode(500).end();
		} catch (Exception e) {
			logger.error("[{}] Unsupported CAS ticket validation response: {}", requestInfo.ticket,
					new String(body.getBytes()), e);
			requestInfo.request.response().setStatusCode(500).end();
		}
	}

	private Optional<ExternalCreds> validateCasTicket(RequestInfo requestInfo, Document document) {
		return Optional
				.ofNullable(DOMUtils.getUniqueElement(document.getDocumentElement(), "cas:authenticationSuccess"))
				.map(success -> handleCasAuthSuccess(requestInfo, document, success)).orElseGet(() -> {
					handleCasAuthNotSuccess(requestInfo, document);
					return Optional.empty();
				});
	}

	private void handleCasAuthNotSuccess(RequestInfo requestInfo, Document document) {
		Element authFail = Optional
				.ofNullable(DOMUtils.getUniqueElement(document.getDocumentElement(), "cas:authenticationFailure"))
				.orElseThrow(() -> new RuntimeException());

		manageFailure(requestInfo, authFail);
	}

	private void manageFailure(RequestInfo requestInfo, Element authFail) {
		String authFailCode = authFail.getAttribute("code");

		logger.error("[{}] CAS ticket validation fail: {} - {}", requestInfo.ticket, authFail.getAttribute("code"),
				authFail.getChildNodes().getLength() > 0 ? authFail.getChildNodes().item(0).getNodeValue().strip()
						: "unknown");

		switch (authFailCode) {
		case "UNAUTHORIZED_SERVICE":
			requestInfo.request.response().setStatusCode(403).end();
			break;
		case "INVALID_TICKET":
			DomainsHelper.redirectToCasServer(requestInfo.request, requestInfo.domainUid);
			break;
		default:
			requestInfo.request.response().setStatusCode(500).end();
			break;
		}
	}

	private Optional<ExternalCreds> handleCasAuthSuccess(RequestInfo requestInfo, Document document, Element status) {
		String userName = DOMUtils.getUniqueElement(document.getDocumentElement(), "cas:user").getTextContent();
		if (Strings.isNullOrEmpty(userName)) {
			logger.error("[{}] No username found in CAS ticket validation response", requestInfo.ticket);
			return Optional.empty();
		}

		// OK we've got an user
		logger.info("[{}] Ticket validation successful for user : {}", requestInfo.ticket, userName);
		ExternalCreds creds = new ExternalCreds();
		creds.setTicket(requestInfo.ticket);
		if (userName.contains("@")) {
			creds.setLoginAtDomain(userName.toLowerCase());
		} else {
			creds.setLoginAtDomain(String.format("%s@%s", userName.toLowerCase(), requestInfo.domainUid));
		}

		return Optional.of(creds);
	}
}
