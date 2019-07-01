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
package net.bluemind.tag.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.AclStore;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.persistance.TagStore;
import net.bluemind.tag.service.internal.Tags;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class TagsTests {

	protected SecurityContext defaultSecurityContext;
	protected SecurityContext notAuthorizedSecurityContext;
	protected SecurityContext readSecurityContext;

	private Container tagContainer;

	private TagStore tagsStore;

	private ItemStore tagContainerItemStore;
	private String domainUid = "global.virt";
	private String datalocation;
	private DataSource dataDataSource;

	private BmContext context;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();

		datalocation = PopulateHelper.FAKE_CYRUS_IP;
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(datalocation);

		defaultSecurityContext = new SecurityContext("test", "test", Arrays.<String>asList(), Arrays.<String>asList(),
				domainUid);

		notAuthorizedSecurityContext = new SecurityContext("testNotAuthorized", "testNotAuthorized",
				Arrays.<String>asList(), Arrays.<String>asList(), domainUid);

		readSecurityContext = new SecurityContext("testRead", "testRead", Arrays.<String>asList(),
				Arrays.<String>asList(), domainUid);

		Sessions.get().put(defaultSecurityContext.getSessionId(), defaultSecurityContext);
		Sessions.get().put(notAuthorizedSecurityContext.getSessionId(), notAuthorizedSecurityContext);
		Sessions.get().put(readSecurityContext.getSessionId(), readSecurityContext);

		context = new BmTestContext(defaultSecurityContext);

		ContainerStore containerHome = new ContainerStore(context, dataDataSource, defaultSecurityContext);

		String containerId = "test_" + System.nanoTime();
		tagContainer = Container.create(containerId, ITagUids.TYPE, "test", "me", domainUid, true);
		tagContainer = containerHome.create(tagContainer);

		containerHome = new ContainerStore(context, JdbcActivator.getInstance().getDataSource(),
				defaultSecurityContext);
		containerHome.createContainerLocation(tagContainer, datalocation);

		tagContainerItemStore = new ItemStore(dataDataSource, tagContainer, defaultSecurityContext);

		AclStore aclStore = new AclStore(context, dataDataSource);
		aclStore.store(tagContainer,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.Write),
						AccessControlEntry.create(readSecurityContext.getSubject(), Verb.Read)));
		tagsStore = new TagStore(dataDataSource, tagContainer);

		// start vertx eventbus..
		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

	}

	protected ITags getService(SecurityContext sc) {
		BmContext context = new BmTestContext(sc);
		DataSource ds = DataSourceRouter.get(context, tagContainer.uid);
		return new Tags(context, ds, tagContainer);
	}

	@Test
	public void testCreate() throws SQLException, ServerFault {
		ITags tags = getService(defaultSecurityContext);
		Tag tag = new Tag();
		tag.label = "test";
		tag.color = "ff00ff";
		try {
			tags.create("test", tag);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Item item = tagContainerItemStore.get("test");
		assertNotNull(item);
		Tag found = tagsStore.get(item);
		assertNotNull(found);
		assertEquals(tag.label, found.label);
		assertEquals(tag.color, found.color);
		try {
			getService(notAuthorizedSecurityContext).create("test2", tag);
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(readSecurityContext).create("test2", tag);
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void testUpdate() throws SQLException, ServerFault {
		FakeTagEventConsumer.future = SettableFuture.<String>create();
		Tag tag = new Tag();
		tag.label = "test";
		tag.color = "ff00ff";
		tagContainerItemStore.create(Item.create("test", null));
		Item item = tagContainerItemStore.get("test");
		tagsStore.create(item, tag);

		ITags tags = getService(defaultSecurityContext);
		tag.label = "testUpdated";
		tag.color = "0000ff";

		try {
			tags.update("test", tag);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// check event producer/consumer :
		try {
			String uid = FakeTagEventConsumer.future.get(1, TimeUnit.SECONDS);
			assertEquals("test", uid);
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			fail(e1.getMessage());
		}

		item = tagContainerItemStore.get("test");
		assertNotNull(item);
		Tag found = tagsStore.get(item);
		assertNotNull(found);
		assertEquals(tag.label, found.label);
		assertEquals(tag.color, found.color);

		try {
			getService(notAuthorizedSecurityContext).update("test", tag);
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(readSecurityContext).update("test", tag);
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void testDelete() throws SQLException, ServerFault {
		FakeTagEventConsumer.future = SettableFuture.<String>create();

		Tag tag = new Tag();
		tag.label = "test";
		tag.color = "ff00ff";
		tagContainerItemStore.create(Item.create("test", null));
		Item item = tagContainerItemStore.get("test");
		tagsStore.create(item, tag);

		try {
			getService(notAuthorizedSecurityContext).delete("test");
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		assertNotNull(tagsStore.get(item));
		try {
			getService(readSecurityContext).delete("test");
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		assertNotNull(tagsStore.get(item));

		ITags tags = getService(defaultSecurityContext);

		try {
			tags.delete("test");
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertNull(tagsStore.get(item));
		assertNull(tagContainerItemStore.get("test"));
		// check event producer/consumer :
		try {
			String uid = FakeTagEventConsumer.future.get(1, TimeUnit.SECONDS);
			assertEquals("test", uid);
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			fail(e1.getMessage());
		}
	}

	@Test
	public void testAll() throws SQLException, ServerFault {
		Tag tag = new Tag();
		tag.label = "test";
		tag.color = "ff00ff";
		tagContainerItemStore.create(Item.create("test", null));
		Item item = tagContainerItemStore.get("test");
		tagsStore.create(item, tag);
		ITags tags = getService(defaultSecurityContext);

		List<ItemValue<Tag>> itemsValues = tags.all();
		assertNotNull(itemsValues);
		assertEquals(1, itemsValues.size());

		itemsValues = getService(readSecurityContext).all();
		assertNotNull(itemsValues);
		assertEquals(1, itemsValues.size());

		try {
			getService(notAuthorizedSecurityContext).all();
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void noColor() {
		Tag tag = new Tag();
		tag.label = "test";
		ITags tags = getService(defaultSecurityContext);

		try {
			tags.create(UUID.randomUUID().toString(), tag);
			fail("should fail");
		} catch (ServerFault e) {
		}
	}
}
