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
import io.vertx.core.http.HttpHeaders;
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

	@SuppressWarnings("serial")
	public static class UnsupportedCasFailureResponse extends RuntimeException {
	}

	public static class CASRequest {
		@SuppressWarnings("serial")
		public static class InvalidUrl extends RuntimeException {
			public InvalidUrl(Throwable e) {
				super(e);
			}
		}

		public final HttpServerRequest request;

		public final String domainUid;
		public final String ticket;
		public final String casUrl;

		public static CASRequest build(HttpServerRequest request) {
			String domainUid = DomainsHelper.getDomainUid(request);
			if ("global.virt".equals(domainUid)) {
				throw new ServerFault("No valid domain found for URL: " + request.host());
			}

			return build(request, domainUid);
		}

		public static CASRequest build(HttpServerRequest request, String domainUid) {
			String ticket = request.params().get("ticket");
			String casUrl = Optional
					.ofNullable(MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS).get(domainUid))
					.map(ds -> ds.get(AuthDomainProperties.CAS_URL.name()))
					.orElseThrow(() -> new RuntimeException("CAS URL not found for domain: " + domainUid));

			return new CASRequest(request, domainUid, ticket, casUrl);
		}

		private CASRequest(HttpServerRequest request, String domainUid, String ticket, String casUrl) {
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

		public void redirectToCasLogin() {
			request.response().headers().add(HttpHeaders.LOCATION, casUrl + "login?service=" + callbackTo());
			request.response().setStatusCode(302).end();
		}

		public String getValidationUri() {
			return casUrl + "serviceValidate?service=" + callbackTo() + "&ticket=" + ticket;
		}

		private String callbackTo() {
			try {
				return URLEncoder.encode("https://" + request.host() + "/auth/cas", "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new InvalidUrl(e);
			}
		}
	}

	@Override
	public void handle(HttpServerRequest request) {
		try {
			CASRequest casRequest = CASRequest.build(request);

			if (Strings.isNullOrEmpty(casRequest.ticket)) {
				logger.error("Handle CAS authentication, but no ticket found!");
				casRequest.redirectToCasLogin();
				return;
			}

			URI uri = new URI(casRequest.getValidationUri());

			if (logger.isDebugEnabled()) {
				logger.debug("[{}] Validating CAS ticket on {}", casRequest.ticket, casRequest.getValidationUri());
			}

			initHttpClient(uri).request(HttpMethod.GET, uri.getRawPath() + "?" + uri.getRawQuery())
					.onFailure(t -> error(request, t)) //
					.onSuccess(reqHandler -> reqHandler.send() //
							.onFailure(t -> error(request, t))
							.onSuccess(response -> response.bodyHandler(body -> validateToken(casRequest, body))));
		} catch (Exception e) {
			error(request, e);
		}
	}

	private void validateToken(CASRequest casRequest, Buffer body) {
		try {
			Document document = net.bluemind.utils.DOMUtils.parse(new ByteArrayInputStream(body.getBytes()));

			validateCasTicket(casRequest, document).ifPresentOrElse(creds -> createSession(casRequest.request,
					new AuthProvider(vertx), casRequest.getForwardFor(), creds, "/"), () -> {
						if (!casRequest.request.isEnded()) {
							casRequest.request.response().setStatusCode(500).end();
						}
					});
		} catch (SAXException | IOException | ParserConfigurationException | FactoryConfigurationError e) {
			logger.error("[{}] Invalid CAS ticket validation response: {}", casRequest.ticket,
					new String(body.getBytes()), e);
			casRequest.request.response().setStatusCode(500).end();
		} catch (Exception e) {
			logger.error("[{}] Unsupported CAS ticket validation response: {}", casRequest.ticket,
					new String(body.getBytes()), e);
			casRequest.request.response().setStatusCode(500).end();
		}
	}

	private Optional<ExternalCreds> validateCasTicket(CASRequest casRequest, Document document) {
		return Optional
				.ofNullable(DOMUtils.getUniqueElement(document.getDocumentElement(), "cas:authenticationSuccess"))
				.map(success -> handleCasAuthSuccess(casRequest, document)).orElseGet(() -> {
					handleCasAuthNotSuccess(casRequest, document);
					return Optional.empty();
				});
	}

	private void handleCasAuthNotSuccess(CASRequest casRequest, Document document) {
		Element authFail = Optional
				.ofNullable(DOMUtils.getUniqueElement(document.getDocumentElement(), "cas:authenticationFailure"))
				.orElseThrow(UnsupportedCasFailureResponse::new);

		manageFailure(casRequest, authFail);
	}

	private void manageFailure(CASRequest casRequest, Element authFail) {
		String authFailCode = authFail.getAttribute("code");

		logger.error("[{}] CAS ticket validation fail: {} - {}", casRequest.ticket, authFail.getAttribute("code"),
				authFail.getChildNodes().getLength() > 0 ? authFail.getChildNodes().item(0).getNodeValue().strip()
						: "unknown");

		switch (authFailCode) {
		case "UNAUTHORIZED_SERVICE":
			casRequest.request.response().setStatusCode(403).end();
			break;
		case "INVALID_TICKET":
			casRequest.redirectToCasLogin();
			break;
		default:
			casRequest.request.response().setStatusCode(500).end();
			break;
		}
	}

	private Optional<ExternalCreds> handleCasAuthSuccess(CASRequest casRequest, Document document) {
		String userName = DOMUtils.getUniqueElement(document.getDocumentElement(), "cas:user").getTextContent();
		if (Strings.isNullOrEmpty(userName)) {
			logger.error("[{}] No username found in CAS ticket validation response", casRequest.ticket);
			return Optional.empty();
		}

		// OK we've got an user
		logger.info("[{}] Ticket validation successful for user : {}", casRequest.ticket, userName);
		ExternalCreds creds = new ExternalCreds();
		creds.setTicket(casRequest.ticket);
		if (userName.contains("@")) {
			creds.setLoginAtDomain(userName.toLowerCase());
		} else {
			creds.setLoginAtDomain(String.format("%s@%s", userName.toLowerCase(), casRequest.domainUid));
		}

		return Optional.of(creds);
	}
}
