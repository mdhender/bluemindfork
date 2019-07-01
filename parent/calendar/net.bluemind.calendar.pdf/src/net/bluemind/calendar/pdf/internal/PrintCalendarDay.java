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
package net.bluemind.calendar.pdf.internal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Element;

import net.bluemind.calendar.api.PrintOptions;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.utils.DateTimeComparator;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;

public class PrintCalendarDay extends PrintCalendar {

	private int alldayStartGrid;
	private Map<Integer, Float> hoursYCoords;
	private float rowHeight;
	private int days;
	private int dayWidth;
	private Map<Long, List<ItemContainerValue<VEvent>>> ocs;

	private float workHourStart;
	private float workHourEnd;

	private float printHourStart;
	private float printHourEnd;
	private Calendar periodStart;
	private Calendar periodEnd;

	public PrintCalendarDay(PrintContext context, PrintOptions options, List<ItemContainerValue<VEvent>> vevents,
			int days) throws ServerFault {
		super(context, options);

		hoursYCoords = new HashMap<Integer, Float>();
		rowHeight = LINE_HEIGHT;
		addPage();
		this.days = days;
		dayWidth = (pageWidth - 4 * MARGIN) / days;

		Map<String, String> settings = context.userSettings;

		workHourStart = Float.parseFloat(settings.get("work_hours_start"));
		workHourEnd = Float.parseFloat(settings.get("work_hours_end"));

		ocs = sortOccurrences(vevents, options.dateBegin, options.dateEnd);

		printHourStart = Math.min(workHourStart, evtHourStart);
		printHourEnd = Math.max(workHourEnd, evtHourEnd);

		periodStart = Calendar.getInstance(timezone);
		periodStart.setTimeInMillis(new BmDateTimeWrapper(options.dateBegin).toTimestamp(timezone.getID()));

		periodEnd = Calendar.getInstance(timezone);
		periodEnd.setTimeInMillis(new BmDateTimeWrapper(options.dateEnd).toTimestamp(timezone.getID()));
	}

	private void setGrid(int allDay) {

		int contentHeight = pageHeight - y - MARGIN - LINE_HEIGHT;
		int contentWidth = pageWidth - 2 * MARGIN;
		int hourLabelWidth = MARGIN * 2;
		int colHeight = pageHeight - MARGIN - LINE_HEIGHT;

		Element rect = page.createElement("rect");
		rect.setAttribute("x", Integer.toString(MARGIN));
		rect.setAttribute("y", Integer.toString(y));
		rect.setAttribute("width", Integer.toString(contentWidth));
		rect.setAttribute("height", Integer.toString(contentHeight));
		rect.setAttribute("style", STYLE_GRID);
		root.appendChild(rect);

		Element hourSeparator = page.createElement("line");
		hourSeparator.setAttribute("x1", Integer.toString(hourLabelWidth + MARGIN));
		hourSeparator.setAttribute("x2", Integer.toString(hourLabelWidth + MARGIN));
		hourSeparator.setAttribute("y1", Integer.toString(y));
		hourSeparator.setAttribute("y2", Integer.toString(colHeight));
		hourSeparator.setAttribute("style", STYLE_GRID);
		root.appendChild(hourSeparator);

		// days columns
		Element daySeparator;
		int dayWidth = (contentWidth - MARGIN * 2) / days;
		int dStart = hourLabelWidth + MARGIN;
		String x;
		SimpleDateFormat sdf = new SimpleDateFormat("EEE dd", l);
		Calendar start = Calendar.getInstance(timezone);
		start.setTimeInMillis(new BmDateTimeWrapper(options.dateBegin).toTimestamp(timezone.getID()));
		start.set(Calendar.HOUR_OF_DAY, 12);

		Element dayLabel;
		for (int i = 0; i < days; i++) {
			dayLabel = page.createElement("text");
			dayLabel.setAttribute("x", Integer.toString(dStart + i * dayWidth + dayWidth / 2));
			dayLabel.setAttribute("y", Integer.toString(y - 3));
			dayLabel.setAttribute("text-anchor", "middle");
			NodeUtils.setText(dayLabel, sdf.format(start.getTime()));
			root.appendChild(dayLabel);

			daySeparator = page.createElement("line");
			x = Integer.toString(dStart + i * dayWidth);
			daySeparator.setAttribute("x1", x);
			daySeparator.setAttribute("x2", x);
			daySeparator.setAttribute("y1", Integer.toString(y));
			daySeparator.setAttribute("y2", Integer.toString(colHeight));
			daySeparator.setAttribute("style", STYLE_GRID);
			root.appendChild(daySeparator);

			start.add(Calendar.DATE, 1);
		}

		y += allDay * (LINE_HEIGHT + 2) + 1;

		Element h;
		Element txt;

		// Hours rows
		int delta = (int) (Math.ceil(printHourEnd) - Math.floor(printHourStart));
		float nbRows = delta * 2f;
		rowHeight = (pageHeight - y - MARGIN - LINE_HEIGHT) / nbRows;

		for (int i = 0; i < nbRows; i++) {
			h = page.createElement("line");

			float yy = y + i * rowHeight;

			h.setAttribute("x2", Integer.toString(pageWidth - MARGIN));
			h.setAttribute("y1", Float.toString(yy));
			h.setAttribute("y2", Float.toString(yy));

			if (i % 2 == 0) {
				float hy = yy + rowHeight;
				h.setAttribute("x1", Integer.toString(MARGIN));
				h.setAttribute("style", STYLE_GRID);
				txt = page.createElement("text");
				txt.setAttribute("x", Integer.toString(MARGIN + 3));
				txt.setAttribute("y", Float.toString(hy));
				int hod = i + (int) (printHourStart * 2);
				NodeUtils.setText(txt, hod / 2 + ":00");
				root.appendChild(txt);
				hoursYCoords.put(hod / 2, yy);
			} else {
				h.setAttribute("x1", Integer.toString(hourLabelWidth + MARGIN));
				h.setAttribute("style", STYLE_GRID_DASHED);
			}
			root.appendChild(h);
		}
	}

