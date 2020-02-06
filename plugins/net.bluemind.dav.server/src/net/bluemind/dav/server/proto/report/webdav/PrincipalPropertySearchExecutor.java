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

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import io.vertx.core.http.HttpServerResponse;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.Proxy;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.report.IReportExecutor;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;
import net.bluemind.user.api.User;

public class PrincipalPropertySearchExecutor implements IReportExecutor {

	private static final QName root = new QName(NS.WEBDAV, "principal-property-search");

	private static final Logger logger = LoggerFactory.getLogger(PrincipalPropertySearchExecutor.class);

	@Override
	public ReportResponse execute(LoggedCore lc, ReportQuery rq) {
		PrincipalPropertySearchQuery ppsq = (PrincipalPropertySearchQuery) rq;
		PrincipalPropertySearchResponse ppsr = new PrincipalPropertySearchResponse(rq.getPath(), root);
		try {
			List<ItemValue<net.bluemind.user.api.User>> users = new LinkedList<>();
			// FIXME do the search
			ppsr.setUsers(users);
			ppsr.setExpected(ppsq.getExpectedResults());
			logger.info("Got {} users.", users.size());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return ppsr;
	}

	@Override
	public void write(ReportResponse rr, HttpServerResponse sr) {
		PrincipalPropertySearchResponse ppsr = (PrincipalPropertySearchResponse) rr;
		MultiStatusBuilder msb = new MultiStatusBuilder();
		for (ItemValue<User> u : ppsr.getUsers()) {
			String princ = Proxy.path + "/principals/__uids__/" + u.uid + "/";
			Element prop = msb.newResponse(princ, 200);
			for (QName qn : ppsr.getExpected()) {
				Element pe = DOMUtils.createElement(prop, NS.prefix(qn.getNamespaceURI()) + ":" + qn.getLocalPart());
				VCard card = u.value.contactInfos;
				switch (qn.getLocalPart()) {
				case "last-name":
					pe.setTextContent(card.identification.name.familyNames);
					break;
				case "first-name":
					pe.setTextContent(card.identification.name.givenNames);
					break;
				case "displayname":
					pe.setTextContent(card.identification.formatedName.value);
					break;
				case "principal-URL":
					DOMUtils.createElementAndText(pe, "d:href", princ);
					break;
				case "email-address-set":
					DOMUtils.createElementAndText(pe, "cso:email-address", card.defaultMail());
					break;
				case "calendar-user-address-set":
					DOMUtils.createElementAndText(pe, "d:href", princ);
					DOMUtils.createElementAndText(pe, "d:href", "mailto:" + card.defaultMail());
					DOMUtils.createElementAndText(pe, "d:href", "urn:uuid:" + u.uid);
					break;
				case "record-type":
					pe.setTextContent("users");
					break;
				case "calendar-user-type":
					pe.setTextContent("INDIVIDUAL");
					break;
				default:
					logger.warn("Unsupported prop: {}", qn);
				}
			}
		}
		msb.sendAs(sr);
	}

	@Override
	public QName getKind() {
		return root;
	}

}
