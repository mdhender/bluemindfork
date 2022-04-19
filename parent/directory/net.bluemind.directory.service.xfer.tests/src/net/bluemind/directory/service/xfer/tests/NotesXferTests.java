/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.directory.service.xfer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNoteChanges;
import net.bluemind.notes.api.VNoteChanges.ItemDelete;

public class NotesXferTests extends AbstractMultibackendTests {
	@Test
	public void testXferNotes() {
		String container = INoteUids.defaultUserNotes(userUid);

		INote service = ServerSideServiceProvider.getProvider(context).instance(INote.class, container);

		VNote new1 = defaultVNote();
		VNote new2 = defaultVNote();
		String new1UID = "test1_" + System.nanoTime();
		String new2UID = "test2_" + System.nanoTime();

		VNote update = defaultVNote();
		String updateUID = "test_" + System.nanoTime();
		service.create(updateUID, update);
		update.subject = "update" + System.currentTimeMillis();

		VNote delete = defaultVNote();
		String deleteUID = "test_" + System.nanoTime();
		service.create(deleteUID, delete);

		VNoteChanges.ItemAdd add1 = VNoteChanges.ItemAdd.create(new1UID, new1, false);
		VNoteChanges.ItemAdd add2 = VNoteChanges.ItemAdd.create(new2UID, new2, false);

		VNoteChanges.ItemModify modify = VNoteChanges.ItemModify.create(updateUID, update, false);

		ItemDelete itemDelete = VNoteChanges.ItemDelete.create(deleteUID, false);

		VNoteChanges changes = VNoteChanges.create(Arrays.asList(add1, add2), Arrays.asList(modify),
				Arrays.asList(itemDelete));

		service.updates(changes);

		// initial container state
		int nbItems = service.all().size();
		assertEquals(3, nbItems);
		long version = service.getVersion();
		assertEquals(6, version);

		TaskRef tr = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.xfer(userUid, shardIp);
		waitTaskEnd(tr);

		// current service should return nothing
		assertTrue(service.all().isEmpty());

		// new INote instance
		service = ServerSideServiceProvider.getProvider(context).instance(INote.class, container);

		assertEquals(nbItems, service.all().size());
		assertEquals(3L, service.getVersion());

		service.create("new-one", defaultVNote());

		ContainerChangeset<String> changeset = service.changeset(3L);
		assertEquals(1, changeset.created.size());
		assertEquals("new-one", changeset.created.get(0));
		assertTrue(changeset.updated.isEmpty());
		assertTrue(changeset.deleted.isEmpty());
	}

	protected VNote defaultVNote() {
		VNote note = new VNote();
		note.subject = "Note " + System.currentTimeMillis();
		note.body = "Content";
		note.height = 25;
		note.width = 42;
		note.posX = 25;
		note.posY = 42;
		return note;
	}
}