	@Override
	public void process() throws ServerFault {
		addVEvents();
	}

	/**
	 * 
	 */
	private void addVEvents() {
		alldayStartGrid = y;

		Map<Integer, List<ItemContainerValue<VEvent>>> indays = new HashMap<>();
		Map<Integer, List<ItemContainerValue<VEvent>>> alldays = new TreeMap<>();
		Map<Integer, Integer> alldaysCount = new HashMap<Integer, Integer>();

		Calendar dtstart = Calendar.getInstance(timezone);
		Calendar dtend;
		for (Long key : ocs.keySet()) {
			for (ItemContainerValue<VEvent> item : ocs.get(key)) {
				VEvent e = item.value;
				dtstart.setTimeInMillis(new BmDateTimeWrapper(e.dtstart).toTimestamp(timezone.getID()));
				dtend = (Calendar) dtstart.clone();
				dtend.setTimeInMillis(new BmDateTimeWrapper(e.dtend).toTimestamp(timezone.getID()));

				if (dtstart.compareTo(periodStart) < 0) {
					dtstart.set(Calendar.YEAR, periodStart.get(Calendar.YEAR));
					dtstart.set(Calendar.MONTH, periodStart.get(Calendar.MONTH));
					dtstart.set(Calendar.DATE, periodStart.get(Calendar.DATE));
					e.dtstart = BmDateTimeWrapper.fromTimestamp(dtstart.getTimeInMillis(), timezone.getID(),
							Precision.Date);

				}
				if (dtend.compareTo(periodEnd) > 0) {
					dtend.set(Calendar.YEAR, periodEnd.get(Calendar.YEAR));
					dtend.set(Calendar.MONTH, periodEnd.get(Calendar.MONTH));
					dtend.set(Calendar.DATE, periodEnd.get(Calendar.DATE));
					e.dtend = BmDateTimeWrapper.fromTimestamp(dtend.getTimeInMillis(), timezone.getID(),
							Precision.Date);
				}

				if (dtstart.compareTo(dtend) == 0) {
					dtend.add(Calendar.DATE, 1);
					e.dtend = BmDateTimeWrapper.fromTimestamp(dtend.getTimeInMillis(), timezone.getID(),
							e.dtend.precision);
				}

				if (e.allDay() || dtstart.get(Calendar.DATE) != dtend.get(Calendar.DATE)) {
					registerAllDayEvent(dtstart, dtend, alldays, item, alldaysCount);
				} else {
					int d = dtstart.get(Calendar.DAY_OF_WEEK);
					if (!indays.containsKey(d)) {
						List<ItemContainerValue<VEvent>> le = new ArrayList<>();
						indays.put(d, le);
					}
					indays.get(d).add(item);
				}
			}
		}

		int c = 0;
		for (Integer k : alldaysCount.keySet())

		{
			c = Math.max(c, alldaysCount.get(k));
		}

		setGrid(c);
		addAllDayEvent(alldays);
		addIndayEvent(indays);
	}

