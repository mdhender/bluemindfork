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
package net.bluemind.eas.command.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.w3c.dom.Document;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.search.Range;
import net.bluemind.eas.dto.search.SearchRequest;
import net.bluemind.eas.dto.search.SearchResponse;
import net.bluemind.eas.dto.search.SearchResponse.Status;
import net.bluemind.eas.dto.search.SearchResult;
import net.bluemind.eas.dto.search.StoreName;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.search.ISearchSource;
import net.bluemind.eas.search.ISearchSource.Results;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.search.SearchRequestParser;
import net.bluemind.eas.serdes.search.SearchResponseFormatter;
import net.bluemind.eas.utils.RunnableExtensionLoader;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class SearchProtocol implements IEasProtocol<SearchRequest, SearchResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SearchProtocol.class);
	private Map<StoreName, Set<ISearchSource>> sources;

	public SearchProtocol() {
		sources = new HashMap<StoreName, Set<ISearchSource>>();
		registerSources();
	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<SearchRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Parsing *******");
		}
		SearchRequestParser parser = new SearchRequestParser();
		SearchRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, SearchRequest query, Handler<SearchResponse> responseHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Executing *******");
		}

		SearchResponse response = new SearchResponse();
		if (query.store.name == null) {
			logger.error("Invalid store name");

			response.status = Status.ServerError;
			responseHandler.handle(response);
			return;
		}

		Results<SearchResult> searchResult = search(bs, query);

		response.status = Status.Success;
		response.store = new SearchResponse.Store();
		response.store.status = SearchResponse.Store.Status.Success;
		response.store.results = searchResult;
		response.total = searchResult.getNumFound();
		response.range = Range.create(query.store.options.range.min,
				(query.store.options.range.min + response.total.intValue() - 1));

		responseHandler.handle(response);

	}

	private void registerSources() {
		RunnableExtensionLoader<ISearchSource> rel = new RunnableExtensionLoader<ISearchSource>();
		List<ISearchSource> bs = rel.loadExtensions("net.bluemind.eas", "search", "search", "implementation");
		for (ISearchSource ibs : bs) {
			addRegisterSource(ibs.getStoreName(), ibs);
		}
	}

	private void addRegisterSource(StoreName key, ISearchSource value) {
		Set<ISearchSource> set = sources.get(key);
		if (set == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Add " + value.getClass().getName() + " in search sources for store " + key);
			}
			set = new HashSet<ISearchSource>();
			this.sources.put(key, set);
		}
		set.add(value);
	}

	public Results<SearchResult> search(BackendSession bs, SearchRequest request) {
		Results<SearchResult> ret = new Results<SearchResult>();
		for (ISearchSource source : sources.get(request.store.name)) {
			Results<SearchResult> rlist = source.search(bs, request);
			ret.setNumFound(rlist.getNumFound());
			ret.addAll(rlist);
		}
		return ret;
	}

	@Override
	public void write(BackendSession bs, Responder responder, SearchResponse response, final Handler<Void> completion) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Writing *******");
		}

		SearchResponseFormatter formatter = new SearchResponseFormatter();
		IResponseBuilder builder = new WbxmlResponseBuilder(bs.getLoginAtDomain(), responder.asOutput());
		formatter.format(builder, bs.getProtocolVersion(), response, new Callback<Void>() {

			@Override
			public void onResult(Void data) {
				completion.handle(null);
			}
		});
	}

	@Override
	public String address() {
		return "eas.protocol.search";
	}

}
