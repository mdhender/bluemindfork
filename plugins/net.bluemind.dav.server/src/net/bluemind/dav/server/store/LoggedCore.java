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
package net.bluemind.dav.server.store;

import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.shareddata.Shareable;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;

public final class LoggedCore implements Shareable {

	private final IServiceProvider core;

	private Map<String, String> prefs;

	private static final Logger logger = LoggerFactory.getLogger(LoggedCore.class);

	private ItemValue<User> user;
	private String domain;

	public LoggedCore(IServiceProvider sp) {
		this.core = sp;
		try {
			AuthUser me = core.instance(IAuthentication.class).getCurrentUser();
			this.domain = me.domainUid;
			IUser userApi = core.instance(IUser.class, me.domainUid);
			this.user = userApi.getComplete(me.uid);
			IUserSettings settingsApi = core.instance(IUserSettings.class, me.domainUid);
			prefs = settingsApi.get(me.uid);
		} catch (Exception e) {
			Throwables.propagate(e);
		}
	}

	public IServiceProvider getCore() {
		return core;
	}

	public ItemValue<User> getUser() {
		return user;
	}

	public Map<String, String> getPrefs() {
		return prefs;
	}

	public long getLastMod(DavResource dr) {
		try {
			ContainerDescriptor cd = vStuffContainer(dr);
			if ("todolist".equals(cd.type)) {
				return core.instance(ITodoList.class, cd.uid).getVersion();
			} else if ("calendar".equals(cd.type)) {
				return core.instance(ICalendar.class, cd.uid).getVersion();
			} else if ("addressbook".equals(cd.type)) {
				return core.instance(IAddressBook.class, cd.uid).getVersion();
			} else {
				throw new ServerFault("unknow DavResource " + cd.type, ErrorCode.UNKNOWN);
			}
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public void logout() {
		try {
			core.instance(IAuthentication.class).logout();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public ContainerDescriptor vStuffContainer(DavResource dr) {
		String uid = dr.getUid();
		logger.debug("[{}@{}] Fetching container matching uid {}", user.value.login, domain, uid);
		try {
			Matcher m = dr.getResType().matcher(dr.getPath());
			m.find();
			String tlUid = m.group(2);
			logger.debug("tlUid: {}", tlUid);
			tlUid = URLDecoder.decode(tlUid, "utf-8");
			logger.debug("Decoded: {}", tlUid);
			IContainers containers = core.instance(IContainers.class);
			return containers.get(tlUid);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public DirEntry principalDirEntry(DavResource dr) {
		try {
			Matcher m = dr.getResType().matcher(dr.getPath());
			m.find();
			String entryUid = m.group(1);
			logger.debug("entryUid: {}", entryUid);
			entryUid = URLDecoder.decode(entryUid, "utf-8");
			logger.debug("Decoded: {}", entryUid);
			IDirectory dir = core.instance(IDirectory.class, getDomain());
			return dir.findByEntryUid(entryUid);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public Map<String, ContainerDescriptor> getBooks() {
		try {
			IContainers containers = core.instance(IContainers.class);
			String bookUid = IAddressBookUids.defaultUserAddressbook(user.uid);
			ContainerDescriptor cd = containers.get(bookUid);
			Map<String, ContainerDescriptor> ret = ImmutableMap.of(bookUid, cd);
			return ret;
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public String getDomain() {
		return domain;
	}
}
