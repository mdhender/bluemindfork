/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.eas.integration.tests;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevice;
import net.bluemind.eas.http.internal.CoreStateListener;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.SystemState;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class EasServerSetup {

	private String domainUid;
	private String userUid;
	private Device device;

	public EasServerSetup(String userUid, String domainUid) {
		this.userUid = userUid;
		this.domainUid = domainUid;

	}

	public Device device() {
		return device;
	}

	public String loginAtDomain() {
		return userUid + "@" + domainUid;
	}

	public String password() {
		return userUid;
	}

	public void beforeTest() throws Exception {
		VertxPlatform.eventBus().publish(SystemState.BROADCAST,
				new JsonObject().put("operation", SystemState.CORE_STATE_RUNNING.operation()));

		System.err.println("Start populate user " + userUid);
		PopulateHelper.addUser(userUid, domainUid, Routing.internal, BasicRoles.ROLE_EAS);

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDevice devApi = prov.instance(IDevice.class, userUid);
		Device dev = new Device();
		dev.hasPartnership = true;
		dev.identifier = "junit-" + userUid;
		dev.type = "junit-phone";
		dev.isWipe = false;
		dev.owner = userUid;
		devApi.create("junit-" + userUid, dev);

		this.device = devApi.byIdentifier("junit-" + userUid).value;

		System.err.println("Test setup is complete, dev: " + device);

		Awaitility.await().atMost(10, TimeUnit.SECONDS)
				.until(() -> CoreStateListener.state == SystemState.CORE_STATE_RUNNING);

	}

	public void afterTest() throws Exception {
	}

}
