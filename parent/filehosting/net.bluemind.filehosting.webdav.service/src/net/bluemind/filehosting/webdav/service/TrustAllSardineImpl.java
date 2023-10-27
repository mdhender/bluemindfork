/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.filehosting.webdav.service;

import java.io.IOException;
import java.net.ProxySelector;
import java.time.Duration;

import org.apache.http.Consts;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.KerberosSchemeFactory;
import org.apache.http.impl.auth.NTLMSchemeFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sardine.Version;
import com.github.sardine.impl.SardineImpl;

import net.bluemind.utils.Trust;

public class TrustAllSardineImpl extends SardineImpl {

	private static final int CONNECTION_TIMEOUT = Duration.ofMinutes(2).toMillisPart();
	private static final int SOCKET_TIMEOUT = Duration.ofMinutes(4).toMillisPart();

	private static final Logger logger = LoggerFactory.getLogger(TrustAllSardineImpl.class);

	public TrustAllSardineImpl(String username, String password) {
		super(username, password);
	}

	@Override
	protected HttpClientBuilder configure(ProxySelector selector, CredentialsProvider credentials) {
		Registry<ConnectionSocketFactory> schemeRegistry = this.createDefaultSchemeRegistry();
		HttpClientConnectionManager cm = this.createDefaultConnectionManager(schemeRegistry);
		String version = Version.getSpecification();
		if (version == null) {
			version = VersionInfo.UNAVAILABLE;
		}

		Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
				.register(AuthSchemes.BASIC, new BasicSchemeFactory(Consts.UTF_8))
				.register(AuthSchemes.DIGEST, new DigestSchemeFactory(Consts.UTF_8))
				.register(AuthSchemes.NTLM, new NTLMSchemeFactory())
				.register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory())
				.register(AuthSchemes.KERBEROS, new KerberosSchemeFactory()).build();

		return HttpClients.custom().setDefaultAuthSchemeRegistry(authSchemeRegistry).setUserAgent("Sardine/" + version)
				.setDefaultCredentialsProvider(credentials).setRedirectStrategy(this.createDefaultRedirectStrategy())
				.setDefaultRequestConfig(RequestConfig.custom()
						// Only selectively enable this for PUT but not all entity enclosing methods
						.setExpectContinueEnabled(false) //
						.setConnectTimeout(CONNECTION_TIMEOUT) //
						.setConnectionRequestTimeout(CONNECTION_TIMEOUT) //
						.setSocketTimeout(SOCKET_TIMEOUT) //
						.build())
				.setConnectionManager(cm)
				.setRoutePlanner(this.createDefaultRoutePlanner(this.createDefaultSchemePortResolver(), selector));

	}

	@Override
	protected <T> T execute(HttpClientContext context, HttpRequestBase request, ResponseHandler<T> responseHandler)
			throws IOException {
		HttpContext requestLocalContext = new BasicHttpContext(context);
		try {
			if (responseHandler != null) {
				return this.client.execute(request, responseHandler, requestLocalContext);
			} else {
				request.getParams().setParameter(AuthPNames.CREDENTIAL_CHARSET, "UTF-8");
				return (T) this.client.execute(request, requestLocalContext);
			}
		} catch (HttpResponseException e) {
			// Don't abort if we get this exception, caller may want to repeat request.
			throw e;
		} catch (IOException e) {
			request.abort();
			throw e;
		} finally {
			context.setAttribute(HttpClientContext.USER_TOKEN,
					requestLocalContext.getAttribute(HttpClientContext.USER_TOKEN));
		}
	}

	@Override
	protected ConnectionSocketFactory createDefaultSecureSocketFactory() {
		Trust trust = new Trust();
		return new SSLConnectionSocketFactory(trust.getSSLSocketFactory("webdav"), trust.getHostNameVerifier("webdav"));
	}

}
