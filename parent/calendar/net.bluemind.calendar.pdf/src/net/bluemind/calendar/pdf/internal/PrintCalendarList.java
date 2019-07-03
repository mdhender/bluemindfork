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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.InputSource;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.calendar.api.PrintOptions;
import net.bluemind.calendar.api.PrintOptions.CalendarMetadata;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.Role;

public class PrintCalendarList extends PrintCalendar {

	private static Configuration cfg;

	public static class CalDisplay {
		public String name;
		public String color;

		public String getName() {
			return name;
		}

		public String getColor() {
			return color;
		}

		public CalDisplay(String name, String color) {
			this.name = name;
			this.color = color;
		}

	}

	public static class CalDay {
		public String name;
		public List<PrintEvent> events;

		public CalDay(String name, List<PrintEvent> events) {
			super();
			this.name = name;
			this.events = events;
		}

		public String getName() {
			return name;
		}

		public List<PrintEvent> getEvents() {
			return events;
		}

	}

	public static class PrintEvent {
		public String timeSlot;
		public VEvent event;
		public String title;
		public String color;
		public boolean details;
		public List<CalDisplay> attendees;
		private List<CalDisplay> fattendees;
		private CalDisplay chair;
		private String description;

		public PrintEvent(String timeSlot, String color, String title, boolean details, VEvent event, CalDisplay chair,
				List<CalDisplay> attendees, List<CalDisplay> fattendees, String description) {
			super();
			this.timeSlot = timeSlot;
			this.event = event;
			this.title = title;
			this.color = color;
			this.details = details;
			this.chair = chair;
			this.attendees = attendees;
			this.fattendees = fattendees;
			this.description = description;
		}

		public String getTimeSlot() {
			return timeSlot;
		}

		public VEvent getEvent() {
			return event;
		}

		public String getTitle() {
			return title;
		}

		public boolean isDetails() {
			return details;
		}

		public List<CalDisplay> getAttendees() {
			return attendees;
		}

		public List<CalDisplay> getFattendees() {
			return fattendees;
		}

		public CalDisplay getChair() {
			return chair;
		}

		public String getDescription() {
			return description;
		}
	}

	static {
		cfg = new Configuration();

		cfg.setClassForTemplateLoading(PrintCalendarList.class, "/tpl");
		cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		BeansWrapper wrapper = new BeansWrapper();
		wrapper.setExposeFields(true);
		cfg.setObjectWrapper(wrapper);
	}

	private String htmlDocument;
	private Map<Long, List<ItemContainerValue<VEvent>>> ocs;

	public PrintCalendarList(PrintContext context, PrintOptions options, List<ItemContainerValue<VEvent>> vevents)
			throws ServerFault {
		super(context, options);

		addPage();
		ocs = sortOccurrences(vevents, options.dateBegin, options.dateEnd);

	}

	@Override
	public void process() throws ServerFault {
		Template tpl = null;
		try {
			tpl = cfg.getTemplate("list-print.ftl");
		} catch (IOException e) {
			throw new ServerFault(e);
		}
		ResourceBundle rb = ResourceBundle.getBundle("lang/translation", l);

		Map<String, Object> model = new HashMap<>();

		model.put("msg", new MessageResolverMethod(rb, l));

		Calendar c = Calendar.getInstance(GMTtimezone);
		c.setTimeZone(timezone);
		Date now = c.getTime();

		SimpleDateFormat footerDateFormat = new SimpleDateFormat(
				userSettings.get("date") + ", " + userSettings.get("timeformat"), l);

		footerDateFormat.setTimeZone(timezone);
		model.put("printDate", footerDateFormat.format(now));

		Calendar start = Calendar.getInstance(timezone);
		start.setTimeInMillis(new BmDateTimeWrapper(options.dateBegin).toTimestamp(timezone.getID()));
		// not done in other print
		// need it here ?
		// FIXME question
		// options.dateBegin + start.get(Calendar.ZONE_OFFSET) +
		// start.get(Calendar.DST_OFFSET));

		Calendar end = Calendar.getInstance(timezone);
		end.setTimeInMillis(new BmDateTimeWrapper(options.dateEnd).toTimestamp(timezone.getID()));
		// FIXME question same as start
		// end.setTimeInMillis(options.getDateEnd() - 1 +
		// end.get(Calendar.ZONE_OFFSET) + end.get(Calendar.DST_OFFSET));

		String date = dateFormat.format(start.getTime());
		if (!date.equals(dateFormat.format(end.getTime()))) {
			date += " - " + dateFormat.format(end.getTime());
		}

		model.put("date", date);

		List<CalDisplay> cals = new ArrayList<>();
		for (CalendarMetadata cal : options.calendars) {
			CalInfo calInfo = calInfos.get(cal.uid);
			cals.add(new CalDisplay(calInfo.name, calInfo.color));
		}
		model.put("cals", cals);
		model.put("options", options);

		model.put("multi", cals.size() > 1);

		addEvents(model);
		StringWriter sw = new StringWriter();
		try {
			tpl.process(model, sw);
		} catch (TemplateException | IOException e) {
			throw new ServerFault(e);
		}

		htmlDocument = sw.toString();
	}

