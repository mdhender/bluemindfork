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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.integration.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.cli.integration.tests.utils.CliTestHelper;
import net.bluemind.cli.integration.tests.utils.CliTestHelper.TestDomainOptions;
import net.bluemind.cli.launcher.CLIManager;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class OneDomainTenUsersCliTests {

	public static final int USER_COUNT = 10;
	private static String domainUid;
	private static CliTestHelper testHelper;
	private static CLIManager cli;

	@BeforeClass
	public static void beforeClass() throws Exception {
		domainUid = "junit" + System.currentTimeMillis() + ".cli";
		testHelper = CliTestHelper.builder()//
				.withDomains(domainUid)//
				.enableCyrusReplication()//
				.withDomainOptions(TestDomainOptions.justUsers(USER_COUNT))//
				.build();
		testHelper.beforeTest();
		cli = new CLIManager(Activator.context.getBundle().getVersion());
	}

	@Test
	public void testMaintenanceListing() {
		cli.processArgs("maintenance", "list", domainUid);
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		List<JsonObject> entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		assertFalse(entryObjects.isEmpty());
		System.out.println("Found entries: " + entryObjects.size());
		assertTrue("We provisionned " + USER_COUNT + " users but got " + entryObjects.size(),
				entryObjects.size() >= USER_COUNT);
	}

	@Test
	public void testIndexShards() {
		cli.processArgs("index", "shards");
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		int jsonArrayStart = output.indexOf("[ {");
		assertTrue(jsonArrayStart >= 0);
		String justJson = output.substring(jsonArrayStart);
		JsonArray reparsedStats = new JsonArray(justJson);
		assertTrue(reparsedStats.size() > 0);
	}

	@Test
	public void testUserListingPagination() {
		cli.processArgs("user", "get", domainUid);
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		List<JsonObject> entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		assertEquals(11, entryObjects.size()); // 10 users + 1 admin

		// all provisionned
		cli.processArgs("user", "get", "--match", "u.*", "--display", "\"uid\"", domainUid);
		output = testHelper.outputAndReset();
		entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		assertEquals(10, entryObjects.size());

		// none
		cli.processArgs("user", "get", "--match", "z.*", "--display", "\"uid\"", domainUid);
		output = testHelper.outputAndReset();
		entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		assertEquals(0, entryObjects.size());

	}

	@Test
	public void testUserListing() {
		cli.processArgs("user", "get", domainUid);
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		List<JsonObject> entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		assertEquals(11, entryObjects.size()); // 10 users + 1 admin

		String archivedUserUid = entryObjects.get(0).getString("uid");
		String hiddenUserUid = entryObjects.get(1).getString("uid");

		// set one user archived
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);
		ItemValue<User> user = userService.getComplete(archivedUserUid);
		user.value.archived = true;
		userService.update(archivedUserUid, user.value);

		cli.processArgs("user", "get", domainUid);
		output = testHelper.outputAndReset();
		entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		assertEquals(10, entryObjects.size()); // 10 users + 1 admin -1 archived

		cli.processArgs("user", "get", "--archived", domainUid);
		output = testHelper.outputAndReset();
		assertNotNull(output);
		entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		assertEquals(1, entryObjects.size()); // 1 archived
		assertEquals(entryObjects.get(0).getString("uid"), archivedUserUid);

		// set one user hidden
		user = userService.getComplete(hiddenUserUid);
		user.value.hidden = true;
		userService.update(hiddenUserUid, user.value);

		cli.processArgs("user", "get", domainUid);
		output = testHelper.outputAndReset();
		assertNotNull(output);
		entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		assertEquals(10, entryObjects.size()); // 10 users + 1 admin -1 archived

		cli.processArgs("user", "get", "--hidden", domainUid);
		output = testHelper.outputAndReset();
		assertNotNull(output);
		entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		assertEquals(1, entryObjects.size()); // 1 hidden
		assertEquals(entryObjects.get(0).getString("uid"), hiddenUserUid);
	}

	@Test
	public void testUserAdmin0PasswordUpdate() {
		cli.processArgs("user", "update", "admin0@global.virt", "--password", "newpassword");
		String output = testHelper.outputAndReset();
		System.err.println(output);
		assertNotNull(output);

		IAuthentication authService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAuthentication.class);
		LoginResponse response = authService.login("admin0@global.virt", "newpassword", "test");
		assertTrue(response.status.equals(LoginResponse.Status.Ok));
	}

	@Test
	public void testDomainQuotaUpdate() {
		cli.processArgs("user", "get", "--display", "quota email", domainUid);
		String output = testHelper.outputAndReset();
		Optional<JsonObject> entryObject = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).findFirst();
		String email = entryObject.get().getString("email");

		cli.processArgs("user", "update", domainUid, "--quota", "2047");
		output = testHelper.outputAndReset();

		cli.processArgs("user", "get", "--display", "quota", email);
		output = testHelper.outputAndReset();

		List<JsonObject> entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		entryObjects.forEach(o -> assertEquals(2047, o.getInteger("quota").intValue()));
	}

	@Test
	public void testUserExport() {
		cli.processArgs("user", "get", "--display", "quota email", domainUid);
		String output = testHelper.outputAndReset();
		Optional<JsonObject> entryObject = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).findFirst();
		String email = entryObject.get().getString("email");

		cli.processArgs("user", "export", email);
		output = testHelper.outputAndReset();
		System.out.println(output);

	}

	@After
	public void after() throws Exception {
		testHelper.afterTest();
	}

	@AfterClass
	public static void afterCLass() throws Exception {
		testHelper.afterClassTest();
	}

}
