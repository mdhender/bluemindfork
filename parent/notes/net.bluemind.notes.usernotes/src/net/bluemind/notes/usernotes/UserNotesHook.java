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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.notes.usernotes;

import java.util.Arrays;

import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;
import net.bluemind.user.hook.DefaultUserHook;

public class UserNotesHook extends DefaultUserHook {

	private final IServiceProvider sp() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	@Override
	public void onUserCreated(BmContext context, String domainUid, ItemValue<User> created) throws ServerFault {
		if (!created.value.system) {
			ItemValue<User> user = created;
			String uid = INoteUids.defaultUserNotes(user.uid);
			ContainerDescriptor noteContainer = ContainerDescriptor.create(uid, "$$mynotes$$", user.uid, INoteUids.TYPE,
					domainUid, true);

			try {
				IContainers containers = sp().instance(IContainers.class);

				containers.create(uid, noteContainer);

				IUserSubscription userSubService = sp().instance(IUserSubscription.class, domainUid);
				userSubService.subscribe(user.uid, Arrays.asList(ContainerSubscription.create(uid, true)));

			} catch (ServerFault e) {
				LoggerFactory.getLogger(UserNotesHook.class).error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, User previous) throws ServerFault {
		if (!previous.system) {
			ItemValue<User> user = ItemValue.create(uid, previous);
			String defaultNote = INoteUids.defaultUserNotes(user.uid);

			try {
				deleteNoteList(user, defaultNote);
				IContainers cs = sp().instance(IContainers.class);
				ContainerQuery query = new ContainerQuery();
				query.type = INoteUids.TYPE;
				query.owner = user.uid;
				cs.all(query).forEach(note -> deleteNoteList(user, note.uid));
			} catch (ServerFault e) {
				LoggerFactory.getLogger(UserNotesHook.class).error("error during Notes deletion ", e);
			}
		}
	}

	private void deleteNoteList(ItemValue<User> user, String container) {
		LoggerFactory.getLogger(UserNotesHook.class).info("Delete Notes {} for user {}", container, user.value.login);
		INote noteService = sp().instance(INote.class, container);
		noteService.reset();
		sp().instance(IContainers.class).delete(container);
	}
}
