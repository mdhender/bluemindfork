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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Strings;

import net.bluemind.calendar.api.PrintOptions;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.utils.DateTimeComparator;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;

public abstract class PrintCalendar {
	private static final Logger logger = LoggerFactory.getLogger(PrintCalendar.class);

	public static class CalInfo {
		public String uid;
		public String name;
		public String color;
		public String colorLighter;
		public String colorDarker;
		public String colorDarkerDarker;
		public String textColor;
	}

	protected static final int MARGIN = 30;
	protected static final int LINE_HEIGHT = 12;
	protected static final int PORTRAIT_LINE_WIDTH = 125;
	protected static final int LANDSCAPE_LINE_WIDTH = 170;
	protected static final int TAB_WIDTH = 15;

	protected static final String STYLE_GRID = "fill:white;stroke:#CCC;stroke-width:1;";
	protected static final String STYLE_GRID_DASHED = "stroke-dasharray: 3, 3;fill:white;stroke:#CCC;stroke-width:1;";

	public String getEventTitleStyle(String calId, ParticipationStatus part) {
		String ret = "fill:" + calInfos.get(calId).textColor + ";font-weight:bolder;font-size:8px;";
		if (part == ParticipationStatus.Declined) {
			ret += "text-decoration:line-through;";
		}
		return ret;
	}

	public String getEventLocationStyle(String calId, ParticipationStatus part) {
		String ret = "fill:" + calInfos.get(calId).textColor + ";font-style:italic;font-size:8px;";
		if (part == ParticipationStatus.Declined) {
			ret += "text-decoration:line-through;";
		}

		return ret;
	}

	protected static final String STYLE_MORE_EVENTS = "fill:#CCC;stroke-width:1;stroke:#;";
	protected static final String STYLE_MORE_EVENTS_LABEL = "fill:#000;font-size:6px;";

	protected static final String STYLE_GEN_DATE = "fill:#CCC;font-size:9px;";

	private static final int PORTRAIT_WIDTH = 744;
	private static final int PORTRAIT_HEIGHT = 1052;

	private static final int LANDSCAPE_WIDTH = 1042;
	private static final int LANDSCAPE_HEIGHT = 744;

	protected int pageWidth;
	protected int pageHeight;
	protected int pageLineWidth;

	protected PrintOptions options;
	protected SimpleDateFormat dateFormat;
	protected SimpleDateFormat timeFormat;
	public List<Document> pages;
	protected TimeZone GMTtimezone;
	protected TimeZone timezone;
	protected String locale;
	protected SecurityContext securityContext;
	protected int y;
	protected Document page;
	protected Element root;
	protected Map<String, CalInfo> calInfos = new HashMap<>();
	protected Properties lang;
	protected String pageStyle;
	protected Locale l;
	protected int firstDayOfWeek;
	protected float evtHourStart;
	protected float evtHourEnd;

	private List<CalInfo> calendars;
	protected Map<String, String> userSettings;

	public PrintCalendar(PrintContext context, PrintOptions options) throws ServerFault {
		this.options = options;

		userSettings = context.userSettings;

		this.securityContext = context.securityContext;
		locale = userSettings.get("lang");
		l = new Locale(locale);
		dateFormat = new SimpleDateFormat("EEEE dd MMMM yyyy", l);
		timeFormat = new SimpleDateFormat(userSettings.get("timeformat"), l);
		GMTtimezone = TimeZone.getTimeZone("GMT");
		timezone = TimeZone.getTimeZone(userSettings.get("timezone"));

		dateFormat.setTimeZone(timezone);
		timeFormat.setTimeZone(timezone);
		pages = new ArrayList<Document>();

		evtHourStart = 23;
		evtHourEnd = 0;

		firstDayOfWeek = Calendar.SUNDAY;
		if ("monday".equals(userSettings.get("day_weekstart"))) {
			firstDayOfWeek = Calendar.MONDAY;
		}

		String file = "en.properties";
		if ("fr".equals(locale)) {
			file = "fr.properties";
		}
		InputStream in = getClass().getClassLoader().getResourceAsStream("translation_" + file);
		lang = new Properties();
		try {
			lang.load(in);
		} catch (Exception t) {
			throw new ServerFault(t.getMessage(), t);
		}

		this.calendars = context.calendars;
		for (CalInfo calInfo : context.calendars) {
			calInfos.put(calInfo.uid, calInfo);
		}

	}

