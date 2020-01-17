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
package net.bluemind.dav.server.proto.report.webdav;

import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableList;

import io.vertx.core.http.HttpServerResponse;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.report.IReportExecutor;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;

public class ExpandPropertyExecutor implements IReportExecutor {

	private static final QName root = new QName(NS.WEBDAV, "expand-property");
	private static final Logger logger = LoggerFactory.getLogger(ExpandPropertyExecutor.class);

	@Override
	public ReportResponse execute(LoggedCore lc, ReportQuery rq) {
		ExpandPropertyQuery epq = (ExpandPropertyQuery) rq;
		List<Property> props = epq.getProperties();
		DavStore ds = new DavStore(lc);
		DavResource dr = ds.from(rq.getPath());
		for (Property p : props) {
			IPropertyValue value = ds.getValue(p, dr);
			logger.info("Expand {} => {}", p.getQName().getLocalPart(), value);
			p.setValue(value);
		}

		ExpandPropertyResponse ret = new ExpandPropertyResponse(rq.getPath(), root);
		ret.setProperties(ImmutableList.copyOf(props));

		return ret;
	}

	@Override
	public void write(ReportResponse rr, HttpServerResponse sr) {
		ExpandPropertyResponse epr = (ExpandPropertyResponse) rr;
		MultiStatusBuilder msb = new MultiStatusBuilder();
		Element prop = msb.newResponse(epr.getHref(), 200);
		for (Property pr : epr.getProperties()) {
			QName qn = pr.getQName();
			// FIXME we send as if the property had no value...
			Element el = DOMUtils.createElement(prop, qn.getPrefix() + ":" + qn.getLocalPart());
			pr.getValue().appendValue(el);
		}
		msb.sendAs(sr);
	}

	@Override
	public QName getKind() {
		return root;
	}

}