	private void addEvents(Map<String, Object> model) {
		List<CalDay> days = new ArrayList<>();
		for (Long key : ocs.keySet()) {
			Calendar c = Calendar.getInstance(timezone);
			c.setTimeInMillis(key);

			List<ItemContainerValue<VEvent>> occurrences = ocs.get(key);
			Collections.sort(occurrences, new EventComparator(timezone.getID()));

			List<PrintEvent> dayEvents = new ArrayList<>();
			for (ItemContainerValue<VEvent> o : occurrences) {
					PrintEvent pe = addEvent(o);
					dayEvents.add(pe);
			}

			CalDay cd = new CalDay(dateFormat.format(c.getTime()), dayEvents);
			days.add(cd);
		}

		model.put("days", days);
	}

	protected PrintEvent addEvent(ItemContainerValue<VEvent> item) {
		VEvent e = item.value;

		boolean isAttendee = isAttendee(e);
		List<Attendee> attendees = new ArrayList<>(e.attendees);

		boolean priv = true;
		if (!(e.classification == VEvent.Classification.Private
				&& (e.organizer != null && securityContext.getSubject().equals(e.organizer.uri)) && !isAttendee)) {
			priv = false;
		}

		String title = getTitle(e);
		String color = this.getCalInfo(item.containerUid).color;
		String timeSlot = null;
		if (e.allDay()) {
			timeSlot = lang.getProperty("allday");
		} else {

			Calendar begin = Calendar.getInstance(timezone);
			begin.setTimeInMillis(new BmDateTimeWrapper(e.dtstart).toTimestamp(timezone.getID()));
			Calendar end = (Calendar) begin.clone();
			end.add(Calendar.SECOND, duration(e).intValue());
			timeSlot = timeFormat.format(begin.getTime()) + "-" + timeFormat.format(end.getTime());
		}

		List<CalDisplay> atts = new ArrayList<>();
		List<CalDisplay> fatts = new ArrayList<>();

		Attendee organizerToChair = organizerToChair(item.value.organizer);
		if (organizerToChair != null) {
			attendees.add(organizerToChair);
		}
		if (attendees.isEmpty()) {
			attendees.add(calendarToChair(this.getCalInfo(item.containerUid)));
		}
		CalDisplay chair = null;
		for (Attendee a : attendees) {
			if (securityContext.getSubject().equals(a.uri) // FIXME
					|| !priv) {
				String attendeeColor = null;
				CalInfo calInfo = getCalInfo(a.uri);
				if (calInfo != null) {
					attendeeColor = calInfo.color;
				}
				String name = a.commonName;
				if (name == null) {
					name = a.mailto;
				}
				CalDisplay calDisplay = new CalDisplay(name, attendeeColor);

				if (a.role != null) {
					switch (a.role) {
					case RequiredParticipant:
						atts.add(calDisplay);
						if (chair == null) {
							chair = calDisplay;
						}
						break;
					case OptionalParticipant:
						fatts.add(calDisplay);
						break;
					case Chair:
						chair = calDisplay;
					default:
						break;

					}
				}
			}
		}

		String description = null;
		if (!priv && e.description != null && e.description.length() > 0) {
			StringWriter dw = new StringWriter();
			Tidy tidy = new Tidy();
			tidy.setInputEncoding("UTF-8");
			tidy.setOutputEncoding("UTF-8");
			tidy.setWraplen(Integer.MAX_VALUE);
			tidy.setPrintBodyOnly(true);
			tidy.setXmlOut(true);
			tidy.setSmartIndent(true);
			tidy.parseDOM(new StringReader(e.description), dw);
			description = dw.toString();

		}

		return new PrintEvent(timeSlot, color, title, options.showDetail, e, chair, atts, fatts, description);
	}

