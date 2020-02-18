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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import io.netty.util.internal.StringUtil;
import io.vertx.core.json.JsonObject;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;

@Command(name = "log", description = "Pretty-Print an auditlog file")
public class AuditLogCommand implements ICmdLet, Runnable {

	private CliContext ctx;
	/**
	 * decompose auditlog based on
	 * {@link net.bluemind.core.auditlog.appender.slf4j.Slf4jEventAppender}
	 */
	private static final String format = "([^\\s]*)\\s([^\\s]*)\\s([^\\s]*)[^\\(]*\\(([^\\)]*)[^\\(]*\\(([^\\)]*)[^\\(]*\\(([^\\)]*)\\)\\s*(.*)";
	private static final Pattern pattern = Pattern.compile(format);
	// decompose member property
	// ("member\":\"\\\"addressbook_blue-mind.net/group_entity_46161\\\"\")
	private static final Pattern badMemberPattern = Pattern.compile("\"{2}([^\"]{1,})\"{2}");
	private Set<String> filteredActionSet;

	@Arguments(required = true, description = "Path to the auditlog file")
	public String file;

	@Option(name = "--data", required = false, description = "Show data")
	public boolean data;

	@Option(name = "--show-errors", required = false, description = "Show lines containing errors")
	public boolean showErrors;

	@Option(name = "--show-ro", required = false, description = "Show read-only data")
	public boolean readOnly;

	@Option(name = "--data-by-date", required = false, description = "Show data only on a specific date (yyyy-MM-dd hh:mm:ss). Matching also works with substrings of this pattern")
	public String dataByDate;

	@Option(name = "--event-query", required = false, description = "Event query string (Columns Action and Event)")
	public String eventQuery;

	@Option(name = "--event-uid", required = false, description = "Event UID query string")
	public String eventUid;

	@Option(name = "--calendar-query", required = false, description = "Calendar query string (Column Calendar)")
	public String calendarQuery;

