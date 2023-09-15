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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

package net.bluemind.cli.auditlog.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.cli.auditlog.tests.utils.CliTestHelper;
import net.bluemind.cli.launcher.CLIManager;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.logs.tests.AbstractAuditLogServiceTests;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.system.state.StateContext;

public class AuditLogFetchTests extends AbstractAuditLogServiceTests {
	private static final Logger logger = LoggerFactory.getLogger(AuditLogFetchTests.class);
	protected static final String AUDIT_LOG_INDEX = "audit_log";
	private static CliTestHelper testHelper;
	private static CLIManager cli;
	private ObjectMapper mapper = new ObjectMapper();

	@BeforeClass
	public static void beforeClass() throws Exception {
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
		testHelper = new CliTestHelper();
		cli = new CLIManager(Activator.context.getBundle().getVersion());
	}

	@Before
	public void before() throws Exception {
		super.before();
		testHelper.beforeTest();
	}

	@After
	public void after() throws Exception {
		super.after();
		testHelper.afterTest();

	}

	@AfterClass
	public static void afterClass() throws Exception {
		testHelper.afterClassTest();
	}

	@Test
	public void testGetCalendarJSonFormatAuditLogs() {
		ESearchActivator.refreshIndex(AUDIT_LOG_INDEX);
		cli.processArgs("auditlog", "fetch", "--logtype", "calendar");
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		List<JsonObject> entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		List<AuditLogEntry> auditLogEntries = entryObjects.stream().map(e -> {
			try {
				return mapper.readValue(e.toString(), AuditLogEntry.class);
			} catch (JsonProcessingException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
				return null;
			}
		}).toList();
		assertEquals(4, auditLogEntries.size());
		assertEquals(event01.main.summary, auditLogEntries.get(0).content.description());
		assertEquals(event02.main.summary, auditLogEntries.get(1).content.description());
		assertEquals(event03.main.summary, auditLogEntries.get(2).content.description());
		assertEquals(event04.main.summary, auditLogEntries.get(3).content.description());
	}

	@Test
	public void testGetCalendarTableFormatAuditLogs() {
		ESearchActivator.refreshIndex(AUDIT_LOG_INDEX);
		cli.processArgs("auditlog", "fetch", "--logtype", "calendar", "--output", "table");
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		assertTrue(!output.isBlank());
	}

	@Test
	public void testGetCalendarMailboxRecordsAuditLogs() {
		ESearchActivator.refreshIndex(AUDIT_LOG_INDEX);
		cli.processArgs("auditlog", "fetch", "--logtype", "calendar,mailbox_records");
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		List<JsonObject> entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		List<AuditLogEntry> auditLogEntries = entryObjects.stream().map(e -> {
			try {
				return mapper.readValue(e.toString(), AuditLogEntry.class);
			} catch (JsonProcessingException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
				return null;
			}
		}).toList();
		assertEquals(7, auditLogEntries.size());
		assertEquals(event01.main.summary, auditLogEntries.get(0).content.description());
		assertEquals(event02.main.summary, auditLogEntries.get(1).content.description());
		assertEquals(event03.main.summary, auditLogEntries.get(2).content.description());
		assertEquals(event04.main.summary, auditLogEntries.get(3).content.description());
		assertEquals("first subject", auditLogEntries.get(4).content.description());
		assertEquals("second subject", auditLogEntries.get(5).content.description());
		assertEquals("third subject", auditLogEntries.get(6).content.description());
	}

	@Test
	public void testGetCalendarWithAuditLogs() {
		ESearchActivator.refreshIndex(AUDIT_LOG_INDEX);
		cli.processArgs("auditlog", "fetch", "--logtype", "calendar", "--with", user02.value.defaultEmailAddress());
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		List<JsonObject> entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		List<AuditLogEntry> auditLogEntries = entryObjects.stream().map(e -> {
			try {
				return mapper.readValue(e.toString(), AuditLogEntry.class);
			} catch (JsonProcessingException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
				return null;
			}
		}).toList();
		assertEquals(2, auditLogEntries.size());
		assertEquals(event01.main.summary, auditLogEntries.get(0).content.description());
		assertEquals(event02.main.summary, auditLogEntries.get(1).content.description());
	}

