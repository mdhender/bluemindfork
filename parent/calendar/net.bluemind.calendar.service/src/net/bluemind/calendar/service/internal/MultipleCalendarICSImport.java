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
package net.bluemind.calendar.service.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.internal.IInternalCalendar;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.utils.MappedFileStream;
import net.bluemind.icalendar.parser.CalendarOwner;
import net.bluemind.tag.api.TagRef;

public class MultipleCalendarICSImport extends ICSImportTask {

	private final Stream ics;
	private final List<TagRef> allTags;
	private static final int expectedMaxSize = 200 * 1024 * 1024;

	public MultipleCalendarICSImport(IInternalCalendar calendar, Stream ics, Optional<CalendarOwner> owner,
			List<TagRef> allTags, Mode mode) {
		super(calendar, owner, mode);
		this.ics = ics;
		this.allTags = allTags;
	}

	@Override
	protected void convertToVEventList(Consumer<ItemValue<VEventSeries>> consumer) {
		CompletableFuture<ByteBuf> mappedFile = MappedFileStream.createMappedFile(ics, expectedMaxSize);
		try {
			List<ByteBuf> calendars = splitCalendars(mappedFile.get(2, TimeUnit.MINUTES));
			importEvents(consumer, calendars);
		} catch (Exception e) {
			throw new ServerFault("Error while streaming ics to temporary file", e);
		}
	}

	private List<ByteBuf> splitCalendars(ByteBuf mappedfile) {
		int start;
		ByteBuf buffer = mappedfile;
		List<ByteBuf> slices = new ArrayList<>();
		String endCalendar = "\nEND:VCALENDAR";
		String startLookup = "BEGIN:VCALENDAR";
		while ((start = ByteBufUtil.indexOf(Unpooled.wrappedBuffer(startLookup.getBytes()), buffer)) != -1) {
			startLookup = "\nBEGIN:VCALENDAR";
			int end = ByteBufUtil.indexOf(Unpooled.wrappedBuffer(endCalendar.getBytes()), buffer);
			if (end == -1) {
				return slices;
			}
			end += endCalendar.length();
			slices.add(buffer.slice(start, end));
			int remaining = buffer.readableBytes() - end;
			buffer = buffer.slice(end, remaining);
		}
		return slices;
	}

	private void importEvents(Consumer<ItemValue<VEventSeries>> consumer, List<ByteBuf> slices) {
		for (ByteBuf calendar : slices) {
			try {
				try (InputStream in = new ByteBufInputStream(calendar, false)) {
					VEventServiceHelper.parseCalendar(in, owner, allTags, consumer);
				}
			} catch (IOException e) {
				throw new ServerFault(e);
			}
		}
	}

}
