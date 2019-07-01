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

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * Bogus trust manager factory. Creates BogusX509TrustManager
 *
 * @version $Rev$, $Date$
 */
class BogusTrustManagerFactory extends TrustManagerFactorySpi {

	static final X509TrustManager X509 = new X509TrustManager() {
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	};

	static final TrustManager[] X509_MANAGERS = new TrustManager[] { X509 };

	public BogusTrustManagerFactory() {
	}

	protected TrustManager[] engineGetTrustManagers() {
		return X509_MANAGERS;
	}

	protected void engineInit(KeyStore keystore) throws KeyStoreException {
		// noop
	}

	protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
			throws InvalidAlgorithmParameterException {
		// noop
	}
}