	private Attendee organizerToChair(Organizer organizer) {
		if (organizer == null) {
			return null;
		}
		Attendee attendee = new Attendee();
		attendee.role = Role.Chair;
		attendee.commonName = organizer.commonName;
		attendee.uri = organizer.uri;
		if (attendee.uri == null || attendee.uri.isEmpty()) {
			attendee.uri = organizer.dir;
		}
		attendee.mailto = organizer.mailto;
		return attendee;
	}


	private Attendee calendarToChair(CalInfo calendar) {
		Attendee attendee = new Attendee();
		attendee.role = Role.Chair;
		attendee.commonName = calendar.name;
		attendee.uri = calendar.uid;
		return attendee;
	}
	
	private CalInfo getCalInfo(String uri) {
		if (uri == null) {
			return null;
		}
		if (uri.contains("/")) {
			uri = uri.substring(uri.lastIndexOf("/") + 1);
		}
		CalInfo calInfo = calInfos.get(uri);
		if (null == calInfo) {
			calInfo = calInfos.get("calendar:" + uri);
			if (null == calInfo) {
				calInfo = calInfos.get("calendar:Default:" + uri);
			}
		}
		return calInfo;
	}

	@Override
	public byte[] sendPNGString() throws ServerFault {

		ByteArrayOutputStream os = new ByteArrayOutputStream();

		ITextRenderer renderer = new ITextRenderer();
		renderer.setDocumentFromString(htmlDocument);
		renderer.layout();
		try {
			renderer.createPDF(os, true);
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServerFault(e);
		}

		ByteArrayOutputStream ios = new ByteArrayOutputStream();
		try {
			PDDocument document = PDDocument.load(new ByteArrayInputStream(os.toByteArray()));
			PDPage page1 = (PDPage) document.getDocumentCatalog().getAllPages().get(0);
			BufferedImage bim = page1.convertToImage(BufferedImage.TYPE_INT_RGB, 300);

			ImageIO.write(bim, "png", ios);
		} catch (IOException e) {
			throw new ServerFault(e);
		}

		return ios.toByteArray();
	}

	@Override
	public byte[] sendPDFString() throws ServerFault {

		ByteArrayOutputStream os = new ByteArrayOutputStream();

		ITextRenderer renderer = new ITextRenderer();
		renderer.setDocumentFromString(htmlDocument);
		renderer.layout();
		try {
			renderer.createPDF(os, true);
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServerFault(e);
		}
		return os.toByteArray();
	}

	public byte[] sendSVGString() throws ServerFault {
		byte[] ret = null;
		try {

			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(htmlDocument));
			Document doc = db.parse(is);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TransformerFactory fac = TransformerFactory.newInstance();
			Transformer tf = fac.newTransformer();
			tf.setOutputProperty(OutputKeys.INDENT, "no");
			Source input = new DOMSource(doc.getParentNode());
			Result output = new StreamResult(out);
			tf.transform(input, output);
			ret = out.toByteArray();
		} catch (Exception t) {
			throw new ServerFault(t.getMessage(), t);
		}
		return ret;
	}

}