	/**
	 * @param dtstart
	 * @param dtend
	 * @param adays
	 * @param o
	 * @param alldaysCount
	 */
	private void registerAllDayEvent(Calendar dtstart, Calendar dtend,
			Map<Integer, List<ItemContainerValue<VEvent>>> adays, ItemContainerValue<VEvent> o,
			Map<Integer, Integer> alldaysCount) {

		int d = getRelativeDayOfWeeek(dtstart);

		if (!adays.containsKey(d)) {
			List<ItemContainerValue<VEvent>> le = new ArrayList<>();
			adays.put(d, le);
		}
		adays.get(d).add(o);

		while (dtstart.compareTo(dtend) < 0) {
			d = getRelativeDayOfWeeek(dtstart);
			if (!alldaysCount.containsKey(d)) {
				alldaysCount.put(d, 1);
			} else {
				alldaysCount.put(d, alldaysCount.get(d) + 1);
			}

			dtstart.add(Calendar.DATE, 1);
		}
	}

	private void addAllDayEvent(Map<Integer, List<ItemContainerValue<VEvent>>> adays) {
		List<ItemContainerValue<VEvent>> alldays;
		int posX;
		Map<Integer, Map<Integer, String>> fixedPosY = new HashMap<Integer, Map<Integer, String>>();
		Calendar dtstart = Calendar.getInstance(timezone);
		Calendar dtend = (Calendar) dtstart.clone();
		for (Integer key : adays.keySet()) {
			alldays = adays.get(key);

			Collections.sort(alldays, new EventComparator(timezone.getID()));
			for (ItemContainerValue<VEvent> evt : alldays) {
				VEvent o = evt.value;
				dtstart.setTimeInMillis(new BmDateTimeWrapper(o.dtstart).toTimestamp(timezone.getID()));
				dtend.setTimeInMillis(new BmDateTimeWrapper(o.dtend).toTimestamp(timezone.getID()));

				if (dtend.get(Calendar.HOUR_OF_DAY) < dtstart.get(Calendar.HOUR_OF_DAY)) {
					dtend.set(Calendar.HOUR_OF_DAY, dtstart.get(Calendar.HOUR_OF_DAY));
				}

				posX = MARGIN * 3 + key * dayWidth;
				if (days == 1) {
					posX = MARGIN * 3;
				}

				int idx = 0;

				if (fixedPosY.containsKey(key)) {
					Map<Integer, String> list = fixedPosY.get(key);
					while (list.containsKey(idx)) {
						idx++;
					}
				}

				int posY = alldayStartGrid + idx * (LINE_HEIGHT + 2) + 1;

				int pos = 0;
				int size = 0;

				while (dtstart.compareTo(dtend) < 0) {
					int d = getRelativeDayOfWeeek(dtstart);
					if (fixedPosY.containsKey(d)) {
						Map<Integer, String> list = fixedPosY.get(d);
						if (pos == 0) {
							while (list.containsKey(pos)) {
								pos++;
							}
						}
						list.put(pos, o.summary);
					} else {
						Map<Integer, String> list = new HashMap<Integer, String>();
						list.put(idx, o.summary);
						fixedPosY.put(d, list);
					}
					size++;
					dtstart.add(Calendar.DATE, 1);
				}

				Element rect = page.createElement("rect");
				rect.setAttribute("x", Integer.toString(posX));
				rect.setAttribute("y", Integer.toString(posY));
				rect.setAttribute("width", Integer.toString(size * dayWidth));
				rect.setAttribute("height", Integer.toString(LINE_HEIGHT));

				PrintedEvent pe = new PrintedEvent();
				pe.event = o;
				pe.id = evt.uid + "_" + dtstart.getTimeInMillis() + "_" + evt.containerUid;
				pe.dtstart = (Calendar) dtstart.clone();
				pe.dtend = (Calendar) dtend.clone();
				pe.calendarId = evt.containerUid;
				pe.part = getPart(o);

				setEventClass(rect, pe);

				Element text = page.createElement("text");
				if (o.allDay()) {
					NodeUtils.setText(text, getTitle(o));
				} else {
					NodeUtils.setText(text, timeFormat.format(dtstart.getTime()) + " - " + getTitle(o));
				}
				text.setAttribute("x", Integer.toString(posX + 3));
				text.setAttribute("y", Integer.toString(posY + LINE_HEIGHT - 3));
				text.setAttribute("style", getEventTitleStyle(pe.calendarId, pe.part));

				addLocation(evt.containerUid, evt.value, text);

				root.appendChild(rect);
				root.appendChild(text);

				posY += LINE_HEIGHT + 2;
			}
		}
	}

