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
package net.bluemind.eas.search.ldap;

import java.util.Properties;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.utils.IniFile;

class Configuration {

	private static final String LDAP_CONF_FILE = "/etc/bm-eas/ldap_conf.ini";

	private static final String SEARCH_LDAP_URL = "search.ldap.url";
	private static final String SEARCH_LDAP_BASE = "search.ldap.basedn";
	private static final String SEARCH_LDAP_FILTER = "search.ldap.filter";

	private String baseDn;
	private String filter;
	private Properties env;
	private Logger logger;
	private boolean validConf;

	public Configuration() {
		logger = LoggerFactory.getLogger(getClass());
		IniFile ini = new IniFile(LDAP_CONF_FILE) {
			@Override
			public String getCategory() {
				return null;
			}
		};
		init(ini);
	}

	DirContext getConnection() throws NamingException {
		try {
			return new InitialDirContext(env);
		} catch (NamingException e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	public void init(IniFile ini) {
		String url = ini.getProperty(SEARCH_LDAP_URL);
		baseDn = ini.getProperty(SEARCH_LDAP_BASE);
		filter = ini.getProperty(SEARCH_LDAP_FILTER);

		env = new Properties();
		if (url != null && baseDn != null && filter != null) {
			validConf = true;
		} else {
			logger.warn("Can not find data in file " + LDAP_CONF_FILE + ", research in ldap will not be activated");
			return;
		}

		if (!url.startsWith("ldap://")) {
			url = "ldap://" + url;
		}

		env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
		env.put("java.naming.provider.url", url);

		logger.info("EAS LDAP search initialised, url: " + url + " basedn: " + baseDn + " filter: " + filter
				+ " (valid conf: " + validConf + ")");
	}

	public String getBaseDn() {
		return baseDn;
	}

	public String getFilter() {
		return filter;
	}

	public void cleanup(DirContext ctx) {
		if (ctx != null) {
			try {
				ctx.close();
			} catch (NamingException e) {
			}
		}
	}

	public boolean isValid() {
		return validConf;
	}

}
