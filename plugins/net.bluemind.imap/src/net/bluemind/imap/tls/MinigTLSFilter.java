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

import javax.net.ssl.SSLContext;

import org.apache.mina.filter.ssl.SslFilter;

public class MinigTLSFilter extends SslFilter {

	private static SSLContext CTX;

	static {
		try {
			CTX = BogusSSLContextFactory.getInstance();
		} catch (Exception t) {
			t.printStackTrace();
		}
	}

	public MinigTLSFilter() {
		super(CTX);
		setEnabledProtocols(new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" });
		// setEnabledCipherSuites(new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA"
		// });
	}
}