	@Option(name = "--filtered-actions", required = false, description = "Comma-separated list of actions which should not appear in the output")
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
			content = Files.readAllLines(new File(file).toPath());
		} catch (IOException e) {
			throw new CliException("Cannot read file " + file + ":" + e.getMessage());
		}

		Result output = process(content);
		ctx.info(output.content);
		if (!showErrors) {
			ctx.info("Filtered " + output.errors + " erroneous lines");
		}
	}

	private Result process(List<String> content) {
		List<Tblrow> tblData = new ArrayList<>();
		boolean lastLineFailed = false;
		AtomicInteger errors = new AtomicInteger(0);

		for (int i = 0; i < content.size(); i++) {
			String line = content.get(i);
			String next = content.size() > (i + 1) ? content.get(i + 1) : "";
			if (!line.trim().isEmpty()) {
				lastLineFailed = processLine(tblData, line, next, lastLineFailed, errors);
			}
		}

		String table = AsciiTable.getTable(tblData, Arrays.asList( //
				new Column().header("Info").dataAlign(HorizontalAlign.LEFT).with(r -> r.generalData()), //
				new Column().header("Event").dataAlign(HorizontalAlign.LEFT).with(r -> r.data), //
				new Column().header("Calendar").dataAlign(HorizontalAlign.LEFT).with(r -> r.entity) //
		));

		return new Result(table, errors.get());
	}

	private boolean processLine(List<Tblrow> tblData, String line, String nextLine, boolean lastLineFailed,
			AtomicInteger errorCount) {
		// replace encoded \" and \\" in the json structure
		line = line.replace("\\\\\\\"", "");
		line = line.replace("\\\"", "\"");
		line = replaceMemberGroup(line);
		Matcher m = pattern.matcher(line);
		boolean success = true;
		if (!m.matches()) {
			if (!lastLineFailed) {
				errorCount.addAndGet(1);
				if (showErrors) {
					tblData.add(new Tblrow("", "", "", "", "", "Line does not match audit pattern: " + line, "", "",
							Collections.emptyList()));
				}
			}
			return true;
		} else {
			List<String> additionalData = new ArrayList<>();
			String actionPart = m.group(5);
			String type = m.group(3);
			String itemUid = "";
			Optional<String> itemParam = getParam(actionPart, "item-uid");
			if (itemParam.isPresent()) {
				itemUid = itemParam.get();
				if (!StringUtil.isNullOrEmpty(eventUid) && !itemUid.equals(eventUid)) {
					return success(type);
				}
			} else {
				if (!StringUtil.isNullOrEmpty(eventUid)) {
					return success(type);
				}
			}
			if (!readOnly && actionPart.contains("ro:true")) {
				return !success(type);
			}
			String action = getAction(actionPart);
			if (filteredActionSet.contains(action)) {
				return !success(type);
			}
			if (action.equals("send-mail")) {
				additionalData.add("mailto:" + getParam(actionPart, "mailTo").orElse(""));
			}
			getParam(actionPart, "sendNotif").ifPresent(n -> additionalData.add("Send-Notification:" + n));
			getParam(actionPart, "smtp-response")
					.ifPresent(smtpResponse -> additionalData.add("SMTP response:" + smtpResponse));

			String date = formatDate(m.group(1) + " " + m.group(2));
			String actor = formatActor(m.group(4));
			String actionData = "";
			boolean showData = data;

			if (!StringUtil.isNullOrEmpty(dataByDate) && date.startsWith(dataByDate)) {
				showData = true;
			}
			try {
				actionData = formatAction(action, actionPart);
			} catch (Exception e) {
				actionData = "Undecodeable action data";
			}
			if (!StringUtil.isNullOrEmpty(eventQuery) && !((actionPart + actionData).contains(eventQuery))) {
				return !success(type);
			}

			String result = m.group(7);
			String entity;
			try {
				entity = formatEntity(m.group(6));
			} catch (Exception e) {
				entity = "";
			}
			if (!StringUtil.isNullOrEmpty(calendarQuery) && !entity.contains(calendarQuery)) {
				return !success(type);
			}
			success = success(type);
			if (!success) {
				actionData = nextLine;
				showData = true;
			}
			tblData.add(new Tblrow(date, type, actor, itemUid, action, showData ? actionData : "", result, entity,
					additionalData));
		}
		return !success;
	}

	private String formatAction(String action, String actionPart) {
		switch (action) {
		case "create":
		case "update":
			return extractSanitizedValue(actionPart);
		case "updates":
			return extractChangesValue(actionPart);
		default:
			return "";
		}

	}

	private boolean success(String type) {
		return type.equals("INFO");
	}

	private String extractChangesValue(String actionPart) {
		return jsonPart(actionPart, "changes", 8, null, 0);
	}

	private String extractSanitizedValue(String actionPart) {
		return jsonPart(actionPart, "sanitized-value", 17, "sendNotif", 3);
	}

	private String jsonPart(String data, String indexBegin, int offsetBegin, String indexEnd, int negativeOffsetEnd) {
		String content = data.substring(data.indexOf(indexBegin) + offsetBegin);
		if (!StringUtil.isNullOrEmpty(indexEnd)) {
			content = content.substring(0, content.lastIndexOf(indexEnd) - negativeOffsetEnd);
		}
		JsonObject asJson = new JsonObject(content);
		return asJson.encodePrettily();
	}

	public static String replaceMemberGroup(String source) {
		Matcher m = badMemberPattern.matcher(source);
		if (!m.find()) {
			return source;
		}
		source = new StringBuilder(source).replace(m.start(), m.end(), "\"" + m.group(1) + "\"").toString();
		return replaceMemberGroup(source);
	}

	private String formatEntity(String data) {
		String payload = data.substring(data.indexOf("container-json=") + 16, data.length() - 2);
		return new JsonObject(payload).encodePrettily();
	}

	private String getAction(String action) {
		return action.substring("action:".length(), action.indexOf(","));
	}

	private String formatActor(String actor) {
		return actor.substring("actor:".length(), actor.indexOf(" "));
	}

	private String formatDate(String date) {
		return date.substring(0, date.lastIndexOf(","));
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public class Tblrow {
		public final String date;
		public final String type;
		public final String actor;
		public final String action;
		public final String data;
		public final String result;
		public final String entity;
		public final String itemUid;
		public final List<String> otherData;

		public Tblrow(String date, String type, String actor, String itemUid, String action, String data, String result,
				String entity, List<String> otherData) {
			this.date = date;
			this.type = type;
			this.actor = actor;
			this.itemUid = itemUid;
			this.action = action;
			this.data = data;
			this.result = result;
			this.entity = entity;
			this.otherData = otherData;
		}

		public String generalData() {
			String toString = date + "\n" + type + "\n" + actor + "\n" + action + "\n" + result + "\n" + itemUid + "\n";
			for (String s : otherData) {
				toString += (s + "\n");
			}
			return toString;
		}
	}

	private Optional<String> getParam(String actionPart, String param) {
		int index = actionPart.indexOf(param);
		if (index == -1) {
			return Optional.empty();
		}
		index += param.length() + 2;
		int end = actionPart.substring(index).indexOf("\"");

		return Optional.of(actionPart.substring(index, index + end));
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
