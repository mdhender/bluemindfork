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
package net.bluemind.eas.protocol.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.eas.command.settings.SettingsProtocol;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.EasHeaders;
import net.bluemind.eas.testhelper.device.TestDeviceHelper;
import net.bluemind.eas.testhelper.device.TestDeviceHelper.TestDevice;
import net.bluemind.eas.testhelper.mock.RequestsFactory;
import net.bluemind.eas.testhelper.vertx.Deploy;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.lib.vertx.VertxPlatform;

public abstract class AbstractProtocolTests extends TestCase {
	protected RequestsFactory reqFactory;
	private Set<String> deploymentIDs;
	protected TestDevice testDevice;

	public void setUp() throws Exception {
		this.testDevice = TestDeviceHelper.beforeTest("junit-" + getName());
		this.reqFactory = new RequestsFactory(testDevice.loginAtDomain, testDevice.password,
				"http://" + testDevice.vmHostname);
		final CountDownLatch mqLatch = new CountDownLatch(1);
		MQ.init(new IMQConnectHandler() {

			@Override
			public void connected() {
				mqLatch.countDown();
			}
		});
		mqLatch.await(5, TimeUnit.SECONDS);

	}

	public void tearDown() throws ServerFault {
		Deploy.afterTest(deploymentIDs);
		TestDeviceHelper.afterTest(testDevice);
	}

	protected AuthorizedDeviceQuery query(String cmd) {
		ImmutableMap<String, String> headers = ImmutableMap.of(EasHeaders.Client.PROTOCOL_VERSION, "14.1");
		ImmutableMap<String, String> queryParams = ImmutableMap.of("Cmd", cmd);
		AuthorizedDeviceQuery query = reqFactory.authorized(VertxPlatform.getVertx(), testDevice.devId,
				testDevice.devType, headers, queryParams, testDevice.device.uid);
		return query;
	}

	protected Document load(String reqPath)
			throws SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
		InputStream in = SettingsProtocol.class.getClassLoader().getResourceAsStream("requests/" + reqPath);
		return DOMUtils.parse(in);
	}
}
