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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.directory.service.internal;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;

public class DirectoryService implements IDirectory {

	public static List<DirectoryDecorator> decorators;

	private final BmContext context;
	private final Directory directory;

	public DirectoryService(BmContext context, Container dirContainer, ItemValue<Domain> domain) {
		this.context = context;
		this.directory = new Directory(context, dirContainer, domain);
	}

	@Override
	public DirEntry getRoot() throws ServerFault {
		ItemValue<DirEntry> decorated = decorate(directory.getRoot());
		if (decorated == null) {
			return null;
		}
		return decorated.value;
	}

	@Override
	public DirEntry getEntry(String path) throws ServerFault {
		ItemValue<DirEntry> decorated = decorate(directory.getEntry(path));
		if (decorated == null) {
			return null;
		}
		return decorated.value;
	}

	@Override
	public List<DirEntry> getEntries(String path) throws ServerFault {
		return directory.getEntries(path).stream().map(this::decorate).map(e -> e.value).collect(Collectors.toList());
	}

	@Override
	public TaskRef delete(String path) throws ServerFault {
		return directory.delete(path);
	}

	@Override
	public ItemValue<VCard> getVCard(String uid) throws ServerFault {
		return directory.getVCard(uid);
	}

	@Override
	public TaskRef deleteByEntryUid(String entryUid) throws ServerFault {
		return directory.deleteByEntryUid(entryUid);
	}

	@Override
	public ContainerChangelog changelog(Long since) throws ServerFault {
		return directory.changelog(since);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		return directory.changeset(since);
	}

	@Override
	public ListResult<ItemValue<DirEntry>> search(DirEntryQuery query) throws ServerFault {
		ListResult<ItemValue<DirEntry>> search = directory.search(query);
		return ListResult.create(search.values.stream().map(this::decorate).collect(Collectors.toList()), search.total);
	}

	@Override
	public DirEntry findByEntryUid(String entryUid) throws ServerFault {
		ItemValue<DirEntry> decorated = decorate(directory.findByEntryUid(entryUid));
		if (decorated == null) {
			return null;
		}
		return decorated.value;
	}

	@Override
	public byte[] getEntryIcon(String entryUid) throws ServerFault {
		return directory.getEntryIcon(entryUid);
	}

	@Override
	public byte[] getEntryPhoto(String entryUid) throws ServerFault {
		return directory.getEntryPhoto(entryUid);
	}

	@Override
	public byte[] getIcon(String path) throws ServerFault {
		return directory.getIcon(path);
	}

	@Override
	public Set<String> getRolesForDirEntry(String entryUid) throws ServerFault {
		return directory.getRolesForDirEntry(entryUid);
	}

	@Override
	public Set<String> getRolesForOrgUnit(String orgUnitUid) throws ServerFault {
		return directory.getRolesForOrgUnit(orgUnitUid);
	}

	@Override
	public DirEntry getByEmail(String email) {
		ItemValue<DirEntry> decorated = decorate(directory.getByEmail(email.toLowerCase()));
		if (decorated == null) {
			return null;
		}
		return decorated.value;
	}

	@Override
	public List<ItemValue<DirEntry>> getMultiple(List<String> id) {
		return directory.getMultiple(id).stream().map(this::decorate).collect(Collectors.toList());
	}

	@Override
	public TaskRef xfer(String entryUid, String serverUid) throws ServerFault {
		return directory.xfer(entryUid, serverUid);
	}

	private ItemValue<DirEntry> decorate(ItemValue<DirEntry> entry) {
		if (entry != null && entry.value != null) {
			decorators.forEach(decorator -> decorator.decorate(context, entry));
		}
		return entry;
	}

	@Override
	public List<ItemValue<DirEntry>> getByRoles(List<String> roles) throws ServerFault {
		return directory.getByRoles(roles).stream().map(this::decorate).collect(Collectors.toList());
	}

}
