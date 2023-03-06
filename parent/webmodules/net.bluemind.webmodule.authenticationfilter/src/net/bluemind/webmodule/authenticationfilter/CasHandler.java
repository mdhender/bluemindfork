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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
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

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.utils.DOMUtils;
import net.bluemind.webmodule.authenticationfilter.internal.ExternalCreds;

public class CasHandler extends AbstractAuthHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(CasHandler.class);

	@Override
	public void handle(HttpServerRequest event) {

		if (!Strings.isNullOrEmpty(event.params().get("ticket"))) {
			try {

				List<String> forwadedFor = new ArrayList<>(event.headers().getAll("X-Forwarded-For"));
				forwadedFor.add(event.remoteAddress().host());

				String ticket = event.params().get("ticket");
				SharedMap<String, String> sysconf = MQ.sharedMap(Shared.MAP_SYSCONF);
				String casURL = sysconf.get(SysConfKeys.cas_url.name());
				String externalURL = sysconf.get(SysConfKeys.external_url.name());

				String validationURI = casURL + "serviceValidate?service=" + callbackTo(event, externalURL) + "&ticket="
						+ ticket;

				URI uri = new URI(validationURI);

				HttpClient client = initHttpClient(uri);

				logger.info("validate CAS ticket {} : {}", ticket, validationURI);

				client.request(HttpMethod.GET, uri.getPath(), reqHandler -> {
					if (reqHandler.succeeded()) {
						HttpClientRequest r = reqHandler.result();
						r.response(respHandler -> {
							if (respHandler.succeeded()) {
								HttpClientResponse resp = respHandler.result();
								resp.bodyHandler(body -> validateToken(event, forwadedFor, ticket, body));

							}
						});
					} else {
						error(event, reqHandler.cause());
					}
				});
			} catch (Exception e) {
				error(event, e);
				logger.error(e.getMessage(), e);
			}
		}
		event.response().end();
	}

	private String callbackTo(HttpServerRequest req, String callbackURL) {
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

	private void validateToken(HttpServerRequest request, List<String> forwadedFor, String ticket, Buffer body) {
		Optional<ExternalCreds> optionalCreds = validateCasTicket(ticket, body);
		if (optionalCreds.isPresent()) {
			AuthProvider prov = new AuthProvider(vertx);
			// TODO openid stuff token endpoint here if needed
			createSession(request, prov, forwadedFor, optionalCreds.get(), "/");
		}
	}

	private Optional<ExternalCreds> validateCasTicket(String ticket, Buffer responseData) {
		logger.debug("[CAS] Debug : \n {}", responseData);

		InputStream body = new ByteArrayInputStream(responseData.getBytes());
		// Parse XML response to find if the authentication was successful
		Document document = null;
		try {
			document = net.bluemind.utils.DOMUtils.parse(body);
		} catch (SAXException | IOException | ParserConfigurationException | FactoryConfigurationError e1) {
			throw new RuntimeException(e1);
		}

		Element status = DOMUtils.getUniqueElement(document.getDocumentElement(), "cas:authenticationSuccess");
		if (status != null) {
			String userName = DOMUtils.getUniqueElement(document.getDocumentElement(), "cas:user").getTextContent();
			if (!Strings.isNullOrEmpty(userName)) {
				// OK we've got an user
				logger.info("[CAS] Ticket validation successful for user : {}", userName);
				ExternalCreds creds = new ExternalCreds();
				creds.setTicket(ticket);
				if (userName.contains("@")) {
					creds.setLoginAtDomain(userName.toLowerCase());
				} else {
					SharedMap<String, String> sysconf = MQ.sharedMap(Shared.MAP_SYSCONF);
					String casDomain = sysconf.get(SysConfKeys.cas_domain.name());
					creds.setLoginAtDomain(String.format("%s@%s", userName.toLowerCase(), casDomain.toLowerCase()));
				}
				return Optional.of(creds);
			}
		} else {
			logger.warn("Missing status in XML from CAS:\n{}", responseData);
		}

		return Optional.empty();

	}

}
