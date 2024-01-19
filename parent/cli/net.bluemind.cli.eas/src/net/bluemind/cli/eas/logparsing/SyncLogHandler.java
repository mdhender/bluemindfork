/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cli.eas.logparsing;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import net.bluemind.cli.eas.logparsing.LogParser.SyncInfo;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.model.ItemValue;

public class SyncLogHandler implements ILogHandler {

	private final Map<String, SyncOperation> syncs;
	private final OutputOptions options;
	private final IContainersFlatHierarchy service;

	public SyncLogHandler(IContainersFlatHierarchy service, OutputOptions options) {
		this.syncs = new LinkedHashMap<>();
		this.options = options;
		this.service = service;
	}

	@Override
	public void syncRequest(String rid, String date, SyncInfo request) {
		syncs.put(rid, new SyncOperation(rid, date, request, null, null));
	}

	@Override
	public void syncResponse(String rid, SyncInfo response) {
		syncs.merge(rid, new SyncOperation(rid, null, null, response, null), (existingSync, newSync) -> {
			return new SyncOperation(rid, existingSync.date, existingSync.request, response, existingSync.code);
		});
	}

	@Override
	public void syncResponseProcessed(String rid, String code) {
		syncs.merge(rid, new SyncOperation(rid, null, null, null, code), (existingSync, newSync) -> {
			return new SyncOperation(rid, existingSync.date, existingSync.request, existingSync.response, code);
		});
	}

	@Override
	public String toString() {
		return syncs.entrySet().stream() //
				.map(sync -> String.format("Request-ID: %s: %n%s%n%n", sync.getKey(), sync.getValue().toString())) //
				.reduce("", String::concat);
	}

	@Override
	public String toTable() {

		List<SyncOperation> filtered = syncs.values().stream().filter(this::filter).toList();
		if (options.resolve == RESOLVE.LOOKUP) {
			filtered = filtered.stream().map(this::resolve).toList();
		}

		if (options.data() == DATA.INCLUDE) {
			return AsciiTable.getTable(filtered, Arrays.asList( //
					new Column().header("Summary").dataAlign(HorizontalAlign.LEFT).with(Object::toString), //
					new Column().header("RequestData").dataAlign(HorizontalAlign.LEFT).with(r -> r.request().xml()), //
					new Column().header("ResponseData").dataAlign(HorizontalAlign.LEFT).with(r -> {
						if (r.response() != null) {
							return r.response().xml();
						} else {
							return "";
						}
					})));
		} else {
			return AsciiTable.getTable(filtered, Arrays.asList( //
					new Column().header("Date").dataAlign(HorizontalAlign.LEFT).with(SyncOperation::date), //
					new Column().header("RID").dataAlign(HorizontalAlign.LEFT).with(SyncOperation::rid), //
					new Column().header("SyncKey").dataAlign(HorizontalAlign.LEFT).with(r -> r.request().syncKey()), //
					new Column().header("CollectionId").dataAlign(HorizontalAlign.LEFT)
							.with(r -> r.request().collectionId())));
		}
	}

	private SyncOperation resolve(SyncOperation operation) {
		long id = Long.parseLong(operation.request().collectionId().trim());
		ItemValue<ContainerHierarchyNode> node = service.getCompleteById(id);
		if (node == null) {
			return operation;
		}
		SyncInfo request = new SyncInfo(operation.request().syncKey(), id + "(" + node.value.name + ")",
				operation.request().xml());
		return new SyncOperation(operation.rid, operation.date, request, operation.response, operation.code);
	}

	private boolean filter(SyncOperation operation) {
		boolean ok = true;
		ok &= options.filter().typeFilter() == null
				|| operation.request().syncKey().contains(options.filter().typeFilter());
		ok &= options.filter().collectionFilter() == null
				|| operation.request().collectionId().equals(options.filter().collectionFilter());
		return ok;
	}

	private record SyncOperation(String rid, String date, SyncInfo request, SyncInfo response, String code) {

		@Override
		public String toString() {
			return String.format(
					"Date: %s%nRID: %s%nSyncKey: %s%nSyncRequestFrom: %s%nSyncResponseTo: %s%nCollectionId: %s%nResponseCode: %s",
					date, rid, request.syncKey(), toSyncDate(request()), toSyncDate(response()), request.collectionId(),
					code);
		}

		private String toSyncDate(SyncInfo syncInfo) {
			if (syncInfo == null || syncInfo.syncKey() == null) {
				return "";
			}

			String[] split = syncInfo.syncKey().split("-");
			String date = split[1];
			ZonedDateTime parsedDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(date)),
					ZoneId.of("Europe/Paris"));
			return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(parsedDate);
		}

	}

	public record OutputOptions(RESOLVE resolve, DATA data, FilterOptions filter) {
	}

	public record FilterOptions(String collectionFilter, String typeFilter) {
	}

	public enum DATA {
		INCLUDE, EXCLUDE
	}

	public enum RESOLVE {
		LOOKUP, NO
	}

}
