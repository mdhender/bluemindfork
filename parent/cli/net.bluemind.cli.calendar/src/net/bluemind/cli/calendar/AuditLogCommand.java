/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.cli.calendar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import io.netty.util.internal.StringUtil;
import io.vertx.core.json.JsonObject;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "log", description = "Pretty-Print an auditlog file")
public class AuditLogCommand implements ICmdLet, Runnable {

	private CliContext ctx;
	/**
	 * decompose auditlog based on
	 * {@link net.bluemind.core.auditlog.appender.slf4j.Slf4jEventAppender}
	 */
	private Set<String> filteredActionSet;

	@Parameters(paramLabel = "<file>", description = "Path to the auditlog file")
	public Path file;

	@Option(names = "--data", required = false, description = "Show data")
	public boolean data;

	@Option(names = "--show-ro", required = false, description = "Show read-only data")
	public boolean readOnly;

	@Option(names = "--data-by-date", required = false, description = "Show data only on a specific date (yyyy-MM-ddThh:mm:ss). Matching also works with substrings of this pattern")
	public String dataByDate;

	@Option(names = "--event-query", required = false, description = "Event query string (Columns Action and Event)")
	public String eventQuery;

	@Option(names = "--event-uid", required = false, description = "Event UID query string")
	public String eventUid;

	@Option(names = "--calendar-query", required = false, description = "Calendar query string (Column Calendar)")
	public String calendarQuery;

	@Option(names = "--filtered-actions", required = false, description = "Comma-separated list of actions which should not appear in the output")
	public String filteredActions;

	@Override
	public void run() {
		List<String> content;
		try {
			filteredActionSet = new HashSet<>();
			if (!StringUtil.isNullOrEmpty(filteredActions)) {
				filteredActionSet = new HashSet<>(Arrays.asList(filteredActions.split(",")).stream().map(s -> s.trim())
						.collect(Collectors.toList()));
			}
			content = Files.readAllLines(file);
		} catch (IOException e) {
			throw new CliException("Cannot read file " + file + ":" + e.getMessage());
		}

		Result output = process(content);
		ctx.info(output.content);
	}

	private Result process(List<String> content) {
		List<Tblrow> tblData = new ArrayList<>();
		AtomicInteger errors = new AtomicInteger(0);

		for (int i = 0; i < content.size(); i++) {
			String line = content.get(i);
			if (!line.trim().isEmpty()) {
				try {
					processLine(tblData, new JsonObject(line), errors);
				} catch (Exception e) {
					LoggerFactory.getLogger(this.getClass()).warn("Error while processing line {}", line, e);
				}
			}
		}

		String table = AsciiTable.getTable(tblData, Arrays.asList( //
				new Column().header("Info").dataAlign(HorizontalAlign.LEFT).with(r -> r.generalData()), //
				new Column().header("Event").dataAlign(HorizontalAlign.LEFT).with(r -> r.data), //
				new Column().header("Calendar").dataAlign(HorizontalAlign.LEFT).with(r -> r.entity) //
		));

		return new Result(table, errors.get());
	}