	/**
	 * @param vevents
	 * @param begin
	 * @param end
	 * @return
	 */

	protected Map<Long, List<ItemContainerValue<VEvent>>> sortOccurrences(List<ItemContainerValue<VEvent>> vevents,
			BmDateTime begin, BmDateTime end) {

		boolean showDeclinedEvents = new Boolean(this.userSettings.get("show_declined_events"));

		// sort occurrences by date
		Map<Long, List<ItemContainerValue<VEvent>>> ocs = new TreeMap<Long, List<ItemContainerValue<VEvent>>>();
		Calendar ocStart = Calendar.getInstance(TimeZone.getTimeZone(timezone.getID()));
		Calendar ocEnd = (Calendar) ocStart.clone();
		Long key;
		// fix occurrence date begin/end (starts before and/or ends after
		// date range);

		Calendar rangeStart = (Calendar) ocStart.clone();
		rangeStart.setTimeInMillis(new BmDateTimeWrapper(begin).toTimestamp(timezone.getID()));
		rangeStart.set(Calendar.HOUR_OF_DAY, 0);
		rangeStart.set(Calendar.MINUTE, 0);
		rangeStart.set(Calendar.SECOND, 0);
		rangeStart.set(Calendar.MILLISECOND, 0);

		Calendar rangeEnd = Calendar.getInstance(TimeZone.getTimeZone(timezone.getID()));
		rangeEnd.setTimeInMillis(new BmDateTimeWrapper(end).toTimestamp(timezone.getID()));
		rangeEnd.set(Calendar.HOUR_OF_DAY, 0);
		rangeEnd.set(Calendar.MINUTE, 0);
		rangeEnd.set(Calendar.SECOND, 0);
		rangeEnd.set(Calendar.MILLISECOND, 0);

		for (ItemContainerValue<VEvent> item : vevents) {
			VEvent vevent = item.value;

			if (!showDeclinedEvents && this.isADeclinedInvitation(item)) {
				continue;
			}

			BmDateTimeWrapper dtstartBm = new BmDateTimeWrapper(vevent.dtstart);
			BmDateTimeWrapper dtendBm = new BmDateTimeWrapper(vevent.dtend);
			ocStart.setTimeInMillis(dtstartBm.toTimestamp(timezone.getID()));
			if (vevent.allDay()) {
				ocEnd.setTimeInMillis(dtstartBm.toTimestamp(timezone.getID()));
			} else {
				ocEnd.setTimeInMillis(dtendBm.toTimestamp(timezone.getID()));
			}

			if (ocStart.compareTo(rangeStart) < 0) {
				if (!vevent.allDay()) {
					ocStart.set(Calendar.DAY_OF_YEAR, rangeStart.get(Calendar.DAY_OF_YEAR));
					vevent.dtstart = dtstartBm.copy(ocStart.getTimeInMillis());
				} else {
					vevent.dtstart = dtstartBm.copy(rangeStart.getTimeInMillis());
				}
			}

			if (ocEnd.compareTo(rangeEnd) > 0) {
				if (!vevent.allDay()) {
					ocEnd.set(Calendar.DAY_OF_YEAR, rangeEnd.get(Calendar.DAY_OF_YEAR));
					vevent.dtend = dtendBm.copy(ocEnd.getTimeInMillis());
				} else {
					vevent.dtend = dtendBm.copy(rangeEnd.getTimeInMillis());
				}
			}

			Calendar eend = (Calendar) ocEnd.clone();
			eend.add(Calendar.SECOND, -1);
			if (!vevent.allDay() && ocStart.get(Calendar.DAY_OF_YEAR) == eend.get(Calendar.DAY_OF_YEAR)) {
				evtHourStart = Math.min(evtHourStart,
						(ocStart.get(Calendar.HOUR_OF_DAY) + (float) ocStart.get(Calendar.MINUTE) / 60));

				if (ocEnd.get(Calendar.HOUR_OF_DAY) + ocEnd.get(Calendar.MINUTE) / 60 == 0) {
					evtHourEnd = 24;
				} else {
					evtHourEnd = Math.max(evtHourEnd,
							(ocEnd.get(Calendar.HOUR_OF_DAY) + (float) ocEnd.get(Calendar.MINUTE) / 60));
				}
			}
			ocStart.set(Calendar.HOUR_OF_DAY, 0);
			ocStart.set(Calendar.MINUTE, 0);
			ocStart.set(Calendar.SECOND, 0);
			ocStart.set(Calendar.MILLISECOND, 0);
			key = ocStart.getTimeInMillis();
			if (!ocs.containsKey(key)) {
				ocs.put(key, new ArrayList<ItemContainerValue<VEvent>>());
			}
			ocs.get(key).add(item);

		}
		return ocs;
	}

