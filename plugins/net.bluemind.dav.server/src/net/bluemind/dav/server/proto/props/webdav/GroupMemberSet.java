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
package net.bluemind.dav.server.proto.props.webdav;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.dav.server.Proxy;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public class GroupMemberSet implements IPropertyValue {
	public static final QName NAME = new QName(NS.WEBDAV, "group-member-set");
	private static final Logger logger = LoggerFactory.getLogger(GroupMemberSet.class);
	private List<Property> scope;
	private ArrayList<UserMember> members;

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new GroupMemberSet();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		if (members == null || scope == null) {
			return;
		}
		for (UserMember um : members) {
			DirEntry dir = um.getUserUid();
			Element re = DOMUtils.createElement(parent, "d:response");
			String princ = Proxy.path + "/principals/__uids__/" + dir.entryUid + "/";
			DOMUtils.createElementAndText(re, "d:href", princ);
			Element pse = DOMUtils.createElement(re, "d:propstat");
			Element pre = DOMUtils.createElement(pse, "d:prop");
			DOMUtils.createElementAndText(pse, "d:status", "HTTP/1.1 200 OK");

			for (Property prop : scope) {
				QName qn = prop.getQName();
				Element ve = DOMUtils.createElement(pre, qn.getPrefix() + ":" + qn.getLocalPart());
				switch (qn.getLocalPart()) {
				case "displayname":
					ve.setTextContent(dir.displayName);
					break;
				case "calendar-user-address-set":
					DOMUtils.createElementAndText(ve, "d:href", princ);
					if (dir.email != null && !dir.email.isEmpty()) {
						DOMUtils.createElementAndText(ve, "d:href", "mailto:" + dir.email);
					}
					break;
				case "email-address-set":
					if (dir.email != null && !dir.email.isEmpty()) {
						DOMUtils.createElementAndText(ve, "cso:email-address", dir.email);
					}
					break;
				default:
					logger.warn("Unsupported prop: {}", qn);
				}
			}
		}
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		logger.info("fetch");
		// TODO
	}

	@Override
	public void expand(LoggedCore lc, DavResource dr, List<Property> scope) throws Exception {
		logger.info("expand");
		ResType rt = dr.getResType();
		switch (rt) {
		case PRINCIPAL_CAL_PROXY_RW:
			Matcher m = rt.matcher(dr.getPath());
			m.find();
			String rw = m.group(2);
			String defaultCalUid = ICalendarUids.defaultUserCalendar(m.group(1));
			logger.info("rw: {}, defaultCalUid: {}", rw, defaultCalUid);
			IContainerManagement acl = lc.getCore().instance(IContainerManagement.class, defaultCalUid);
			List<AccessControlEntry> acls = acl.getAccessControlList();
			logger.info("Fetching ACLs on {}", defaultCalUid);
			this.members = new ArrayList<UserMember>(acls.size());
			IDirectory dirApi = lc.getCore().instance(IDirectory.class, lc.getDomain());
			for (AccessControlEntry ace : acls) {
				logger.info(" * On ace for subject {}", ace.subject);
				Verb r = ace.verb;
				if ("read".equals(rw)) {
					// skip acls with write or without read
					if (r != Verb.Read) {
						continue;
					}
				}
				if ("write".equals(rw)) {
					// skip acls without write
					if (r != Verb.Write) {
						continue;
					}
				}
				if (ace.subject.equals(lc.getUser().uid)) {
					// skip my own acl
					continue;
				}

				DirEntry dir = dirApi.getEntry(ace.subject);
				if (dir != null) {
					members.add(new UserMember(ace, dir));
				} else {
					logger.error("Cannot find dirEntry with subject {}", ace.subject);
				}
			}
			this.scope = scope;
			break;
		default:
			logger.warn("expand unsupported on {}", rt);
			break;
		}
	}

	@Override
	public void set(LoggedCore lc, DavResource dr, Element value) throws Exception {
		ResType rt = dr.getResType();
		logger.info("[{}] set on {}", rt, dr.getPath());
		switch (rt) {
		case PRINCIPAL_CAL_PROXY_RW:
			Matcher m = rt.matcher(dr.getPath());
			m.find();
			String rw = m.group(2);
			String defaultCalUid = ICalendarUids.defaultUserCalendar(m.group(1));
			logger.info("rw: {}, defaultCalUid: {}", rw, defaultCalUid);

			IContainerManagement acl = lc.getCore().instance(IContainerManagement.class, defaultCalUid);
			List<AccessControlEntry> acls = acl.getAccessControlList();
			Map<String, AccessControlEntry> userIdIdx = new HashMap<>();
			for (AccessControlEntry ae : acls) {
				userIdIdx.put(ae.subject, ae);
			}
			if (value != null) {
				NodeList nl = value.getElementsByTagNameNS(NS.WEBDAV, "href");
				int len = nl.getLength();
				DavStore ds = new DavStore(lc);
				for (int i = 0; i < len; i++) {
					String princ = nl.item(i).getTextContent();
					String userEntry = ds.from(princ).getUid();
					logger.info("Grant '{}' to '{}'", rw, userEntry);
					if (userIdIdx.containsKey(userEntry)) {
						// rm previous version
						Iterator<AccessControlEntry> it = acls.iterator();
						while (it.hasNext()) {
							AccessControlEntry ae = it.next();
							if (userEntry.equals(ae.subject)) {
								it.remove();
							}
						}
					}
					Verb newVerb = "read".equals(rw) ? Verb.Read : Verb.Write;
					AccessControlEntry newAce = AccessControlEntry.create(userEntry, newVerb);
					acls.add(newAce);
				}
				acl.setAccessControlList(acls);
			}
			break;
		default:
			logger.warn("Don't know how to set group-member-set.");
		}
	}
}
