package net.bluemind.dav.server.proto.post;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.NS;

public class CalMultiputSaxHandler extends DefaultHandler {

	private static final Logger logger = LoggerFactory.getLogger(CalMultiputSaxHandler.class);

	private final List<VEventPut> events;
	private final StringBuilder sb;
	private boolean inEvent;
	private boolean inHref;
	private String updateHref;
	private boolean isDelete;

	public CalMultiputSaxHandler() {
		events = new LinkedList<>();
		sb = new StringBuilder(512);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("href".equals(localName) && NS.WEBDAV.equals(uri)) {
			inHref = true;
		} else if ("calendar-data".equals(localName)) {
			inEvent = true;
			sb.setLength(0);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("href".equals(localName) && NS.WEBDAV.equals(uri)) {
			inHref = false;
			updateHref = sb.toString();
			sb.setLength(0);
		} else if ("calendar-data".equals(localName)) {
			inEvent = false;
			String ics = sb.toString();
			try {
				Consumer<ItemValue<VEventSeries>> consumer = (series -> events.add(new VEventPut(series, updateHref)));
				VEventServiceHelper.parseCalendar(new ByteArrayInputStream(ics.getBytes()), Optional.empty(),
						Collections.emptyList(), consumer);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			updateHref = null;
		} else if ("delete".equals(localName) && NS.ME_COM.equals(uri)) {
			isDelete = true;
		} else if ("resource".equals(localName) && NS.ME_COM.equals(uri)) {
			if (isDelete) {
				events.add(new VEventPut(null, updateHref));
				isDelete = false;
			}
			sb.setLength(0);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (inEvent || inHref) {
			sb.append(ch, start, length);
		}
	}

	public List<VEventPut> getEvents() {
		return events;
	}

}