	private void addIndayEvent(Map<Integer, List<ItemContainerValue<VEvent>>> indaysPerDay) {
		for (Integer d : indaysPerDay.keySet()) {
			addIndayEvent(indaysPerDay.get(d));
		}
	}

	private void addIndayEvent(List<ItemContainerValue<VEvent>> occurrences) {

		Calendar dtstart = Calendar.getInstance(timezone);
		Calendar dtend = (Calendar) dtstart.clone();
		Calendar current;

		ListEvents ec = new ListEvents();

		// register events
		String calendarId;
		for (ItemContainerValue<VEvent> item : occurrences) {
			VEvent e = item.value;
			dtstart.setTimeInMillis(new BmDateTimeWrapper(e.dtstart).toTimestamp(timezone.getID()));

			dtend.setTimeInMillis(new BmDateTimeWrapper(e.dtend).toTimestamp(timezone.getID()));

			calendarId = item.containerUid;

			current = (Calendar) dtstart.clone();
			PrintedEvent pe = new PrintedEvent();
			pe.event = e;
			pe.id = item.uid + "_" + dtstart.getTimeInMillis() + "_" + calendarId;
			pe.dtstart = (Calendar) dtstart.clone();
			pe.dtend = (Calendar) dtend.clone();
			pe.calendarId = calendarId;
			pe.part = getPart(e);

			while (current.compareTo(dtend) < 0) {
				Float coord = hoursYCoords.get(current.get(Calendar.HOUR_OF_DAY));

				if (coord != null) {
					float cell = (float) (coord + (current.get(Calendar.MINUTE) / 30d) * rowHeight);
					ec.addEvent(cell, pe);
				}
				current.add(Calendar.MINUTE, 30);
			}
		}

		HashMap<String, PrintedEvent> updated = new HashMap<String, PrintedEvent>();
		int unit = 0;

		TreeSet<Float> keys = new TreeSet<Float>(new ArrayList<Float>(ec.getList().keySet()));

		for (Float cell : keys) {
			ArrayList<PrintedEvent> cellContent = ec.getList().get(cell);
			int cellLength = cellContent.size();

			Collections.sort(cellContent, new CellItemComparator(timezone.getID()));

			HashMap<Integer, Boolean> usedPosition = new HashMap<Integer, Boolean>();
			int position = 0;

			for (PrintedEvent pe : cellContent) {
				int idx = cellContent.indexOf(pe);
				String id = pe.id;
				PrintedEvent cur = pe;

				if (!updated.containsKey(id)) {

					if (usedPosition.isEmpty()) {
						unit = 1;
					}

					while (usedPosition.containsKey(position)) {
						position++;
					}

					int end = position;
					if ((idx + 1) == cellLength) {
						while (end < unit && !usedPosition.containsKey(end)) {
							end++;
						}
					}

					cur.position = position;
					cur.end = end;
					cur.unit = unit;

					updated.put(id, cur);
				} else {
					cur = updated.get(id);
				}

				usedPosition.put(cur.position, true);

				if (cellLength > cur.unit) {
					unit = cellLength;
					cur.unit = cellLength;
				}

				if ((cur.position + 1) < cellLength && (idx + 1) < cellLength) {
					cur.end = cur.position + 1;
				}

			}
		}

		// Print event
		PrintedEvent pe;
		for (String id : updated.keySet()) {
			pe = updated.get(id);
			pe.size = pe.end - pe.position;
			printEvent(pe);
		}

	}

