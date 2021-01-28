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
package net.bluemind.webmodules.contact.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.addressbook.api.IAddressBookPromise;
import net.bluemind.addressbook.api.IAddressBooksPromise;
import net.bluemind.addressbook.api.VCardInfo;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.api.IContainerManagementPromise;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.server.NeedVertx;

public class ExportVCardHandler implements Handler<HttpServerRequest>, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(ExportVCardHandler.class);
	private HttpClientProvider prov;

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	@Override
	public void handle(final HttpServerRequest request) {
		String containerUid = request.params().get("containerUid");
		final String tagUid = request.params().get("tagUid");

		MultiMap headers = request.headers();
		String sessionId = headers.get("BMSessionId");
		final VertxPromiseServiceProvider clientProvider = new VertxPromiseServiceProvider(prov, locator, sessionId);

		if (null != containerUid) {
			exportByContainer(request, containerUid, clientProvider);
		} else { // exporting tagged vcards
			exportByTag(request, tagUid, clientProvider);
		}
	}

	private void handleException(HttpServerRequest request) {
		HttpServerResponse resp = request.response();
		resp.setStatusCode(500);
		resp.end();
	}

	private void exportByTag(final HttpServerRequest request, final String tagUid,
			final VertxPromiseServiceProvider clientProvider) {
		VCardQuery query = VCardQuery.create("value.explanatory.categories.itemUid:" + tagUid);
		IAddressBooksPromise addressbooks = clientProvider.instance(IAddressBooksPromise.class);

		Map<String, List<String>> cards = new HashMap<>();
		CompletableFuture<ListResult<ItemContainerValue<VCardInfo>>> search = addressbooks.search(query);
		search.thenAccept(searchResult -> {
			searchResult.values.forEach(cardContainerValue -> {
				String container = cardContainerValue.containerUid;
				String uid = cardContainerValue.uid;
				List<String> cardUids = null;
				if (cards.containsKey(container)) {
					cardUids = cards.get(container);
				} else {
					cardUids = new ArrayList<>();
				}
				cardUids.add(uid);
				cards.put(container, cardUids);
			});
			prepareResponse(request, tagUid + ".vcf");
			stream(request, clientProvider, cards);
		});

		search.exceptionally(e -> {
			handleException(request);
			return null;
		});
	}

	private void exportByContainer(final HttpServerRequest request, String containerUid,
			final VertxPromiseServiceProvider clientProvider) {
		Map<String, List<String>> cards = new HashMap<>();
		IContainerManagementPromise container = clientProvider.instance(IContainerManagementPromise.class,
				containerUid);
		IAddressBookPromise addressbook = clientProvider.instance(IAddressBookPromise.class, containerUid);

		CompletableFuture<ContainerDescriptor> descriptor = container.getDescriptor();
		CompletableFuture<List<String>> uids = addressbook.allUids();

		CompletableFuture<Void> finalStage = uids.thenAcceptBoth(descriptor, (uidList, containerDescriptor) -> {
			prepareResponse(request, containerDescriptor.name + "_" + containerUid + ".vcf");
			cards.put(containerUid, uidList);
			stream(request, clientProvider, cards);
		});

		finalStage.exceptionally(e -> {
			handleException(request);
			return null;
		});

	}

	private void stream(final HttpServerRequest request, final IServiceProvider clientProvider,
			Map<String, List<String>> cards) {
		if (logger.isDebugEnabled()) {
			for (String container : cards.keySet()) {
				logger.debug("exporting container {} containing {} vcards", container, cards.get(container).size());
			}
		}
		VCardStream vcardStream = new VCardStream(clientProvider, cards);
		vcardStream.pipeTo(request.response());
	}

	private void prepareResponse(final HttpServerRequest request, String filename) {
		request.response().headers().add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		request.response().setChunked(true).setStatusCode(200);
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.prov = new HttpClientProvider(vertx);

	}
}
