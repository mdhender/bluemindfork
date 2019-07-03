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

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.user.api.User;
import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.DBInitException;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepositoryMDImpl;
import tigase.util.Base64;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.BareJID;

public class BMAuthRepo extends UserRepositoryMDImpl implements AuthRepository {

	private static final Logger logger = LoggerFactory.getLogger(BMAuthRepo.class);

	private static final String[] non_sasl_mechs = { "password" };

	private static final String[] sasl_mechs = { "PLAIN" };

	public BMAuthRepo() {
		logger.info("*********** AUTH REPO LOADED ************");
	}

	@Override
	public void addUser(BareJID user, String password) throws UserExistsException, TigaseDBException {
		logger.info("addUser " + user + " p: " + password);
	}

	@Override
	public boolean digestAuth(BareJID user, String digest, String id, String alg)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		logger.info("digestAuth " + user);
		return false;
	}

	@Override
	public String getResourceUri() {
		logger.info("trace", new Throwable());
		return null;
	}

	@Override
	public long getUsersCount() {
		logger.info("getUsersCount");
		return 0;
	}

	@Override
	public long getUsersCount(String domain) {
		logger.info("getUsersCount " + domain);
		return 0;
	}

	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		logger.info("initRepository uri: " + resource_uri);
	}

	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {
		logger.info("[" + user + "] logout");
	}

	@Override
	public boolean otherAuth(Map<String, Object> props)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		if (logger.isDebugEnabled()) {
			logger.debug("otherAuth");
			for (Entry<String, Object> s : props.entrySet()) {
				logger.debug(" * {} => {}", s.getKey(), s.getValue());
			}
		}
		String proto = (String) props.get(PROTOCOL_KEY);

		if (proto.equals(PROTOCOL_VAL_SASL)) {
			if (props.get(MACHANISM_KEY).equals("PLAIN")) {
				return saslAuth(props);
			}
		} else if (proto.equals(PROTOCOL_VAL_NONSASL)) {
			String password = (String) props.get(PASSWORD_KEY);
			BareJID user_id = (BareJID) props.get(USER_ID_KEY);
			String latd = user_id.getLocalpart() + "@" + user_id.getDomain();
			boolean auth = CF.login(latd, password);
			if (auth) {
				try {
					ItemValue<User> u = CF.user(BareJID.bareJIDInstance(latd));
					props.put(USER_ID_KEY, u.value.defaultEmail().address);
				} catch (TigaseStringprepException e) {
					logger.error(e.getMessage(), e);
				}
			}
			return auth;
		}

		throw new AuthorizationException("Protocol is not supported: " + proto);
	}

	/**
	 * @param authProps
	 * @return
	 * @throws TigaseDBException
	 */
	private boolean saslAuth(Map<String, Object> authProps) throws TigaseDBException {
		logger.info("saslAuth {}", authProps);
		String b64 = (String) authProps.get("data");
		if (b64 == null) {
			logger.error("sasl auth with null data", new Exception("sasl auth with null data"));
			throw new TigaseDBException("sasl auth with null data");
		}

		byte[] dec = Base64.decode(b64);

		int auth_idx = 0;
		while ((dec[auth_idx] != 0) && (auth_idx < dec.length)) {
			++auth_idx;
		}
		int user_idx = ++auth_idx;
		while ((dec[user_idx] != 0) && (user_idx < dec.length)) {
			++user_idx;
		}

		String login = new String(dec, auth_idx, user_idx - auth_idx);
		++user_idx;
		if (!login.contains("@") && authProps.containsKey("server-name")) {
			login = login + "@" + authProps.get("server-name");
		}
		logger.info("login: '" + login + "'");

		String passwd = new String(dec, user_idx, dec.length - user_idx);
		logger.debug("passwd: '" + passwd + "'");

		boolean ret = CF.login(login, passwd);

		if (ret) {
			// authProps.put(AuthRepository.RESULT_KEY, "challenge");
			try {
				ItemValue<User> u = CF.user(BareJID.bareJIDInstance(login));
				authProps.put(AuthRepository.USER_ID_KEY, u.value.defaultEmail().address);
				logger.info("User " + login + " authenticated.");
			} catch (TigaseStringprepException e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			logger.error("Fail to authenticate " + login);
		}

		return ret;
	}

	@Override
	public boolean plainAuth(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		logger.info("plainAuth");
		return CF.login(user.toString(), password);
	}

	@Override
	public void queryAuth(Map<String, Object> authProps) {
		if (logger.isInfoEnabled()) {
			logger.info("queryAuth");
			for (Entry<String, Object> s : authProps.entrySet()) {
				logger.info(" * {} => {}", s.getKey(), s.getValue());
			}
		}
		String protocol = (String) authProps.get(PROTOCOL_KEY);

		if (protocol.equals(PROTOCOL_VAL_NONSASL)) {
			authProps.put(RESULT_KEY, non_sasl_mechs);
			logger.debug("=> non_sasl_mechs selected.");
		} else if (protocol.equals(PROTOCOL_VAL_SASL)) {
			authProps.put(RESULT_KEY, sasl_mechs);
			logger.debug("=> sasl_mechs selected.");
		}
	}

	@Override
	public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
		logger.info("rmUser");

	}

	@Override
	public void updatePassword(BareJID user, String password) throws UserNotFoundException, TigaseDBException {
		logger.info("updatePassword");
	}

	@Override
	public String getPassword(BareJID arg0) throws UserNotFoundException, TigaseDBException {
		return null;
	}

	@Override
	public boolean isUserDisabled(BareJID arg0) throws UserNotFoundException, TigaseDBException {
		return false;
	}

	@Override
	public void setUserDisabled(BareJID arg0, Boolean arg1) throws UserNotFoundException, TigaseDBException {
		// TODO Auto-generated method stub

	}

}
