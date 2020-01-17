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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;

import net.bluemind.calendar.api.PrintOptions;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;

public class PrintCalendarMonth extends PrintCalendar {

	private List<ItemContainerValue<VEvent>> vevents;
	private int drawableEvents;
	private int dayWidth;
	private int dayHeight;
	private Map<Integer, Integer> weekIdx;

	public PrintCalendarMonth(PrintContext context, PrintOptions options, List<ItemContainerValue<VEvent>> vevents)
			throws ServerFault {
		super(context, options);
		this.vevents = vevents;
		addPage();
		weekIdx = new HashMap<Integer, Integer>();
	}

	private void setGrid() {
		Calendar start = Calendar.getInstance(timezone);
		start.setTimeInMillis(new BmDateTimeWrapper(options.dateBegin).toTimestamp(timezone.getID()));

		Calendar end = Calendar.getInstance(timezone);
		end.setTimeInMillis(new BmDateTimeWrapper(options.dateEnd).toTimestamp(timezone.getID()));

		int contentHeight = pageHeight - y - MARGIN - LINE_HEIGHT;
		int contentWidth = pageWidth - 3 * MARGIN;

		int startX = MARGIN * 2;

		Element rect = page.createElement("rect");
		rect.setAttribute("x", Integer.toString(startX));
		rect.setAttribute("y", Integer.toString(y));
		rect.setAttribute("width", Integer.toString(contentWidth));
		rect.setAttribute("height", Integer.toString(contentHeight));
		rect.setAttribute("style", STYLE_GRID);
		root.appendChild(rect);

		Element separator;
		dayWidth = contentWidth / 7;
		String x;
		SimpleDateFormat sdf = new SimpleDateFormat("EEE", l);
		sdf.setTimeZone(start.getTimeZone());

		// mon / tue / wed / thu / fri / sat / sun
		for (int i = 0; i < 7; i++) {
			Element dayLabel = page.createElement("text");
			dayLabel.setAttribute("x", Integer.toString(startX + i * dayWidth + dayWidth / 2));
			dayLabel.setAttribute("y", Integer.toString(y - 3));
			dayLabel.setAttribute("text-anchor", "middle");
			NodeUtils.setText(dayLabel, sdf.format(start.getTime()));
			root.appendChild(dayLabel);

			separator = page.createElement("line");
			x = Integer.toString(startX + i * dayWidth);
			separator.setAttribute("x1", x);
			separator.setAttribute("x2", x);
			separator.setAttribute("y1", Integer.toString(y));
			separator.setAttribute("y2", Integer.toString(pageHeight - MARGIN - LINE_HEIGHT));
			separator.setAttribute("style", STYLE_GRID);
			root.appendChild(separator);

			start.add(Calendar.DATE, 1);
		}
		start.setTimeInMillis(new BmDateTimeWrapper(options.dateBegin).toTimestamp(timezone.getID()));
		start.add(Calendar.DATE, 1);

		// rows
		dayHeight = contentHeight / 6;
		drawableEvents = 6;
		for (int i = 0; i < 6; i++) {
			separator = page.createElement("line");
			int h = y + i * dayHeight;
			separator.setAttribute("x1", Integer.toString(startX));
			separator.setAttribute("x2", Integer.toString(contentWidth + startX));
			separator.setAttribute("y1", Integer.toString(h));
			separator.setAttribute("y2", Integer.toString(h));
			separator.setAttribute("style", STYLE_GRID);
			root.appendChild(separator);
		}

		Element txt;
		int d = 1;
		int w = 0;
		sdf = new SimpleDateFormat("dd", l);
		// DATE
		while (start.compareTo(end) < 0) {

			txt = page.createElement("text");
			txt.setAttribute("x", Integer.toString(startX + d * dayWidth - MARGIN / 2));
			txt.setAttribute("y", Integer.toString(y + w * dayHeight + LINE_HEIGHT));
			NodeUtils.setText(txt, sdf.format(start.getTime()));
			root.appendChild(txt);
			start.add(Calendar.DATE, 1);

			int week = start.get(Calendar.WEEK_OF_YEAR);
			if (d == 1) {
				Element weekNum = page.createElement("text");
				weekNum.setAttribute("x", Integer.toString(startX / 2));
				weekNum.setAttribute("y", Integer.toString(y + w * dayHeight + dayHeight / 2));
				NodeUtils.setText(weekNum, Integer.toString(week));
				root.appendChild(weekNum);
				weekIdx.put(week, w);
			}

			if (d == 7) {
				w++;
				d = 0;
			}
			d++;
		}
	}