	private class CellItemComparator implements Comparator<PrintedEvent> {
		private final String timezone;

		public CellItemComparator(String timezone) {
			this.timezone = timezone;
		}

		public int compare(PrintedEvent pe1, PrintedEvent pe2) {
			VEvent e1 = pe1.event;
			VEvent e2 = pe2.event;

			int d = new DateTimeComparator(timezone).compare(e1.dtstart, e2.dtstart);
			if (d != 0)
				return d;
			d = (int) (duration(e2) - duration(e1));
			if (d != 0)
				return d;

			return 0;
		}
	}

	private void printEvent(PrintedEvent pe) {
		int day;
		if (days == 1) {
			day = 0;
		} else {
			day = getRelativeDayOfWeeek(pe.dtstart);
		}

		float y = (float) (hoursYCoords.get(pe.dtstart.get(Calendar.HOUR_OF_DAY))
				+ pe.dtstart.get(Calendar.MINUTE) / 30d * rowHeight + 1);

		int width = (dayWidth / pe.unit);
		int x = MARGIN * 3 + (width * pe.position) + 1 + day * dayWidth;
		float height = (float) (rowHeight * (duration(pe.event) / 1800d) - 2);

		if (height <= rowHeight) {
			height = rowHeight;
		}

		Element g = page.createElement("g");
		Element rect = page.createElement("rect");

		int rectWidth = width * pe.size - 1;

		rect.setAttribute("x", Integer.toString(x));
		rect.setAttribute("y", Float.toString(y));
		rect.setAttribute("width", Integer.toString(rectWidth));
		rect.setAttribute("height", Float.toString(height));
		setEventClass(rect, pe);
		g.appendChild(rect);

		int txtHeight = LINE_HEIGHT - 2; // line height - margin

		// Clip
		String key = "clip-" + pe.calendarId + "-" + pe.dtstart.getTimeInMillis();
		Element clipPath = page.createElement("clipPath");
		clipPath.setAttribute("id", key);
		Element clipPathRect = page.createElement("rect");
		clipPathRect.setAttribute("width", Integer.toString(rectWidth - 3));
		clipPathRect.setAttribute("height", Float.toString(height));
		clipPathRect.setAttribute("x", Integer.toString(x));
		clipPathRect.setAttribute("y", Float.toString(y));
		clipPath.appendChild(clipPathRect);
		root.appendChild(clipPath);

		if (duration(pe.event) >= 3600) {
			if (height >= txtHeight) {
				Element h = page.createElement("text");
				Calendar begin = Calendar.getInstance(timezone);
				begin.setTimeInMillis(new BmDateTimeWrapper(pe.event.dtstart).toTimestamp(timezone.getID()));

				Calendar end = Calendar.getInstance(timezone);
				end.setTimeInMillis(new BmDateTimeWrapper(pe.event.dtend).toTimestamp(timezone.getID()));

				String txt = (timeFormat.format(begin.getTime()) + " - " + timeFormat.format(end.getTime()));
				NodeUtils.setText(h, txt);
				h.setAttribute("x", Integer.toString(x + 3));
				h.setAttribute("y", Float.toString((float) (y + (double) LINE_HEIGHT / 2 + 3)));
				h.setAttribute("clip-path", "url(#" + key + ")");
				h.setAttribute("style", getEventTitleStyle(pe.calendarId, pe.part));
				g.appendChild(h);
			}

			if (height >= txtHeight * 2) {

				Element title = page.createElement("text");
				String titleTxt = getTitle(pe.event);
				String locationTxt = getLocation(pe.event);
				Element tspan;

				int textLength = titleTxt.length() * 5;
				if (locationTxt != null && !locationTxt.isEmpty()) {
					textLength = (titleTxt.length() + 2 + locationTxt.length()) * 5;
				}

				if (textLength > rectWidth) {
					// word-wrap
					List<Element> lines = multipleLines(pe, titleTxt, locationTxt, rectWidth);
					int i = 1;
					for (Element line : lines) {
						line.setAttribute("x", Integer.toString(x + 3));
						line.setAttribute("y", Float.toString(y + LINE_HEIGHT * i + 6));
						line.setAttribute("clip-path", "url(#" + key + ")");
						title.appendChild(line);
						i++;
					}
				} else {
					tspan = page.createElement("tspan");
					NodeUtils.setText(tspan, titleTxt);
					tspan.setAttribute("x", Integer.toString(x + 3));
					tspan.setAttribute("y", Float.toString(y + LINE_HEIGHT + 6));
					tspan.setAttribute("clip-path", "url(#" + key + ")");
					tspan.setAttribute("style", getEventTitleStyle(pe.calendarId, pe.part));
					title.appendChild(tspan);

					if (locationTxt != null && !locationTxt.isEmpty()) {
						NodeUtils.setText(tspan, ", ");
						tspan = page.createElement("tspan");
						NodeUtils.setText(tspan, locationTxt);
						tspan.setAttribute("style", getEventLocationStyle(pe.calendarId, pe.part));
						title.appendChild(tspan);
					}

				}

				g.appendChild(title);
			}

		} else {
			if (height >= txtHeight) {

				Calendar begin = Calendar.getInstance(timezone);
				begin.setTimeInMillis(new BmDateTimeWrapper(pe.event.dtstart).toTimestamp(timezone.getID()));

				Element text = page.createElement("text");
				NodeUtils.setText(text, timeFormat.format(begin.getTime()) + " - " + getTitle(pe.event));
				text.setAttribute("x", Integer.toString(x + 3));
				text.setAttribute("y", Float.toString(rowHeight + y - (height / 2)));
				text.setAttribute("clip-path", "url(#" + key + ")");
				text.setAttribute("style", getEventTitleStyle(pe.calendarId, pe.part));
				g.appendChild(text);

				addLocation(pe.calendarId, pe.event, text);
			}
		}
		root.appendChild(g);
	}