	@Test
	public void testGetCalendarDescriptionAuditLogs() {
		ESearchActivator.refreshIndex(AUDIT_LOG_INDEX);
		cli.processArgs("auditlog", "fetch", "--logtype", "calendar", "--description", "First Meeting");
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		List<JsonObject> entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		List<AuditLogEntry> auditLogEntries = entryObjects.stream().map(e -> {
			try {
				return mapper.readValue(e.toString(), AuditLogEntry.class);
			} catch (JsonProcessingException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
				return null;
			}
		}).toList();
		assertEquals(3, auditLogEntries.size());
		assertEquals(event01.main.summary, auditLogEntries.get(0).content.description());
		assertEquals(event02.main.summary, auditLogEntries.get(1).content.description());
		assertEquals(event03.main.summary, auditLogEntries.get(2).content.description());
	}

	@Test
	public void testAuditCliJSONLogin() throws ElasticsearchException, IOException {
		IAuthentication authentication = getAuthenticationService(null);

		LoginResponse response = authentication.login(user01.value.defaultEmailAddress(), user01.value.login, "junit");

		assertEquals(Status.Ok, response.status);
		assertNotNull(response.authKey);

		String authKey = response.authKey;

		response = authentication.login(user01.value.defaultEmailAddress(), authKey, "auth-key");
		assertEquals(Status.Ok, response.status);
		assertEquals(authKey, response.authKey);

		response = authentication.login(user02.value.defaultEmailAddress(), user02.value.login, "junit");
		assertEquals(Status.Ok, response.status);

		ESearchActivator.refreshIndex(AUDIT_LOG_INDEX);
		cli.processArgs("auditlog", "fetch", "--logtype", "login");
		String output = testHelper.outputAndReset();
		assertNotNull(output);
		List<JsonObject> entryObjects = Pattern.compile("\n").splitAsStream(output).filter(l -> l.startsWith("{"))
				.map(l -> new JsonObject(l)).collect(Collectors.toList());
		List<AuditLogEntry> auditLogEntries = entryObjects.stream().map(e -> {
			try {
				return mapper.readValue(e.toString(), AuditLogEntry.class);
			} catch (JsonProcessingException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
				return null;
			}
		}).toList();
		assertEquals(3, auditLogEntries.size());
		assertEquals(user01.value.defaultEmailAddress(), auditLogEntries.get(0).securityContext.email());
		assertEquals(user01.value.defaultEmailAddress(), auditLogEntries.get(1).securityContext.email());
		assertEquals(user02.value.defaultEmailAddress(), auditLogEntries.get(2).securityContext.email());
		assertEquals("junit", auditLogEntries.get(0).securityContext.origin());
		assertEquals("junit", auditLogEntries.get(1).securityContext.origin());
		assertEquals("junit", auditLogEntries.get(2).securityContext.origin());
	}

	@Test
	public void testAuditCliTableLogin() throws ElasticsearchException, IOException {
		IAuthentication authentication = getAuthenticationService(null);

		LoginResponse response = authentication.login(user01.value.defaultEmailAddress(), user01.value.login, "junit");

		assertEquals(Status.Ok, response.status);
		assertNotNull(response.authKey);

		String authKey = response.authKey;

		response = authentication.login(user01.value.defaultEmailAddress(), authKey, "auth-key");
		assertEquals(Status.Ok, response.status);
		assertEquals(authKey, response.authKey);

		response = authentication.login(user02.value.defaultEmailAddress(), user02.value.login, "junit");
		assertEquals(Status.Ok, response.status);

		ESearchActivator.refreshIndex(AUDIT_LOG_INDEX);
		cli.processArgs("auditlog", "fetch", "--logtype", "login", "--output", "table");
		String output = testHelper.outputAndReset();
		System.err.println(output);
		assertNotNull(output);
	}

	private IAuthentication getAuthenticationService(String sessionId) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", sessionId)
				.instance(IAuthentication.class);
	}

}
