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
package net.bluemind.dav.server.proto.report.carddav;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableList;

import io.vertx.core.http.HttpServerResponse;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IVCardService;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.report.IReportExecutor;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.SyncTokens;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;

public class AddressbookMultigetExecutor implements IReportExecutor {

	private static final QName root = RDReports.ADDRESSBOOK_MULTIGET;
	private static final Logger logger = LoggerFactory.getLogger(AddressbookMultigetExecutor.class);

	@Override
	public ReportResponse execute(LoggedCore lc, ReportQuery rq) {
		AddressbookMultigetQuery cmq = (AddressbookMultigetQuery) rq;
		List<String> extIds = new ArrayList<>(cmq.getHrefs().size());
		DavStore ds = new DavStore(lc);
		DavResource dr = ds.from(cmq.getPath());
		ContainerDescriptor cd = lc.vStuffContainer(dr);
		for (String href : cmq.getHrefs()) {
			int lastDot = href.lastIndexOf('.');
			int lastSlash = href.lastIndexOf('/', lastDot);
			String extId = href.substring(lastSlash + 1, lastDot);

			try {
				extIds.add(URLDecoder.decode(extId, "UTF-8"));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

		if ("addressbook".equals(cd.type)) {
			List<ItemValue<VCard>> cards = new LinkedList<>();
			try {
				IAddressBook ab = lc.getCore().instance(IAddressBook.class, cd.uid);
				List<ItemValue<VCard>> multipleGet = ab.multipleGet(extIds);
				cards.addAll(multipleGet);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				cards = ImmutableList.of();
			}

			logger.info("Fetched {} cards(s) from {} extIds", cards.size(), extIds.size());
			return new AddressbookMultigetResponse(rq.getPath(), root, cards,
					lc.getCore().instance(IVCardService.class, cd.uid), cmq.getProps());
		} else {
			logger.error("Multiget unsupported on " + dr.getResType() + " " + dr.getPath());
			return null;
		}

	}

	@Override
	public void write(ReportResponse rr, HttpServerResponse sr) {
		AddressbookMultigetResponse cmr = (AddressbookMultigetResponse) rr;

		MultiStatusBuilder msb = new MultiStatusBuilder();
		for (ItemValue<VCard> card : cmr.getCards()) {

			String vcfPath = cmr.getHref() + card + ".vcf";
			Element propElem = msb.newResponse(vcfPath, 200);
			for (QName prop : cmr.getProps()) {
				Element pe = DOMUtils.createElement(propElem, prop.getPrefix() + ":" + prop.getLocalPart());
				switch (prop.getLocalPart()) {
				case "getetag":
					pe.setTextContent(SyncTokens.getEtag(vcfPath, card.version));
					break;
				case "address-data":
					pe.setTextContent(cmr.getIvCardService().exportCards(Arrays.asList(card.uid)));
					break;
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