	protected ParticipationStatus getPart(VEvent vevent) {
		for (Attendee a : vevent.attendees) {
			if (("bm://" + securityContext.getContainerUid() + "/users/" + securityContext.getSubject())
					.equals(a.dir)) {
				return a.partStatus;
			}
		}
		return null;
	}

	/**
	 * @return <code>true</code> if the given event is a declined invitation
	 *         i.e.: this event is declined by X and the event's calendar is the
	 *         X's default calendar
	 */
	private boolean isADeclinedInvitation(ItemContainerValue<VEvent> event) {
		if (event.value.attendees == null || event.value.attendees.isEmpty()) {
			// no attendee, so it is not an invitation
			return false;
		}

		// search for a declined attendee in her own default calendar
		String calendarUid = this.extractUidFromItemContainerUid(event.containerUid);
		for (Attendee attendee : event.value.attendees) {
			if (ParticipationStatus.Declined == attendee.partStatus) {
				String attendeeUid = this.extractUidFromAttendeeDir(attendee.dir);
				return attendeeUid.equals(calendarUid);
			}
		}

		return false;
	}

	/**
	 * The uid of the attendee is found at the very end after the last
	 * <b>&#47;</b> and the last <b>:</b>.
	 */
	private String extractUidFromAttendeeDir(String attendeeDir) {
		String[] split = Strings.nullToEmpty(attendeeDir).split("[/:]");
		return split[split.length - 1];
	}

	/**
	 * The uid of the container is found at the very end after the last
	 * <b>:</b>.
	 */
	private String extractUidFromItemContainerUid(String itemContainerUid) {
		String[] split = itemContainerUid.split(":");
		return split[split.length - 1];
	}