	@Override
	public void process() throws ServerFault {
		setGrid();
		Map<Long, List<ItemContainerValue<VEvent>>> occurrencePerWeek = new HashMap<>();

		Calendar start = Calendar.getInstance(timezone);
		start.setFirstDayOfWeek(firstDayOfWeek);

		Calendar end = (Calendar) start.clone();

		for (ItemContainerValue<VEvent> item : vevents) {
			VEvent vevent = item.value;
			start.setTimeInMillis(new BmDateTimeWrapper(vevent.dtstart).toTimestamp(timezone.getID()));

			end.setTimeInMillis(new BmDateTimeWrapper(vevent.dtend).toTimestamp(timezone.getID()));
			long cur = 0;
			while (start.compareTo(end) < 0) {
				Calendar startOfWeek = (Calendar) start.clone();
				startOfWeek.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);
				startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
				startOfWeek.set(Calendar.MINUTE, 0);
				startOfWeek.set(Calendar.SECOND, 0);
				startOfWeek.set(Calendar.MILLISECOND, 0);

				long idx = startOfWeek.getTimeInMillis();

				if (cur == 0 || cur != idx) {
					cur = idx;

					ItemContainerValue<VEvent> copy = new ItemContainerValue<VEvent>();
					copy.created = item.created;
					copy.updated = item.updated;
					copy.createdBy = item.createdBy;
					copy.updatedBy = item.updatedBy;
					copy.uid = item.uid;
					copy.version = item.version;
					copy.externalId = item.externalId;
					copy.displayName = item.displayName;
					copy.value = item.value.copy();
					copy.containerUid = item.containerUid;
					if (!occurrencePerWeek.containsKey(idx)) {
						occurrencePerWeek.put(idx, new ArrayList<>());
					}

					occurrencePerWeek.get(idx).add(copy);
				}

				start.add(Calendar.DATE, 1);
			}

		}

