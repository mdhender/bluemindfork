/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cli.calendar;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import com.google.common.base.Strings;

import net.bluemind.calendar.api.CalendarsVEventQuery;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendars;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.utils.JsonUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "list-event-details-by-organizer", description = "List event details")
public class ListEventDetailsCommand implements ICmdLet, Runnable {

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(names = "--domain", required = true, description = "domain")
	public String domain;

	@Option(names = "--query", required = true, description = "query")
	public String query;

	@Option(names = "--dateMin", required = false, description = "yyyy-MM-dd")
	public String dateMin;

	@Option(names = "--dateMax", required = false, description = "yyyy-MM-dd")
	public String dateMax;

	@Parameters(paramLabel = "<email>", description = "Organizer email")
	public String organizerEmail;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("calendar");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ListEventDetailsCommand.class;
		}
	}

	@Override
	public void run() {
		if (Strings.isNullOrEmpty(organizerEmail)) {
			ctx.error("Organizer email is required");
			System.exit(1);
		}

		IContainers containerService = ctx.adminApi().instance(IContainers.class);
		List<ContainerDescriptor> calendars = containerService
				.all(ContainerQuery.ownerAndType(cliUtils.getUserUidByEmail(organizerEmail), ICalendarUids.TYPE));

		ICalendars calendarService = ctx.adminApi().instance(ICalendars.class);
		VEventQuery esQuery = VEventQuery.create(query);
		if (dateMin != null) {
			esQuery.dateMin = new BmDateTime(dateMin, null, Precision.Date);
		}
		if (dateMax != null) {
			esQuery.dateMax = new BmDateTime(dateMax, null, Precision.Date);
		}

		CalendarsVEventQuery calQuery = new CalendarsVEventQuery();
		calQuery.containers = calendars.stream().map(con -> con.uid).toList();
		calQuery.eventQuery = esQuery;

		List<ItemContainerValue<VEventSeries>> results = calendarService.search(calQuery).stream()
				.filter(ret -> ret.value.main != null && ret.value.main.organizer != null
						&& ret.value.main.organizer.mailto != null
						&& ret.value.main.organizer.mailto.equals(organizerEmail))
				.toList();

		ctx.info("Found {} matching events", results.size());

		String table = AsciiTable.getTable(results, Arrays.asList( //
				new Column().header("Calendar").maxWidth(50).dataAlign(HorizontalAlign.LEFT)
						.with(ret -> ret.containerUid), //
				new Column().header("ICS-UID").maxWidth(50).dataAlign(HorizontalAlign.LEFT)
						.with(ret -> ret.value.icsUid), //
				new Column().header("Summary").maxWidth(70).dataAlign(HorizontalAlign.LEFT).with(this::summary), //
				new Column().header("Start").maxWidth(100).dataAlign(HorizontalAlign.LEFT).with(this::start), //
				new Column().header("End").maxWidth(100).dataAlign(HorizontalAlign.LEFT).with(this::end), //
				new Column().header("RRULE").maxWidth(50).dataAlign(HorizontalAlign.LEFT).with(this::rrule)));
		ctx.info(table);

	}

	private String start(ItemContainerValue<VEventSeries> ret) {
		return ret.value.main.dtstart.iso8601;
	}

	private String end(ItemContainerValue<VEventSeries> ret) {
		return ret.value.main.dtend.iso8601;
	}

	private String summary(ItemContainerValue<VEventSeries> ret) {
		return ret.value.main.summary;
	}

	private String rrule(ItemContainerValue<VEventSeries> ret) {
		return ret.value.main.rrule == null ? "" : JsonUtils.asString(ret.value.main.rrule);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}
}
