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
package net.bluemind.eas.testhelper.device;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.junit.Assert;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevice;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class TestDeviceHelper {

	private static final Properties props = new Properties();

	public static class TestDevice {
		public String devId;
		public String devType;
		public ItemValue<Device> device;
		public String loginAtDomain;
		public String password;
		public String vmHostname;
		public String coreUrl;
		public SecurityContext token;
		public ItemValue<User> owner;
		private IDevice deviceService;
		private IUser userService;
		public String domainUid;
	}

	static {
		try (InputStream in = new FileInputStream("/etc/bm/bm.ini")) {
			if (in != null) {
				props.load(in);
				System.out.println("Loaded bm.ini");
			}
		} catch (IOException e) {
		}

	}

	public static TestDevice beforeTest(String origin) throws ServerFault {
		TestDevice td = new TestDevice();
		td.vmHostname = props.getProperty("host");
		td.coreUrl = "http://" + td.vmHostname + ":8090";
		System.out.println("coreUrl: " + td.coreUrl);
		IAuthentication authService = ClientSideServiceProvider.getProvider(td.coreUrl, null)
				.instance(IAuthentication.class);

		LoginResponse admin0 = authService.login("admin0@global.virt", Token.admin0(), origin);
		Assert.assertTrue(admin0.status == LoginResponse.Status.Ok);
		System.out.println("logged in");

		IDomains ds = ClientSideServiceProvider.getProvider(td.coreUrl, admin0.authKey).instance(IDomains.class);
		List<ItemValue<Domain>> domains = ds.all();
		Domain dom = null;
		for (ItemValue<Domain> d : domains) {
			if (!"global.virt".equals(d.value.name)) {
				dom = d.value;
				break;
			}
		}
		td.domainUid = dom.name;
		System.out.println("td.domain " + td.domainUid);

		td.userService = ClientSideServiceProvider.getProvider(td.coreUrl, admin0.authKey).instance(IUser.class,
				dom.name);

		User u = new User();
		String login = "eas" + System.currentTimeMillis();
		u.login = login;
		u.password = "eas";
		VCard card = new VCard();
		card.identification.name = Name.create("Bang", "John", null, null, null, null);
		u.contactInfos = card;
		u.routing = Routing.internal;

		Email em = new Email();
		em.address = login + "@" + dom.name;
		em.isDefault = true;
		em.allAliases = false;
		u.emails = Arrays.asList(em);

		String userUid = UUID.randomUUID().toString();
		td.userService.create(userUid, u);

		td.owner = td.userService.getComplete(userUid);

		td.devId = "APPL" + System.nanoTime();
		td.devType = "iPhone";
		td.loginAtDomain = login + "@" + dom.name;
		td.password = "eas";

		IServiceProvider prov = ClientSideServiceProvider.getProvider(td.coreUrl, admin0.authKey);
		IAuthentication authApi = prov.instance(IAuthentication.class);
		LoginResponse loginResp = authApi.su(em.address);
		Assert.assertEquals(loginResp.status, LoginResponse.Status.Ok);
		td.password = loginResp.authKey;
		System.out.println("sid is now " + td.password);
		td.deviceService = prov.instance(IDevice.class, userUid);

		Device device = new Device();
		device.identifier = td.devId;
		device.type = td.devType;
		device.owner = userUid;

		String deviceUid = UUID.randomUUID().toString();
		td.deviceService.create(deviceUid, device);
		td.deviceService.setPartnership(deviceUid);

		td.device = td.deviceService.getComplete(deviceUid);

		return td;
	}

	public static void afterTest(TestDevice testDevice) throws ServerFault {
		testDevice.deviceService.delete(testDevice.device.uid);
		testDevice.userService.delete(testDevice.owner.uid);
	}

}