	private List<Element> multipleLines(PrintedEvent pe, String titleTxt, String locationTxt, int rectWidth) {
		int nbCharsPerLine = (rectWidth - 3) / 5;
		List<Element> lines = new LinkedList<Element>();

		if (locationTxt == null || locationTxt.isEmpty()) {
			splitToLine(lines, nbCharsPerLine, titleTxt, getEventTitleStyle(pe.calendarId, pe.part));
		} else {
			splitToLine(lines, nbCharsPerLine, titleTxt + ", ", getEventTitleStyle(pe.calendarId, pe.part));
			splitToLine(lines, nbCharsPerLine, locationTxt, getEventLocationStyle(pe.calendarId, pe.part));
		}

		return lines;
	}

	private void splitToLine(List<Element> lines, int nbCharsPerLine, String titleTxt, String styleEventLocation) {
		String[] words = titleTxt.split("|");

		StringBuilder l = new StringBuilder();
		for (String word : words) {
			if (l.length() + word.length() < nbCharsPerLine) {
				l.append(word);
			} else {
				Element tspan = page.createElement("tspan");
				NodeUtils.setText(tspan, l.toString());
				tspan.setAttribute("style", styleEventLocation);
				lines.add(tspan);
				l = new StringBuilder();
				l.append(word);
			}
		}

		if (l.length() > 0) {
			Element tspan = page.createElement("tspan");
			NodeUtils.setText(tspan, l.toString());
			tspan.setAttribute("style", styleEventLocation);
			lines.add(tspan);
		}
	}

	private void setEventClass(Element event, PrintedEvent pe) {
		event.setAttribute("rx", "3");

		String s = "fill:" + calInfos.get(pe.calendarId).color + ";stroke-width:1;stroke:"
				+ calInfos.get(pe.calendarId).colorDarkerDarker + ";";
		if (pe.part == ParticipationStatus.Declined) {
			s += "opacity: 0.5;";
		}

		event.setAttribute("style", s);
	}
}