	private void processLine(List<Tblrow> tblData, JsonObject entry, AtomicInteger errorCount) {

		List<String> additionalData = new ArrayList<>();

		// filter by eventUid
		Optional<String> itemUid = getValue(entry, Arrays.asList("actionMeta", "item-uid"), "value", String.class);
		if (!matches(itemUid, eventUid)) {
			return;
		}

		// filter read-only entries
		Optional<Boolean> entryIsReadOnly = getValue(entry, Collections.emptyList(), "readOnly", Boolean.class);
		if (!readOnly && matches(entryIsReadOnly, "true")) {
			return;
		}

		// filter by action
		String action = getValue(entry, Collections.emptyList(), "action", String.class).get();
		if (filteredActionSet.contains(action)) {
			return;
		}

		if (action.equals("send-mail")) {
			additionalData.add(
					"mailto:" + getValue(entry, Arrays.asList("actionMeta", "mailTo"), "value", String.class).get());
		}

		Optional<String> sendNotif = getValue(entry, Arrays.asList("actionMeta", "sendNotif"), "value", String.class);
		sendNotif.ifPresent(n -> {
			if ("true".equalsIgnoreCase(n)) {
				additionalData.add("Send-Notification:" + n);
			}
		});

		getValue(entry, Arrays.asList("actionMeta", "smtp-response"), "value", String.class)
				.ifPresent(smtpResponse -> additionalData.add("SMTP response:" + smtpResponse));

		String date = getValue(entry, Collections.emptyList(), "timestamp", String.class).get();
		String actor = getValue(entry, Collections.emptyList(), "actor", String.class).get();

		boolean showData = data;

		if (!StringUtil.isNullOrEmpty(dataByDate) && date.startsWith(dataByDate)) {
			showData = true;
		}

		String actionData = "";

		Optional<String> error = getValue(entry, Collections.emptyList(), "error", String.class);
		String result = error.isPresent() ? "FAILED" : "SUCCESS";

		if (error.isPresent()) {
			actionData = error.get();
		} else {
			try {
				actionData = getActionData(action, entry);
			} catch (Exception e) {
				e.printStackTrace();
				actionData = "Undecodeable action data";
			}
		}

		// filter by eventQuery
		if (!StringUtil.isNullOrEmpty(eventQuery) && !actionData.contains(eventQuery)) {
			return;
		}

		String entity = getValue(entry, Arrays.asList("objectMeta"), "container-json", String.class).orElse("");

		if (!StringUtil.isNullOrEmpty(calendarQuery) && !entity.contains(calendarQuery)) {
			return;
		}

		tblData.add(new Tblrow(date, actor, itemUid.orElse(""), action, showData ? actionData : "", result, entity,
				additionalData));

	}

	private <T> Optional<T> getValue(JsonObject root, List<String> path, String key, Class<T> clazz) {
		JsonObject obj = root;
		for (String leaf : path) {
			if (!obj.containsKey(leaf)) {
				return Optional.empty();
			}
			obj = obj.getJsonObject(leaf);
		}
		Object valueAsObject = obj.getValue(key);
		if (valueAsObject == null) {
			return Optional.empty();
		}
		Object value;
		if (valueAsObject instanceof JsonObject) {
			value = ((JsonObject) valueAsObject).encodePrettily();
		} else {
			value = valueAsObject;
		}
		return Optional.ofNullable(clazz.cast(value));
	}

	private boolean matches(Optional<?> entryValue, String requestedValue) {
		if (StringUtil.isNullOrEmpty(requestedValue)) {
			return true;
		}
		if (!entryValue.isPresent()) {
			return false;
		}
		return requestedValue.equalsIgnoreCase(entryValue.get().toString());
	}

	private String getActionData(String action, JsonObject entry) {
		switch (action) {
		case "create":
		case "update":
			return getValue(entry, Arrays.asList("actionMeta"), "sanitized-value", String.class).get();
		case "updates":
			return getValue(entry, Arrays.asList("actionMeta"), "changes", String.class).get();
		default:
			return "";
		}

	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public class Tblrow {
		public final String date;
		public final String actor;
		public final String action;
		public final String data;
		public final String result;
		public final String entity;
		public final String itemUid;
		public final List<String> otherData;

		public Tblrow(String date, String actor, String itemUid, String action, String data, String result,
				String entity, List<String> otherData) {
			this.date = date;
			this.actor = actor;
			this.itemUid = itemUid;
			this.action = action;
			this.data = data;
			this.result = result;
			this.entity = entity;
			this.otherData = otherData;
		}

		public String generalData() {
			String toString = date + "\n" + actor + "\n" + action + "\n" + result + "\n" + itemUid + "\n";
			for (String s : otherData) {
				toString += (s + "\n");
			}
			return toString;
		}
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("calendar");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return AuditLogCommand.class;
		}

	}

	static class Result {
		final String content;
		final int errors;

		public Result(String content, int errors) {
			this.content = content;
			this.errors = errors;
		}

	}

}
