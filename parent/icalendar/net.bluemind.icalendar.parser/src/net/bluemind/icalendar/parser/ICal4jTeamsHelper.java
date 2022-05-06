/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.icalendar.parser;

import org.apache.commons.lang.StringUtils;

import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.videoconferencing.utils.TeamsHeaders;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.property.XProperty;

public class ICal4jTeamsHelper {

	public static void parseTeamsToBm(ICalendarElement iCalendarElement, CalendarComponent cc) {
		Property xSkypeMeetingUrl = cc.getProperty(TeamsHeaders.X_MICROSOFT_SKYPETEAMSMEETINGURL);
		if (xSkypeMeetingUrl != null) {
			iCalendarElement.conference = xSkypeMeetingUrl.getValue();
			iCalendarElement.conferenceId = TeamsHeaders.MICROSOFT_TEAMS_CONFERENCE_ID;
		}

		Property xMSOnlineMeetingInformation = cc.getProperty(TeamsHeaders.X_MICROSOFT_ONLINEMEETINGINFORMATION);
		if (xMSOnlineMeetingInformation != null) {
			iCalendarElement.conferenceConfiguration.put(TeamsHeaders.X_MICROSOFT_ONLINEMEETINGINFORMATION,
					net.fortuna.ical4j.util.Strings.unescape(xMSOnlineMeetingInformation.getValue()));
		}

		Property xMSSchedulingServiceUpdateUrl = cc.getProperty(TeamsHeaders.X_MICROSOFT_SCHEDULINGSERVICEUPDATEURL);
		if (xMSSchedulingServiceUpdateUrl != null) {
			iCalendarElement.conferenceConfiguration.put(TeamsHeaders.X_MICROSOFT_SCHEDULINGSERVICEUPDATEURL,
					net.fortuna.ical4j.util.Strings.unescape(xMSSchedulingServiceUpdateUrl.getValue()));
		}

		Property xMSSkypeTeamsProperties = cc.getProperty(TeamsHeaders.X_MICROSOFT_SKYPETEAMSPROPERTIES);
		if (xMSSkypeTeamsProperties != null) {
			iCalendarElement.conferenceConfiguration.put(TeamsHeaders.X_MICROSOFT_SKYPETEAMSPROPERTIES,
					net.fortuna.ical4j.util.Strings.unescape(xMSSkypeTeamsProperties.getValue()));
		}

		Property xMSOnlineMeetingConfLink = cc.getProperty(TeamsHeaders.X_MICROSOFT_ONLINEMEETINGCONFLINK);
		if (xMSOnlineMeetingConfLink != null) {
			iCalendarElement.conferenceConfiguration.put(TeamsHeaders.X_MICROSOFT_ONLINEMEETINGCONFLINK,
					net.fortuna.ical4j.util.Strings.unescape(xMSOnlineMeetingConfLink.getValue()));
		}
	}

	public static void parseTeamsToICS(PropertyList<XProperty> properties, ICalendarElement iCalendarElement) {
		if (TeamsHeaders.MICROSOFT_TEAMS_CONFERENCE_ID.equals(iCalendarElement.conferenceId)) {

			if (StringUtils.isNotBlank(iCalendarElement.conference)) {
				properties
						.add(new XProperty(TeamsHeaders.X_MICROSOFT_SKYPETEAMSMEETINGURL, iCalendarElement.conference));
			}

			String xMsOnlineMeetingInformation = iCalendarElement.conferenceConfiguration
					.get(TeamsHeaders.X_MICROSOFT_ONLINEMEETINGINFORMATION);
			if (xMsOnlineMeetingInformation != null) {
				properties.add(
						new XProperty(TeamsHeaders.X_MICROSOFT_ONLINEMEETINGINFORMATION, xMsOnlineMeetingInformation));
			}

			String xMSSchedulingServiceUpdateUrl = iCalendarElement.conferenceConfiguration
					.get(TeamsHeaders.X_MICROSOFT_SCHEDULINGSERVICEUPDATEURL);
			if (xMSSchedulingServiceUpdateUrl != null) {
				properties.add(new XProperty(TeamsHeaders.X_MICROSOFT_SCHEDULINGSERVICEUPDATEURL,
						xMSSchedulingServiceUpdateUrl));
			}

			String xMSSkyeTeamsProperties = iCalendarElement.conferenceConfiguration
					.get(TeamsHeaders.X_MICROSOFT_SKYPETEAMSPROPERTIES);
			if (xMSSkyeTeamsProperties != null) {
				properties.add(new XProperty(TeamsHeaders.X_MICROSOFT_SKYPETEAMSPROPERTIES, xMSSkyeTeamsProperties));
			}

			String xMSOnlineMeetingConfLink = iCalendarElement.conferenceConfiguration
					.get(TeamsHeaders.X_MICROSOFT_ONLINEMEETINGCONFLINK);
			if (xMSOnlineMeetingConfLink != null) {
				properties.add(new XProperty(TeamsHeaders.X_MICROSOFT_ONLINEMEETINGCONFLINK, xMSOnlineMeetingConfLink));
			}
		}
	}

}
