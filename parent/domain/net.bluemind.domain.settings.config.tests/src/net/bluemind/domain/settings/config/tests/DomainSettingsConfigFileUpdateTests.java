/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.domain.settings.config.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainSettingsConfigFileUpdateTests {

	private static final String externalUrlFilePath = "/etc/bm/domains-settings";

	String domainUid;
	IDomains domainService;
	INodeClient nodeClient;

	@Before
	public void setup() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		Server nodeServer = new Server();
		nodeServer.ip = new BmConfIni().get(DockerContainer.NODE.getName());
		nodeServer.tags = Lists.newArrayList(TagDescriptor.bm_core.getTag(), TagDescriptor.mail_imap.getTag());
		assertNotNull(nodeServer);
		nodeClient = NodeActivator.get(nodeServer.ip);

		domainUid = "testdomain" + System.currentTimeMillis() + ".loc";
		PopulateHelper.initGlobalVirt(false, nodeServer);
		PopulateHelper.createDomain(domainUid);

		domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		assertNotNull(domainService);
	}

	@After
	public void tearDown() throws Exception {

		// delete external url file
		Files.deleteIfExists(Paths.get(externalUrlFilePath));

		if (!nodeClient.listFiles(externalUrlFilePath).isEmpty()) {
			nodeClient.deleteFile(externalUrlFilePath);
		}

		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void onSettingsUpdated_externalUrl_defaultDomain() throws Exception {

		// create domains
		Domain defaultDomain = Domain.create("default.domain", "default.domain", "", new HashSet<String>());
		domainService.create("default.domain", defaultDomain);
		Domain validDomainLan = Domain.create("valid.domain.lan", "valid.domain.lan", "", new HashSet<String>());
		domainService.create("valid.domain.lan", validDomainLan);
		Domain newDomainLan = Domain.create("new.domain.lan", "new.domain.lan", "", new HashSet<String>());
		domainService.create("new.domain.lan", newDomainLan);

		IDomainSettings domainSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);

		// add settings
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DomainSettingsKeys.default_domain.name(), defaultDomain.name);
		settings.put(DomainSettingsKeys.external_url.name(), "external.url");
		settings.put(DomainSettingsKeys.mailbox_default_user_quota.name(), "5");
		settings.put(DomainSettingsKeys.mailbox_max_user_quota.name(), "15");
		domainSettingsService.set(settings);

		waitForMqMsg();

		// before start check external file content
		Set<Entry<String, String>> settingsFromDb = domainSettingsService.get().entrySet();
		assertTrue(settingsFromDb.stream().anyMatch(
				s -> s.getKey().equals(DomainSettingsKeys.external_url.name()) && s.getValue().equals("external.url")));
		assertTrue(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.default_domain.name())
				&& s.getValue().equals(defaultDomain.name)));
		checkFileContent(domainUid, "external.url", defaultDomain.name);

		//
		// modify external url
		//
		settings = domainSettingsService.get();
		settings.replace(DomainSettingsKeys.external_url.name(), "valid.external.url");
		domainSettingsService.set(settings);

		waitForMqMsg();

		// check database after changes
		settingsFromDb = domainSettingsService.get().entrySet();
		assertTrue(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.external_url.name())
				&& s.getValue().equals("valid.external.url")));
		assertTrue(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.default_domain.name())
				&& s.getValue().equals(defaultDomain.name)));
		// check file content after changes
		checkFileContent(domainUid, "valid.external.url", defaultDomain.name);

		//
		// modify default domain url
		//
		settings = domainSettingsService.get();
		settings.putAll(settings);
		settings.replace(DomainSettingsKeys.default_domain.name(), validDomainLan.name);
		domainSettingsService.set(settings);

		waitForMqMsg();

		// check database after changes
		settingsFromDb = domainSettingsService.get().entrySet();
		assertTrue(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.external_url.name())
				&& s.getValue().equals("valid.external.url")));
		assertTrue(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.default_domain.name())
				&& s.getValue().equals(validDomainLan.name)));
		// check file content after changes
		checkFileContent(domainUid, "valid.external.url", validDomainLan.name);

		//
		// remove external url settings
		//
		settings = domainSettingsService.get();
		settings.remove(DomainSettingsKeys.external_url.name());
		domainSettingsService.set(settings);

		waitForMqMsg();

		// check database after changes
		// external url must be null in database
		settingsFromDb = domainSettingsService.get().entrySet();
		assertFalse(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.external_url.name())));
		assertTrue(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.default_domain.name())
				&& s.getValue().equals(validDomainLan.name)));
		// check file content after changes
		checkFileContent(domainUid, "", validDomainLan.name);

		//
		// remove default domain settings
		//
		settings = domainSettingsService.get();
		settings.remove(DomainSettingsKeys.default_domain.name());
		domainSettingsService.set(settings);

		waitForMqMsg();

		// default_domain must be null in database
		settingsFromDb = domainSettingsService.get().entrySet();
		assertFalse(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.external_url.name())));
		assertFalse(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.default_domain.name())));
		// check file content after changes
		checkFileContent(domainUid, null, null);

		//
		// re-add default domain settings
		//
		settings = domainSettingsService.get();
		settings.put(DomainSettingsKeys.default_domain.name(), newDomainLan.name);
		domainSettingsService.set(settings);

		waitForMqMsg();

		// default_domain must not be null in database
		settingsFromDb = domainSettingsService.get().entrySet();
		assertFalse(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.external_url.name())));
		assertTrue(settingsFromDb.stream().anyMatch(s -> s.getKey().equals(DomainSettingsKeys.default_domain.name())
				&& s.getValue().equals(newDomainLan.name)));
		// check file content after changes
		checkFileContent(domainUid, "", newDomainLan.name);

	}

	private void waitForMqMsg() throws InterruptedException, ExecutionException {

		CompletableFuture<JsonObject> storeMsg = new CompletableFuture<>();
		MQ.init(() -> MQ.registerConsumer("end.domain.settings.file.updated", msg -> {
			storeMsg.complete(msg.toJson());
		}));

		String domainSettingsFilepath = storeMsg.get().getString("filepath");
		assertNotNull(domainSettingsFilepath);
	}

	private void checkFileContent(String uid, String externalUrl, String defaultDomain) throws Exception {
		String fileContent = null;

		try (InputStream input = nodeClient.openStream(externalUrlFilePath)) {
			System.err.println("== Check " + externalUrlFilePath + " file content ");
			StringBuilder sb = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
				String read;
				while ((read = br.readLine()) != null) {
					sb.append(read);
				}
				fileContent = sb.toString();
				assertNotNull(fileContent);
			}
		}

		if (!Strings.isNullOrEmpty(externalUrl) || !Strings.isNullOrEmpty(defaultDomain)) {
			String expectedLine = "" + uid + ":" + externalUrl + ":" + defaultDomain;
			assertTrue(fileContent.contains(expectedLine));
			return;
		}

		assertFalse(fileContent.contains(uid));
	}

}
