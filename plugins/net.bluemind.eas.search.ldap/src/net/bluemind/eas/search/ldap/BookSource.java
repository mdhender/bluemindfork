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

import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.dto.search.GAL;
import net.bluemind.eas.dto.search.SearchRequest;
import net.bluemind.eas.dto.search.SearchResult;
import net.bluemind.eas.dto.search.StoreName;
import net.bluemind.eas.search.ISearchSource;
import net.bluemind.eas.utils.LdapUtils;

public class BookSource implements ISearchSource {

	private Configuration conf;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public BookSource() {
		conf = new Configuration();
	}

	private String uniqueAttribute(String string, Map<String, List<String>> m) {
		List<String> cnl = m.get(string);
		if (cnl == null || cnl.size() == 0) {
			return "";
		} else {
			return cnl.get(0);
		}
	}

	@Override
	public StoreName getStoreName() {
		return StoreName.gal;
	}

	@Override
	public Results<SearchResult> search(BackendSession bs, SearchRequest request) {
		Results<SearchResult> ret = new Results<SearchResult>();
		if (conf.isValid()) {
			logger.info("Perform LDAP search");
			DirContext ctx = null;
			String domain = "";
			int idx = bs.getLoginAtDomain().indexOf("@");
			if (idx > 0) {
				domain = bs.getLoginAtDomain().substring(idx + 1);
			}
			try {
				ctx = conf.getConnection();
				LdapUtils u = new LdapUtils(ctx, conf.getBaseDn().replaceAll("%d", domain));
				List<Map<String, List<String>>> l = u.getAttributes(conf.getFilter(), request.store.query.value,
						new String[] { "displayName", "cn", "sn", "givenName", "mail", "telephoneNumber", "mobile" });
				l = l.subList(0, Math.min(99, l.size()));
				for (Map<String, List<String>> m : l) {
					String sn = uniqueAttribute("sn", m);
					String givenName = uniqueAttribute("givenName", m);
					String cn = uniqueAttribute("cn", m);
					String display = uniqueAttribute("displayName", m);
					List<String> phones = m.get("telephoneNumber");
					if (sn.length() == 0 || givenName.length() == 0) {
						sn = cn;
						givenName = "";
					}
					GAL gal = new GAL();
					if (display != null && display.length() > 0) {
						gal.setDisplayName(display);
					} else {
						gal.setDisplayName(givenName + " " + sn);
					}
					gal.lastname = sn;
					gal.firstname = givenName;
					if (phones != null) {
						if (phones.size() > 0) {
							gal.phone = phones.get(0);
						}
						if (phones.size() > 1) {
							gal.homePhone = phones.get(1);
						}
					}
					gal.mobilePhone = uniqueAttribute("mobile", m);
					List<String> mails = m.get("mail");
					if (mails != null && mails.iterator().hasNext()) {
						gal.emailAddress = mails.iterator().next();
					}
					// TODO photo

					if (gal.getDisplayName() != null) {
						SearchResult sr = new SearchResult();
						sr.searchProperties.gal = gal;
						ret.add(sr);
					}
				}
			} catch (NamingException e) {
				logger.error("findAll error", e);
			} finally {
				conf.cleanup(ctx);
			}
		}
		return ret;
	}
}
