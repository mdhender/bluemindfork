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
package net.bluemind.dataprotect.user;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestoreDefinition;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.dataprotect.webappdata.RestoreUserWebAppData;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.TagChanges;
import net.bluemind.tag.api.TagChanges.ItemAdd;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import net.bluemind.user.persistence.UserStore;

public class RestoreUserTask extends BlockingServerTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(RestoreUserTask.class);
	private DataProtectGeneration backup;
	private Restorable item;

	public RestoreUserTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.item = item;
	}

	@Override
	public void run(IServerTaskMonitor monitor) {
		logger.info("Restoring user {}:{}", item.entryUid, item.displayName);

		IServiceProvider live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDataProtect dp = live.instance(IDataProtect.class);
		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext backupContext = bdp.createContextWithData(backup, item);
			IServiceProvider back = backupContext.provider();

			IUser userService = back.instance(IUser.class, item.domainUid);
			ItemValue<User> backupUser = userService.getComplete(item.entryUid);
			List<ItemValue<Group>> memberOf = userService.memberOf(item.entryUid);
			backupUser.value.password = UUID.randomUUID().toString();

			ItemValue<User> liveUser = createOrUpdateLiveUser(live, backupUser);
			item.setLiveEntryUid(liveUser.uid);

			// restore user pwd
			restoreUserPassword(backupContext, backupUser.internalId, liveUser.internalId);

			restoreUserSettings(live, back);
			restoreUserTags(live, back);
			restoreUserFilters(live, back);
			restoreGroupMembership(live, memberOf);
		} catch (Exception e) {
			logger.warn("Error while restoring user", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		logger.info("Restoring user mbox {}:{}", item.entryUid, item.displayName);

		RestoreDefinition restoreBox = new RestoreDefinition("replace.mailbox", backup.id, item);
		executeTask(dp, restoreBox);

		logger.info("Restoring user addressbooks {}:{}", item.entryUid, item.displayName);

		RestoreDefinition restoreBooks = new RestoreDefinition("replace.books", backup.id, item);
		executeTask(dp, restoreBooks);

		logger.info("Restoring user calendars {}:{}", item.entryUid, item.displayName);

		RestoreDefinition restoreCalendars = new RestoreDefinition("replace.calendars", backup.id, item);
		executeTask(dp, restoreCalendars);

		logger.info("Restoring user todolists {}:{}", item.entryUid, item.displayName);

		RestoreDefinition restoreTodolists = new RestoreDefinition("replace.todolists", backup.id, item);
		executeTask(dp, restoreTodolists);

		logger.info("Restoring user notes {}:{}", item.entryUid, item.displayName);

		RestoreDefinition restoreNotes = new RestoreDefinition("replace.notes", backup.id, item);
		executeTask(dp, restoreNotes);

		RestoreDefinition restoreWebAppData = new RestoreDefinition(RestoreUserWebAppData.identifier, backup.id, item);
		executeTask(dp, restoreWebAppData);

		monitor.end(true, "user " + item.entryUid + ":" + item.displayName + " restored", "");
	}

	private ItemValue<User> createOrUpdateLiveUser(IServiceProvider live, ItemValue<User> backupUser) {
		IUser userServiceLive = live.instance(IUser.class, item.domainUid);

		Optional<ItemValue<User>> liveUser = Optional
				.ofNullable(Optional.ofNullable(userServiceLive.getComplete(item.entryUid))
						.orElse(userServiceLive.byLogin(backupUser.value.login)));

		if (liveUser.isPresent()) {
			userServiceLive.update(liveUser.get().uid, backupUser.value);
			return liveUser.get();
		}

		userServiceLive.create(item.entryUid, backupUser.value);
		return userServiceLive.getComplete(item.entryUid);
	}

	private void restoreUserPassword(BmContext backupContext, long oldUserId, long newUserId) throws SQLException {
		DataSource ds = backupContext.getDataSource();
		ContainerStore cs = new ContainerStore(backupContext, ds, backupContext.getSecurityContext());
		Container domain = cs.get(item.domainUid);
		UserStore userStore = new UserStore(ds, domain);
		String pwd = userStore.getPassword(oldUserId);

		ds = JdbcActivator.getInstance().getDataSource();
		cs = new ContainerStore(backupContext, ds, backupContext.getSecurityContext());
		domain = cs.get(item.domainUid);
		userStore = new UserStore(JdbcActivator.getInstance().getDataSource(), domain);
		Item i = Item.create(item.entryUid, null);
		i.id = newUserId;
		userStore.setPassword(i, pwd, true);
	}

	private void restoreUserFilters(IServiceProvider live, IServiceProvider back) {
		IMailboxes mboxesBackup = back.instance(IMailboxes.class, item.domainUid);
		IMailboxes mboxesLive = live.instance(IMailboxes.class, item.domainUid);
		mboxesLive.setMailboxFilter(item.liveEntryUid(), mboxesBackup.getMailboxFilter(item.entryUid));
	}

	private void restoreUserTags(IServiceProvider live, IServiceProvider back) {
		ITags tagsBackup = back.instance(ITags.class, ITagUids.TYPE + "_" + item.entryUid);
		ITags tagsLive = live.instance(ITags.class, ITagUids.TYPE + "_" + item.liveEntryUid());

		List<TagChanges.ItemAdd> userTags = tagsBackup.all().stream().map(tag -> ItemAdd.create(tag.uid, tag.value))
				.collect(Collectors.toList());

		tagsLive.updates(TagChanges.create(userTags, Collections.emptyList(), Collections.emptyList()));
	}

	private void restoreGroupMembership(IServiceProvider live, List<ItemValue<Group>> memberOf) {
		IGroup groupService = live.instance(IGroup.class, item.domainUid);

		for (ItemValue<Group> group : memberOf) {
			// add to group, if group still exists
			if (null != groupService.getComplete(group.uid)) {
				if (!groupService.getMembers(group.uid).contains(Member.user(item.liveEntryUid()))) {
					groupService.add(group.uid, Arrays.asList(Member.user(item.liveEntryUid())));
				}
			}
		}
	}

	private void restoreUserSettings(IServiceProvider live, IServiceProvider back) {
		IUserSettings settings = back.instance(IUserSettings.class, item.domainUid);
		IUserSettings userSettingsLive = live.instance(IUserSettings.class, item.domainUid);
		userSettingsLive.set(item.liveEntryUid(), settings.get(item.entryUid));
	}

	private void executeTask(IDataProtect dp, RestoreDefinition definition) {
		TaskRef run = dp.run(definition);
		TaskStatus taskResult = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), run);
		if (taskResult.state == TaskStatus.State.InError) {
			throw new ServerFault("Error while restoring user " + taskResult.lastLogEntry);
		}
	}
}
