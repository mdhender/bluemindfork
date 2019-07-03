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
package net.bluemind.ui.adminconsole.system.domains.edit.instantmessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONValue;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.gwt.js.JsDirEntry;
import net.bluemind.directory.api.gwt.serder.DirEntryGwtSerDer;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.User;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;

// FIXME to delete 
// https://forge.bluemind.net/jira/browse/BM-8278
public class ImModelHandler implements IGwtModelHandler {

	public static void registerType() {
		GwtModelHandler.register("bm.ac.ImModelHandler", new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new ImModelHandler();
			}
		});
		GWT.log("bm.ac.ImModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final String domainUid = map.getString(DomainKeys.domainUid.name());
		final GroupGwtEndpoint groupService = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

		groupService.getGroupsWithRoles(Arrays.asList(ImRole.role.getRole()),
				new DefaultAsyncHandler<Set<String>>(handler) {

					@Override
					public void success(Set<String> value) {
						List<DirEntry> entries = new ArrayList<>();
						List<String> asList = new ArrayList<>(value);
						findImGroups(handler, domainUid, groupService, 0, entries, asList, map);
					}

				});
	}

	private void findImGroups(final AsyncHandler<Void> handler, final String domainUid,
			final GroupGwtEndpoint groupService, final int index, final List<DirEntry> entries,
			final List<String> groupIds, final JsMapStringJsObject map) {
		if (index == groupIds.size()) {
			findImUsers(handler, domainUid, entries, map);
			return;
		}
		final String groupId = groupIds.get(index);
		groupService.getComplete(groupId, new DefaultAsyncHandler<ItemValue<Group>>(handler) {

			@Override
			public void success(ItemValue<Group> group) {
				DirEntry dirEntry = new DirEntry();
				dirEntry.kind = DirEntry.Kind.GROUP;
				dirEntry.entryUid = groupId;
				dirEntry.displayName = group.value.name;
				entries.add(dirEntry);
				findImGroups(handler, domainUid, groupService, index + 1, entries, groupIds, map);
			}
		});
	}

	private void findImUsers(final AsyncHandler<Void> handler, final String domainUid, final List<DirEntry> entries,
			final JsMapStringJsObject map) {
		final UserGwtEndpoint userService = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		userService.getUsersWithRoles(Arrays.asList(ImRole.role.getRole()),
				new DefaultAsyncHandler<Set<String>>(handler) {

					@Override
					public void success(Set<String> userIds) {
						List<String> asList = new ArrayList<>(userIds);
						findImUsers(handler, domainUid, userService, 0, entries, asList, map);
					}
				});
	}

	private void findImUsers(final AsyncHandler<Void> handler, final String domainUid,
			final UserGwtEndpoint userService, final int index, final List<DirEntry> entries,
			final List<String> userIds, final JsMapStringJsObject map) {
		if (index == userIds.size()) {
			JSONArray imEntities = new JSONArray();
			int iindex = 0;
			for (DirEntry dirEntry : entries) {
				JSONValue jsDirEntry = new DirEntryGwtSerDer().serialize(dirEntry);
				imEntities.set(iindex++, jsDirEntry);
			}
			map.put(DomainKeys.imEntities.name(), imEntities.getJavaScriptObject());
			map.put(DomainKeys.currentImEntities.name(), imEntities.getJavaScriptObject());
			handler.success(null);
			return;
		}
		final String userId = userIds.get(index);
		userService.getComplete(userId, new DefaultAsyncHandler<ItemValue<User>>(handler) {

			@Override
			public void success(ItemValue<User> user) {
				GWT.log("adding user");
				DirEntry dirEntry = new DirEntry();
				dirEntry.kind = DirEntry.Kind.USER;
				dirEntry.entryUid = userId;
				dirEntry.displayName = user.value.login;
				entries.add(dirEntry);
				findImUsers(handler, domainUid, userService, index + 1, entries, userIds, map);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();
		final String domainUid = map.getString(DomainKeys.domainUid.name());

		JSONArray entitledImEntities = new JSONArray(map.get(DomainKeys.imEntities.name()));
		JSONArray oldEntitledImEntities = new JSONArray(map.get(DomainKeys.currentImEntities.name()));
		removeObsoleteEntities(handler, map, domainUid, oldEntitledImEntities, entitledImEntities, 0);
	}

	private void removeObsoleteEntities(final AsyncHandler<Void> handler, final JsMapStringJsObject map,
			final String domainUid, final JSONArray obsoleteEntities, final JSONArray entitledImEntities,
			final int index) {

		if (index == obsoleteEntities.size()) {
			saveImEntities(handler, map, domainUid, entitledImEntities, 0);
			return;
		}
		final DirEntry entry = new DirEntryGwtSerDer().deserialize(obsoleteEntities.get(index));
		if (!contains(entitledImEntities, entry)) {
			if (entry.kind == DirEntry.Kind.USER) {
				final UserGwtEndpoint userService = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
				userService.getRoles(entry.entryUid, new DefaultAsyncHandler<Set<String>>(handler) {
					@Override
					public void success(Set<String> value) {
						value.remove(ImRole.role.getRole());
						userService.setRoles(entry.entryUid, value, new DefaultAsyncHandler<Void>(handler) {

							@Override
							public void success(Void value) {
								removeObsoleteEntities(handler, map, domainUid, obsoleteEntities, entitledImEntities,
										index + 1);
							}
						});
					}
				});
			} else {
				final GroupGwtEndpoint groupService = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
				groupService.getRoles(entry.entryUid, new DefaultAsyncHandler<Set<String>>(handler) {
					@Override
					public void success(Set<String> value) {
						value.remove(ImRole.role.getRole());
						groupService.setRoles(entry.entryUid, value, new DefaultAsyncHandler<Void>(handler) {

							@Override
							public void success(Void value) {
								removeObsoleteEntities(handler, map, domainUid, obsoleteEntities, entitledImEntities,
										index + 1);
							}
						});
					}
				});
			}

		} else {
			removeObsoleteEntities(handler, map, domainUid, obsoleteEntities, entitledImEntities, index + 1);
		}
	}

	private void saveImEntities(AsyncHandler<Void> handler, JsMapStringJsObject map, String domainUid,
			JSONArray entitledImEntities, int index) {
		if (index == entitledImEntities.size()) {
			handler.success(null);
			return;
		}
		DirEntry entry = new DirEntryGwtSerDer().deserialize(entitledImEntities.get(index));
		if (entry.kind == DirEntry.Kind.USER) {
			addImRoleToUser(handler, domainUid, entry);
		} else {
			addImRoleToGroup(handler, domainUid, entry);
		}
		saveImEntities(handler, map, domainUid, entitledImEntities, index + 1);
	}

	private void addImRoleToUser(final AsyncHandler<Void> handler, String domainUid, final DirEntry entry) {
		final UserGwtEndpoint userService = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		userService.getRoles(entry.entryUid, new DefaultAsyncHandler<Set<String>>(handler) {

			@Override
			public void success(Set<String> value) {
				value.add(ImRole.role.getRole());
				userService.setRoles(entry.entryUid, value, new DefaultAsyncHandler<Void>(handler) {

					@Override
					public void success(Void value) {
					}
				});
			}
		});
	}

	private void addImRoleToGroup(final AsyncHandler<Void> handler, String domainUid, final DirEntry entry) {
		final GroupGwtEndpoint groupService = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		groupService.getRoles(entry.entryUid, new DefaultAsyncHandler<Set<String>>(handler) {

			@Override
			public void success(Set<String> value) {
				value.add(ImRole.role.getRole());
				groupService.setRoles(entry.entryUid, value, new DefaultAsyncHandler<Void>(handler) {

					@Override
					public void success(Void value) {
					}
				});
			}
		});
	}

	private boolean contains(JSONArray array, DirEntry entry) {
		for (int i = 0; i < array.size(); i++) {
			JsDirEntry e = array.get(i).isObject().getJavaScriptObject().cast();
			if (e.getKind().equals(entry.kind) && e.getEntryUid().equals(entry.entryUid)) {
				return true;
			}
		}
		return false;
	}

}
