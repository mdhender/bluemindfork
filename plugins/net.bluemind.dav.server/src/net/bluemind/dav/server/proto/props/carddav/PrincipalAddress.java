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
package net.bluemind.dav.server.proto.props.carddav;

import javax.xml.namespace.QName;

import com.google.common.collect.ImmutableList;

import net.bluemind.dav.server.Proxy;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.QN;
import net.bluemind.dav.server.proto.props.HrefSet;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;

public class PrincipalAddress extends HrefSet {

	public static final QName NAME = QN.qn(NS.CARDDAV, "principal-address");

	public PrincipalAddress() {
		super(NAME);
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new PrincipalAddress();
			}
		};
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		hrefs = ImmutableList.of(Proxy.path + "/addressbooks/__uids__/" + dr.getUid() + "/" + "addressbook_"
				+ lc.getDomain() + "," + lc.getUser().uid + ".vcf");
	}

}
