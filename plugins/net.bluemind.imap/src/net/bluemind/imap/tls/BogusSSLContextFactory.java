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
package net.bluemind.imap.tls;

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory to create a bogus SSLContext.
 * 
 */
public class BogusSSLContextFactory {

	private static final Logger logger = LoggerFactory.getLogger(BogusSSLContextFactory.class);

	private static SSLContext clientInstance;

	/**
	 * Get SSLContext singleton.
	 * 
	 * @return SSLContext
	 * @throws java.security.GeneralSecurityException
	 * 
	 */
	public static synchronized SSLContext getInstance() throws GeneralSecurityException {
		if (clientInstance == null) {
			clientInstance = createBogusClientSSLContext();
			logger.debug("dump ssl context created {}", clientInstance);
		}
		return clientInstance;
	}

	private static SSLContext createBogusClientSSLContext() throws GeneralSecurityException {
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, BogusTrustManagerFactory.X509_MANAGERS, null);
		return context;
	}

}
