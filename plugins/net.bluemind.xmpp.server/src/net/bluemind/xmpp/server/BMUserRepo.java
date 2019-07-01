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
package net.bluemind.xmpp.server;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.user.api.User;
import net.bluemind.xmpp.server.data.IDataProvider;
import net.bluemind.xmpp.server.data.VCardProvider;
import tigase.db.DBInitException;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.db.UserRepositoryMDImpl;
import tigase.xmpp.BareJID;

public class BMUserRepo implements UserRepository {

	private static final Logger logger = LoggerFactory.getLogger(BMUserRepo.class);
	private final ConcurrentHashMap<String, IDataProvider> providers;
	private static final String REPO_PATH = "/usr/share/bm-xmpp/repository/";
	private static BMSessionManager sessionManager;

	public BMUserRepo() {
		logger.info("**************** USER REPO LOADED ***********");
		providers = new ConcurrentHashMap<>();
		providers.put("/public/vcard-temp/vCard", new VCardProvider());
		File f = new File(REPO_PATH);
		f.mkdirs();

	}

	@Override
	public void addDataList(BareJID user, String subnode, String key, String[] list)
			throws UserNotFoundException, TigaseDBException {
		logger.debug("addDataList {} {} {} {}", user, subnode, key, list);
	}

	@Override
	public void addUser(BareJID user) throws UserExistsException, TigaseDBException {
		logger.debug("addUser {}", user);
	}

	@Override
	public String getData(BareJID user, String subnode, String key, String def)
			throws UserNotFoundException, TigaseDBException {
		String k = (subnode != null ? "/" + subnode : "") + "/" + key;
		String ret = null;
		File p = path(user, subnode, key);
		if (providers.containsKey(k)) {
			IDataProvider prov = providers.get(k);
			logger.info("[" + user + "] getData[" + k + "] from " + prov);
			ret = prov.getFor(user);
		} else if ("roster".equals(key)) {
			ret = CF.getRoster(user.toString());
		} else if (p.exists()) {
			try {
				ret = new String(Files.toByteArray(p));
			} catch (IOException e) {
				throw new TigaseDBException(e.getMessage(), e);
			}
		} else if (def != null) {
			ret = def;
		} else if (logger.isDebugEnabled()) {
			logger.debug("[" + user + "] Don't know what to do for '" + k + "'");
		}

		return ret;
	}

	@Override
	public String getData(BareJID user, String subnode, String key) throws UserNotFoundException, TigaseDBException {
		return getData(user, subnode, key, null);
	}

	@Override
	public String getData(BareJID user, String key) throws UserNotFoundException, TigaseDBException {
		return getData(user, null, key, null);
	}

	@Override
	public String[] getDataList(BareJID user, String subnode, String key)
			throws UserNotFoundException, TigaseDBException {
		logger.debug("getDataList");
		return null;
	}

	@Override
	public String[] getKeys(BareJID user, String subnode) throws UserNotFoundException, TigaseDBException {
		logger.debug("getKeys");
		return null;
	}

	@Override
	public String[] getKeys(BareJID user) throws UserNotFoundException, TigaseDBException {
		logger.debug("getKeys " + user);
		return null;
	}

	@Override
	public String getResourceUri() {
		logger.debug("getResourceUri");
		return null;
	}

	@Override
	public String[] getSubnodes(BareJID user, String subnode) throws UserNotFoundException, TigaseDBException {
		String[] ret = new String[0];
		logger.debug("getSubnodes u: " + user + " parent: " + subnode + " => " + ret.length + " nodes.");
		return ret;
	}

	@Override
	public String[] getSubnodes(BareJID user) throws UserNotFoundException, TigaseDBException {
		return getSubnodes(user, null);
	}

	@Override
	public long getUserUID(BareJID user) throws TigaseDBException {
		logger.debug("getUserUID {} ", user);
		ItemValue<User> userItem = CF.user(user);
		if (userItem == null) {
			return -1;
		} else {
			return userItem.internalId;
		}
	}

	@Override
	public List<BareJID> getUsers() throws TigaseDBException {
		logger.debug("getUsers");
		return null;
	}

	@Override
	public long getUsersCount() {
		logger.debug("getUsersCount");
		return 1;
	}

	@Override
	public long getUsersCount(String domain) {
		logger.debug("getUsersCount " + domain);
		return 1;
	}

	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		logger.debug("initRepository uri: " + resource_uri);
	}

	@Override
	public void removeData(BareJID user, String subnode, String key) throws UserNotFoundException, TigaseDBException {
		logger.debug("removeData");
	}

	@Override
	public void removeData(BareJID user, String key) throws UserNotFoundException, TigaseDBException {
		logger.debug("removeData");
	}

	@Override
	public void removeSubnode(BareJID user, String subnode) throws UserNotFoundException, TigaseDBException {
		logger.debug("removeSubnode");
	}

	@Override
	public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
		logger.debug("removeUser");
	}

	@Override
	public void setData(BareJID user, String subnode, String key, String value)
			throws UserNotFoundException, TigaseDBException {

		if ("roster".equals(key)) {
			CF.setRoster(user.toString(), value);
		} else {
			logger.debug("[" + user + "] setData [" + subnode + "][" + key + "]: " + value);
			ensureParent(user, subnode);
			File f = path(user, subnode, key);
			try {
				Files.write(value.getBytes(), f);
			} catch (IOException e) {
				throw new TigaseDBException(e.getMessage(), e);
			}
		}

	}

	@Override
	public void setData(BareJID user, String key, String value) throws UserNotFoundException, TigaseDBException {
		setData(user, null, key, value);
	}

	@Override
	public void setDataList(BareJID user, String subnode, String key, String[] list)
			throws UserNotFoundException, TigaseDBException {
		logger.debug("setDataList " + key + " " + list);
	}

	@Override
	public boolean userExists(BareJID user) {
		try {
			CF.jidToUid(user);
			return true;
		} catch (TigaseDBException te) {
			logger.error("userExists " + user + " => false");
			return false;
		}
	}

	/**
	 * @param latd
	 * @param subnode
	 * @param key
	 * @return
	 * @throws TigaseDBException
	 */
	private File path(BareJID latd, String subnode, String key) throws TigaseDBException {
		StringBuilder path = new StringBuilder(256);
		path.append(REPO_PATH);

		if (latd.getLocalpart() != null) {
			path.append(CF.jidToUid(latd)).append('/');
		} else {
			path.append(latd.getDomain()).append('/');
		}

		if (subnode != null) {
			path.append(subnode).append('/');
		}
		path.append(key).append(".bin");
		return new File(path.toString());
	}

	/**
	 * @param latd
	 * @param subnode
	 * @throws TigaseDBException
	 */
	private void ensureParent(BareJID latd, String subnode) throws TigaseDBException {
		StringBuilder path = new StringBuilder(256);
		path.append(REPO_PATH);
		if (latd.getLocalpart() != null) {
			path.append(CF.jidToUid(latd)).append('/');
		} else {
			path.append(latd.getDomain()).append('/');
		}
		if (subnode != null) {
			path.append(subnode).append('/');
		}
		String p = path.toString();
		new File(p).mkdirs();
	}

	public static void setSessionManager(BMSessionManager bmSessionManager) {
		BMUserRepo.sessionManager = bmSessionManager;
	}

	public static BMSessionManager getSessionManager() {
		return BMUserRepo.sessionManager;
	}

}