		Map<Long, List<ItemContainerValue<VEvent>>> ocs;
		for (Long idx : occurrencePerWeek.keySet()) {
			if (occurrencePerWeek.get(idx).size() > 0) {
				Calendar periodStart = Calendar.getInstance(timezone);
				periodStart.setFirstDayOfWeek(firstDayOfWeek);

				periodStart.setTimeInMillis(idx);
				Calendar periodEnd = (Calendar) periodStart.clone();
				periodEnd.add(Calendar.DATE, 7);

				Iterator<ItemContainerValue<VEvent>> it = occurrencePerWeek.get(idx).iterator();
				while (it.hasNext()) {
					ItemContainerValue<VEvent> item = it.next();
					VEvent e = item.value;
					Calendar dtstart = Calendar.getInstance(timezone);
					dtstart.setTimeInMillis(new BmDateTimeWrapper(e.dtstart).toTimestamp(timezone.getID()));
					Calendar dtend = (Calendar) dtstart.clone();
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
				}

				ocs = sortOccurrences(occurrencePerWeek.get(idx),
						BmDateTimeWrapper.fromTimestamp(periodStart.getTimeInMillis()),
						BmDateTimeWrapper.fromTimestamp(periodEnd.getTimeInMillis()));

				addVEvents(ocs);
			}
		}
	}

	private void addVEvents(Map<Long, List<ItemContainerValue<VEvent>>> ocs) {
		Calendar dtstart = Calendar.getInstance(timezone);
		Map<Integer, List<ItemContainerValue<VEvent>>> alldays = new TreeMap<>();
		for (Long key : ocs.keySet()) {
			for (ItemContainerValue<VEvent> e : ocs.get(key)) {
				VEvent o = e.value;
				dtstart.setTimeInMillis(new BmDateTimeWrapper(o.dtstart).toTimestamp(timezone.getID()));
				registerAllDayEvent(dtstart, alldays, e);
			}
		}
		addAllDayEvent(alldays);
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
			boolean moreEventsLabelAdded = false;
			for (ItemContainerValue<VEvent> item : alldays) {
				VEvent o = item.value;
				dtstart.setTimeInMillis(new BmDateTimeWrapper(o.dtstart).toTimestamp(timezone.getID()));
				dtend.setTimeInMillis(new BmDateTimeWrapper(o.dtend).toTimestamp(timezone.getID()));

				posX = MARGIN * 2 + key * dayWidth;

				int idx = 0;
				if (fixedPosY.containsKey(key)) {
					Map<Integer, String> list = fixedPosY.get(key);
					while (list.containsKey(idx)) {
						idx++;
					}
				}

				int week = dtstart.get(Calendar.WEEK_OF_YEAR);

				if (weekIdx.containsKey(week)) {
					int i = weekIdx.get(week);

					int posY = LINE_HEIGHT + y + idx * (LINE_HEIGHT + 2) + i * dayHeight;
					int pos = 0;
					int size = 0;
					boolean allday = o.allDay() || dtstart.get(Calendar.DATE) != dtend.get(Calendar.DATE);
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

					if (pos < drawableEvents) {
						Element text = page.createElement("text");
						text.setAttribute("x", Integer.toString(posX + 3));
						text.setAttribute("y", Integer.toString(posY + LINE_HEIGHT - 3));

						if (allday) {
							Element rect = page.createElement("rect");
							rect.setAttribute("x", Integer.toString(posX));
							rect.setAttribute("y", Integer.toString(posY));
							rect.setAttribute("width", Integer.toString(size * dayWidth));
							rect.setAttribute("height", Integer.toString(LINE_HEIGHT));
							setEventClass(rect, item.containerUid);

							// Clip
							String clip = "clip-" + item.containerUid + "-" + dtstart.getTimeInMillis();
							Element clipPath = page.createElement("clipPath");
							clipPath.setAttribute("id", clip);
							rect.appendChild(clipPath);

							text.setAttribute("style", getEventTitleStyle(item.containerUid, getPart(item.value)));
							if (o.allDay()) {
								NodeUtils.setText(text, getTitle(o));
							} else {
								NodeUtils.setText(text, timeFormat.format(dtstart.getTime()) + " - " + getTitle(o));
							}
							root.appendChild(rect);
							addLocation(item.containerUid, o, text);

						} else {
							String s = "fill:" + calInfos.get(item.containerUid).color
									+ ";font-weight:bolder;font-size:8px;width:" + dayWidth + ";";
							if (getPart(item.value) == ParticipationStatus.Declined) {
								s += "text-decoration: line-through;";
							}
							text.setAttribute("style", s);
							Calendar begin = Calendar.getInstance(timezone);
							begin.setTimeInMillis(dtstart.getTimeInMillis());
							NodeUtils.setText(text, timeFormat.format(begin.getTime()) + " - " + getTitle(o));

							String clip = "clip-" + item.containerUid + "t-" + dtstart.getTimeInMillis();
							Element clipPath = page.createElement("clipPath");
							clipPath.setAttribute("id", clip);

							Element rect = page.createElement("rect");
							rect.setAttribute("x", Integer.toString(posX));
							rect.setAttribute("y", Integer.toString(posY));
							rect.setAttribute("width", Integer.toString(dayWidth));
							rect.setAttribute("height", Integer.toString(LINE_HEIGHT));
							rect.setAttribute("style", "fill:none");
							clipPath.appendChild(rect);
							root.appendChild(clipPath);

							String locationTxt = getLocation(o);
							if (locationTxt != null && !locationTxt.isEmpty()) {
								NodeUtils.setText(text, ", ");
								Element location = page.createElement("tspan");
								NodeUtils.setText(location, locationTxt);
								location.setAttribute("style", "fill:" + calInfos.get(item.containerUid).color
										+ ";font-weight:bolder;font-size:8px;width:" + dayWidth + ";");
								text.appendChild(location);
							}
							text.setAttribute("clip-path", "url(#" + clip + ")");

						}

						root.appendChild(text);

						posY += LINE_HEIGHT + 2;
					} else {
						// There is more events than available space, just add
						// something to show it
						if (!moreEventsLabelAdded) {
							addMoreEventsElement(item, posX, posY, root);
						}
						moreEventsLabelAdded = true;
					}
				}

			}
		}
	}

	private void registerAllDayEvent(Calendar dtstart, Map<Integer, List<ItemContainerValue<VEvent>>> alldays,
			ItemContainerValue<VEvent> o) {
		int d = getRelativeDayOfWeeek(dtstart);
		if (!alldays.containsKey(d)) {
			List<ItemContainerValue<VEvent>> le = new ArrayList<>();
			alldays.put(d, le);
		}
		alldays.get(d).add(o);
	}

	private void setEventClass(Element event, String calendarId) {
		event.setAttribute("rx", "3");
		event.setAttribute("style", "fill:" + calInfos.get(calendarId).color + ";stroke-width:1;stroke:"
				+ calInfos.get(calendarId).colorDarkerDarker + ";");
	}

	private void addMoreEventsElement(ItemContainerValue<VEvent> item, int posX, int posY, Element root) {
		Element rect = page.createElement("rect");
		rect.setAttribute("x", Integer.toString(posX));
		rect.setAttribute("y", Integer.toString(posY));
		rect.setAttribute("width", Integer.toString(dayWidth));
		rect.setAttribute("height", Integer.toString(LINE_HEIGHT / 2));
		rect.setAttribute("rx", "3");
		rect.setAttribute("style", STYLE_MORE_EVENTS);

		// Clip
		String clip = "clip" + item.containerUid;
		Element clipPath = page.createElement("clipPath");
		clipPath.setAttribute("id", clip);

		Element clipPathRect = page.createElement("rect");
		clipPathRect.setAttribute("width", Integer.toString(dayWidth - 3));
		clipPathRect.setAttribute("height", Integer.toString(LINE_HEIGHT / 2));
		clipPathRect.setAttribute("x", Integer.toString(posX));
		clipPathRect.setAttribute("y", Float.toString(posY));

		clipPath.appendChild(rect);
		root.appendChild(clipPath);

		Element text = page.createElement("text");
		// FIXME translate
		// text.setTextContent("There is more events...");
		text.setAttribute("x", Integer.toString(posX + dayWidth / 2));
		text.setAttribute("y", Integer.toString(posY + (LINE_HEIGHT / 2) - 1));
		text.setAttribute("clip-path", "url(#" + clip + ")");
		text.setAttribute("style", STYLE_MORE_EVENTS_LABEL);
		text.setAttribute("text-anchor", "middle");

		root.appendChild(rect);
		root.appendChild(text);
	}

}
