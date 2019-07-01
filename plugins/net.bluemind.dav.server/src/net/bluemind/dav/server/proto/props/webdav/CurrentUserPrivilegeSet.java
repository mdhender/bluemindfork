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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.Privileges;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.dav.server.xml.DOMUtils;

public class CurrentUserPrivilegeSet implements IPropertyValue {

	private static final Logger logger = LoggerFactory.getLogger(CurrentUserPrivilegeSet.class);

	public static final QName NAME = new QName(NS.WEBDAV, "current-user-privilege-set");

	private Set<QName> privileges;

	@Override
	public QName getName() {
		return NAME;
	}

	@Override
	public void appendValue(Element parent) {
		for (QName p : privileges) {
			Element pe = DOMUtils.createElement(parent, "d:privilege");
			DOMUtils.createElement(pe, p.getPrefix() + ":" + p.getLocalPart());
		}
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new CurrentUserPrivilegeSet();
			}
		};
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) {
		ResType rt = dr.getResType();
		this.privileges = new HashSet<>();

		if (rt == ResType.DROPBOX) {
			privileges.add(Privileges.READ);
			privileges.add(Privileges.READ_CU_PRIV_SET);
			return;
		}

		if (rt.isCalChild() || rt == ResType.CALENDAR) {
			privileges.add(Privileges.READ);
			privileges.add(Privileges.READ_FREE_BUSY);
			privileges.add(Privileges.UNLOCK);
			privileges.add(Privileges.READ_ACL);
			privileges.add(Privileges.READ_CU_PRIV_SET);
			if (dr.getUid().equals(lc.getUser().uid)) {
				logger.info("My CALENDAR");
				// privileges.add(Privileges.ALL);
				privileges.add(Privileges.WRITE);
				privileges.add(Privileges.WRITE_PROPS);
				privileges.add(Privileges.WRITE_CONTENT);
				privileges.add(Privileges.WRITE_ACL);
				// prevent create calendar on server
				// privileges.add(Privileges.BIND);
				// privileges.add(Privileges.UNBIND);
			} else if (rt == ResType.VSTUFF_CONTAINER) {
				try {
					ContainerDescriptor cde = lc.vStuffContainer(dr);
					logger.info("***** [{} {}]: w: {}", cde.type, cde.name, cde.writable);
					if (cde.writable) {
						privileges.add(Privileges.WRITE);
						privileges.add(Privileges.WRITE_PROPS);
						privileges.add(Privileges.WRITE_CONTENT);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		if (rt == ResType.FREEBUSY || rt == ResType.SCHEDULE_INBOX) {
			privileges.add(Privileges.SCHEDULE_DELIVER);
			privileges.add(Privileges.SCHEDULE);
		}
		if (rt == ResType.SCHEDULE_OUTBOX) {
			privileges.add(Privileges.SCHEDULE_SEND);
			privileges.add(Privileges.SCHEDULE);
		}
		if (rt == ResType.ADDRESSBOOK || rt == ResType.VCARDS_CONTAINER) {
			privileges.add(Privileges.READ);
			privileges.add(Privileges.READ_FREE_BUSY);
			privileges.add(Privileges.UNLOCK);
			privileges.add(Privileges.READ_ACL);
			privileges.add(Privileges.READ_CU_PRIV_SET);
			privileges.add(Privileges.ALL);
			privileges.add(Privileges.WRITE);
			privileges.add(Privileges.WRITE_PROPS);
			privileges.add(Privileges.WRITE_CONTENT);
			privileges.add(Privileges.WRITE_ACL);
			privileges.add(Privileges.BIND);
			privileges.add(Privileges.UNBIND);

		}

		if (privileges.isEmpty()) {
			logger.warn("***** NO PRIVILEGES ON {}", dr.getPath());
		}
	}

	@Override
	public void expand(LoggedCore lc, DavResource dr, List<Property> scope) throws Exception {
		logger.info("expand");
	}

	@Override
	public void set(LoggedCore lc, DavResource dr, Element value) throws Exception {
		logger.info("[{}] set on {}", dr.getResType(), dr.getPath());
	}

}
