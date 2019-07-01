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

import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.dav.server.store.SyncTokens;

public class GetETag implements IPropertyValue {

	private static final Logger logger = LoggerFactory.getLogger(GetETag.class);

	public static final QName NAME = new QName(NS.WEBDAV, "getetag");

	private String etag;

	@Override
	public QName getName() {
		return NAME;
	}

	@Override
	public void appendValue(Element parent) {
		parent.setTextContent(etag);
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new GetETag();
			}
		};
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) {
		long timestamp = 0;
		String p = dr.getPath();
		ResType rt = dr.getResType();
		if (rt == ResType.VSTUFF_CONTAINER) {
			timestamp = lc.getLastMod(dr);
			logger.info("{} using lastMod: {}", p, timestamp);
			dr.setEtag(SyncTokens.getEtag(p, timestamp));
		} else if (rt == ResType.VSTUFF) {
			logger.debug("**** on vevent *****");
		}
		etag = SyncTokens.getEtag(p, timestamp);
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
