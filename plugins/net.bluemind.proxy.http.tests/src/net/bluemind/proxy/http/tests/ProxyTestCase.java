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
package net.bluemind.proxy.http.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import junit.framework.TestCase;
import net.bluemind.proxy.http.HttpProxyServer;

public abstract class ProxyTestCase extends TestCase {

	protected HttpProxyServer hps;
	protected String testLogin;
	protected String testPass;

	protected abstract void protectedSetUp() throws Exception;

	@Override
	final protected void setUp() throws Exception {
		super.setUp();

		Properties props = loadTestProps("tests.properties");
		if (props.isEmpty()) {
			System.out.println("tests.properties not found, using tests.properties.sample");
			props = loadTestProps("tests.properties.sample");
		}
		this.testLogin = props.get("login").toString();
		this.testPass = props.get("pass").toString();
		System.out.println("Test " + getName() + " credentials: " + testLogin + " / " + testPass);

		try {
			hps = new HttpProxyServer();
			hps.setPort(18079);
			hps.run();
		} catch (Throwable t) {
			System.err.println("Fail to start proxy: " + t.getMessage());
			t.printStackTrace();
			if (hps != null) {
				hps.stop();
			}
		}

		try {
			protectedSetUp();
		} catch (Exception e) {
			if (hps != null) {
				hps.stop();
				hps = null;
			}
			throw e;
		}
	}

	private Properties loadTestProps(String propFileName) throws IOException {
		Properties props = new Properties();
		InputStream in = ProxyTestCase.class.getClassLoader().getResourceAsStream("data/" + propFileName);
		if (in != null) {
			props.load(in);
			in.close();
		}
		return props;
	}

	@Override
	protected void tearDown() throws Exception {
		if (hps != null) {
			hps.stop();
			hps = null;
		}
		super.tearDown();
	}

}
