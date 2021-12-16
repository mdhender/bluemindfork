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
package net.bluemind.imip.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.todolist.api.VTodo;

/**
 * Informations extracted from an iTIP/iMIP message.
 * 
 * @author tom
 * 
 */
public class IMIPInfos {

	public ITIPMethod method;
	public String messageId;
	public String uid;
	public String organizerEmail;
	public int sequence;
	public Map<String, String> properties = new HashMap<>();
	public List<ICalendarElement> iCalendarElements = new ArrayList<>();
	public Map<String, Cid> cid;

	public IMIPInfos() {
	}

	public void attendees(List<Attendee> attendees) {
		iCalendarElements.forEach(element -> element.attendees = attendees);
	}

	public IMIPType type() {
		// we don't support mixed content yet
		if (iCalendarElements.isEmpty()) {
			return null;
		} else if (iCalendarElements.get(0) instanceof VEvent || iCalendarElements.get(0) instanceof VEventOccurrence) {
			return IMIPType.VEVENT;
		} else if (iCalendarElements.get(0) instanceof VTodo) {
			return IMIPType.VTODO;
		}
		return null;
	}

	public static enum IMIPType {
		VEVENT, VTODO
	}

	public IMIPInfos copy() {
		IMIPInfos infos = new IMIPInfos();
		infos.method = this.method;
		infos.messageId = this.messageId;
		infos.uid = this.uid;
		infos.organizerEmail = this.organizerEmail;
		infos.sequence = this.sequence;
		infos.properties = this.properties;
		infos.cid = this.cid;
		infos.iCalendarElements = this.iCalendarElements.stream().map(cal -> cal.copy()).collect(Collectors.toList());
		return infos;
	}

	public static class Cid {
		public final String name;
		public final String tmpFile;

		public Cid(String name, String tmpFile) {
			this.name = name;
			this.tmpFile = tmpFile;
		}
	}

	public void release() {
		if (cid != null && !cid.isEmpty()) {
			cid.values().forEach(cid -> {
				try {
					Files.delete(new File(cid.tmpFile).toPath());
				} catch (IOException e) {
				}
			});
		}
	}

}
