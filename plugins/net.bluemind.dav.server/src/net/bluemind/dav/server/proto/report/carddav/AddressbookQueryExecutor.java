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
import net.bluemind.addressbook.api.VCardInfo;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.report.IReportExecutor;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.dav.server.proto.report.carddav.AddressbookQueryQuery.Filter;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.SyncTokens;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;

public class AddressbookQueryExecutor implements IReportExecutor {

	private static final QName root = RDReports.ADDRESSBOOK_QUERY;
	private static final Logger logger = LoggerFactory.getLogger(AddressbookQueryExecutor.class);

	@Override
	public ReportResponse execute(LoggedCore lc, ReportQuery rq) {
		AddressbookQueryQuery cmq = (AddressbookQueryQuery) rq;
		DavStore ds = new DavStore(lc);
		DavResource dr = ds.from(cmq.getPath());

		ContainerDescriptor cd = lc.vStuffContainer(dr);
		VCardQuery query = new VCardQuery();
		for (Filter f : cmq.getFilters()) {
			f.update(query);
		}
		List<ItemValue<VCardInfo>> cards = new LinkedList<>();
		try {
			IAddressBook abApi = lc.getCore().instance(IAddressBook.class, cd.uid);
			ListResult<ItemValue<VCardInfo>> foundInfos = abApi.search(query);
			cards.addAll(foundInfos.values);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			cards = ImmutableList.of();
		}
		logger.info("Fetched {} cards(s) of container {}", cards.size(), cd.uid);
		IVCardService service = lc.getCore().instance(IVCardService.class, cd.uid);
		return new AddressbookQueryResponse(service, rq.getPath(), root, cards, cmq.getProps());
	}

	@Override
	public void write(ReportResponse rr, HttpServerResponse sr) {
		AddressbookQueryResponse cmr = (AddressbookQueryResponse) rr;

		MultiStatusBuilder msb = new MultiStatusBuilder();
		if (cmr.getCards() != null) {
			logger.info("Got {} distinct card UIDs", cmr.getCards().size());
			for (ItemValue<VCardInfo> card : cmr.getCards()) {
				String vcfPath = cmr.getHref() + card.uid + ".vcf";

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
		}
		msb.sendAs(sr);
	}

	@Override
	public QName getKind() {
		return root;
	}

}