	public byte[] sendSVGString() throws ServerFault {
		byte[] ret = null;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TransformerFactory fac = TransformerFactory.newInstance();
			Transformer tf = fac.newTransformer();
			tf.setOutputProperty(OutputKeys.INDENT, "no");
			Source input = new DOMSource(pages.get(0).getDocumentElement());
			Result output = new StreamResult(out);
			tf.transform(input, output);
			ret = out.toByteArray();
		} catch (Exception t) {
			throw new ServerFault(t.getMessage(), t);
		}
		return ret;
	}

	public byte[] sendJPEGString() throws ServerFault {
		ByteArrayOutputStream os = null;
		try {
			JPEGTranscoder t = new JPEGTranscoder();
			t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(0.8));
			t.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, new Float(250));
			ByteArrayInputStream bais = svgToByteArrayInputStream(pages.get(0));
			TranscoderInput input = new TranscoderInput(bais);
			os = new ByteArrayOutputStream();
			TranscoderOutput output = new TranscoderOutput(os);
			t.transcode(input, output);
			return os.toByteArray();
		} catch (Exception t) {
			throw new ServerFault(t.getMessage(), t);
		} finally {
			try {
				if (os != null) {
					os.flush();
					os.close();
				}
			} catch (IOException e) {
			}
		}
	}

	public byte[] sendPNGString() throws ServerFault {
		ByteArrayOutputStream os = null;
		try {
			PNGTranscoder t = new PNGTranscoder();
			t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(250));
			ByteArrayInputStream bais = svgToByteArrayInputStream(pages.get(0));
			TranscoderInput input = new TranscoderInput(bais);
			os = new ByteArrayOutputStream();
			TranscoderOutput output = new TranscoderOutput(os);
			t.transcode(input, output);
			return os.toByteArray();
		} catch (Exception t) {
			throw new ServerFault(t.getMessage(), t);
		} finally {
			try {
				if (os != null) {
					os.flush();
					os.close();
				}
			} catch (IOException e) {
			}
		}
	}

	public byte[] sendPDFString() throws ServerFault {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			PDFTranscoder t = new PDFTranscoder();
			if (pages.size() > 1) {
				PDFMergerUtility mergePdf = new PDFMergerUtility();
				for (Document page : pages) {
					ByteArrayInputStream bais = svgToByteArrayInputStream(page);
					TranscoderInput input = new TranscoderInput(bais);
					TranscoderOutput output = new TranscoderOutput(os);
					t.transcode(input, output);
					mergePdf.addSource(new ByteArrayInputStream(os.toByteArray()));
					os.reset();
				}
				mergePdf.setDestinationStream(os);
				mergePdf.mergeDocuments();
			} else {
				ByteArrayInputStream bais = svgToByteArrayInputStream(pages.get(0));
				TranscoderInput input = new TranscoderInput(bais);
				TranscoderOutput output = new TranscoderOutput(os);
				t.transcode(input, output);
			}
			return os.toByteArray();
		} catch (Exception t) {
			throw new ServerFault(t.getMessage(), t);
		} finally {
			try {
				os.flush();
				os.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private ByteArrayInputStream svgToByteArrayInputStream(Document page) throws TransformerException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TransformerFactory fac = TransformerFactory.newInstance();
		Transformer tf = fac.newTransformer();
		Source in = new DOMSource(page.getDocumentElement());
		Result r = new StreamResult(out);
		tf.transform(in, r);

		return new ByteArrayInputStream(out.toByteArray());
	}

	protected Document newPage() {

		DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
		Document document = impl.createDocument(svgNS, "svg", null);
		Element root = document.getDocumentElement();
		if (options.layout == PrintOptions.PrintLayout.PORTRAIT) {
			root.setAttributeNS(null, "width", Integer.toString(PORTRAIT_WIDTH));
			root.setAttributeNS(null, "height", Integer.toString(PORTRAIT_HEIGHT));
			pageWidth = PORTRAIT_WIDTH;
			pageHeight = PORTRAIT_HEIGHT;
			pageLineWidth = PORTRAIT_LINE_WIDTH;
		} else {
			root.setAttributeNS(null, "width", Integer.toString(LANDSCAPE_WIDTH));
			root.setAttributeNS(null, "height", Integer.toString(LANDSCAPE_HEIGHT));
			pageWidth = LANDSCAPE_WIDTH;
			pageHeight = LANDSCAPE_HEIGHT;
			pageLineWidth = LANDSCAPE_LINE_WIDTH;
		}
		return document;
	}

	public abstract void process() throws ServerFault;

	protected void addPage() {
		page = newPage();
		root = page.getDocumentElement();
		pages.add(page);
		y = MARGIN;
		setHeader();
	}

	protected void addText(int x, String text) {
		if (y > pageHeight - MARGIN) {
			addPage();
		}

		int length = text.length();
		String line = text;
		int startIdx = 0;
		int endIdx;
		int lineWidth = pageLineWidth - x / 4;
		Element value;
		Element tspan;
		while (startIdx < length) {
			value = page.createElement("text");

			endIdx = startIdx + lineWidth;
			if ((startIdx + lineWidth) > text.length()) {
				endIdx = text.length();
			}

			line = text.substring(startIdx, endIdx);
			tspan = page.createElement("tspan");
			tspan.setAttribute("x", Integer.toString(x));
			tspan.setAttribute("y", Integer.toString(y));
			NodeUtils.setText(tspan, line);
			value.appendChild(tspan);
			startIdx += lineWidth;
			y += LINE_HEIGHT;
			root.appendChild(value);
			if (y > pageHeight - MARGIN) {
				addPage();
			}
		}
	}

	private void setHeader() {
		Calendar start = Calendar.getInstance(timezone);
		start.setTimeInMillis(new BmDateTimeWrapper(options.dateBegin).toTimestamp(timezone.getID()));
		String date = dateFormat.format(start.getTime());

		Calendar end = Calendar.getInstance(timezone);
		end.setTimeInMillis(new BmDateTimeWrapper(options.dateEnd).toTimestamp(timezone.getID()) - 1);
		if (!date.equals(dateFormat.format(end.getTime()))) {
			date += " - " + dateFormat.format(end.getTime());
		}
		addText(MARGIN, date);

		addCalendars(calendars);

		// add empty line
		y += LINE_HEIGHT;
	}

	/**
	 * @param calendars
	 */
	private void addCalendars(List<CalInfo> calendars) {
		Element containers = page.createElement("text");
		containers.setAttribute("x", Integer.toString(MARGIN));
		containers.setAttribute("y", Integer.toString(y));
		Element calendar;
		Element sep = null;
		int idx = 0;
		List<Element> lines = new ArrayList<Element>();
		lines.add(containers);
		for (CalInfo ci : calendars) {

			String label = ci.name;
			calendar = page.createElement("tspan");
			calendar.setAttribute("style", "fill:" + ci.color);
			NodeUtils.setText(calendar, label);

			idx += label.length() + 2;
			if (idx > pageLineWidth) {
				y += LINE_HEIGHT;
				containers = page.createElement("text");
				containers.setAttribute("x", Integer.toString(MARGIN));
				containers.setAttribute("y", Integer.toString(y));
				lines.add(containers);
			}
			if (sep != null) {
				containers.appendChild(sep);
			}
			containers.appendChild(calendar);

			sep = page.createElement("tspan");
			NodeUtils.setText(sep, ", ");
		}

		for (Element line : lines) {
			root.appendChild(line);
		}
		y += LINE_HEIGHT;
	}

	public String getTitle(VEvent e) {
		boolean isAttendee = isAttendee(e);

		String title = lang.getProperty("private");
		if (!(e.classification == VEvent.Classification.Private
				&& (e.organizer != null && securityContext.getSubject().equals(e.organizer.uri)) && !isAttendee)) { // FIXME
			title = e.summary;
		}

		return title;
	}

	protected boolean isAttendee(VEvent e) {
		boolean isAttendee = false;
		List<VEvent.Attendee> attendees = e.attendees;
		if (attendees == null) {
			return false;
		}
		for (VEvent.Attendee a : attendees) {
			if (("bm://" + securityContext.getContainerUid() + "/users/" + securityContext.getSubject())
					.equals(a.dir)) {
				isAttendee = true;
				break;
			}
		}
		return isAttendee;
	}

	protected void addLocation(String calId, VEvent event, Element text) {
		String locationTxt = getLocation(event);
		if (locationTxt != null && !locationTxt.isEmpty()) {
			NodeUtils.setText(text, ", ");
			Element location = page.createElement("tspan");
			NodeUtils.setText(location, locationTxt);
			location.setAttribute("style", getEventLocationStyle(calId, getPart(event)));
			text.appendChild(location);
		}
	}

	protected String getLocation(VEvent event) {
		boolean isAttendee = isAttendee(event);

		if (!(event.classification == VEvent.Classification.Private
				&& (event.organizer != null && securityContext.getSubject().equals(event.organizer.uri))
				&& !isAttendee)) { // FIXME
			if (event.location != null && !event.location.isEmpty()) {
				return event.location;
			}
		}

		return null;
	}

	public class EventComparator implements Comparator<ItemContainerValue<VEvent>> {
		private final String timezone;

		public EventComparator(String timezone) {
			this.timezone = timezone;
		}

		public int compare(ItemContainerValue<VEvent> o1, ItemContainerValue<VEvent> o2) {
			int d = new DateTimeComparator(timezone).compare(o1.value.dtstart, o2.value.dtstart);
			if (d != 0)
				return d;

			d = duration(o2.value).intValue() - duration(o1.value).intValue();
			if (d != 0)
				return d;

			return 0;
		}
	}

	protected int getRelativeDayOfWeeek(Calendar date) {
		return Math.floorMod(date.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek, 7);
	}

	public Long duration(VEvent vevent) {
		return vevent.dtend != null
				? (new BmDateTimeWrapper(vevent.dtend).toTimestamp(timezone.getID())
						- new BmDateTimeWrapper(vevent.dtstart).toTimestamp(timezone.getID())) / 1000
				: null;

	}
}
