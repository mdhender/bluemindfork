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
package net.bluemind.notes.service;

import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;

import org.junit.Test;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteIndexMgmt;
import net.bluemind.notes.api.INotes;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.hook.NoteHookAddress;

public class NoteIndexMgmtServiceTests extends AbstractServiceTests {

	@Test
	public void testReindexAll() throws ServerFault, SQLException {
		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(NoteHookAddress.CREATED);

		VNote note = defaultVNote();
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		VNote note2 = defaultVNote();
		String uid2 = "test2_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid2, note2);

		Item item = itemStore.get(uid);
		assertNotNull(item);
		VNote vnote = vnoteStore.get(item);
		assertNotNull(vnote);

		getServiceNoteMgmt(defaultSecurityContext).reindexAll();

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Test
	public void testReindex() throws ServerFault, SQLException {
		VertxEventChecker<JsonObject> createdMessageChecker = new VertxEventChecker<>(NoteHookAddress.CREATED);

		VNote note = defaultVNote();
		String uid = "test_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid, note);

		VNote note2 = defaultVNote();
		String uid2 = "test2_" + System.nanoTime();
		getServiceNote(defaultSecurityContext, container.uid).create(uid2, note2);

		Item item = itemStore.get(uid);
		assertNotNull(item);
		VNote vnote = vnoteStore.get(item);
		assertNotNull(vnote);

		getServiceNoteMgmt(defaultSecurityContext).reindex(uid);

		Message<JsonObject> message = createdMessageChecker.shouldSuccess();
		assertNotNull(message);
	}

	@Override
	protected INoteIndexMgmt getServiceNoteMgmt(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(INoteIndexMgmt.class, container.uid);
	}

	@Override
	protected INote getServiceNote(SecurityContext context, String containerUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(INote.class, containerUid);
	}

	@Override
	protected INotes getServiceNotes(SecurityContext context) throws ServerFault {
		return null;
	}
}
