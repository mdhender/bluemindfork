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
package net.bluemind.videoconferencing.service.template;

import java.util.Map;
import java.util.UUID;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.videoconferencing.api.VideoConference;

public abstract class TemplateBasedVideoConferencingProvider {

	private static final VideoConferencingTemplateHelper templateHelper = new VideoConferencingTemplateHelper();

	public VideoConference getConferenceInfo(BmContext context, Map<String, String> resourceSettings,
			ItemValue<ResourceDescriptor> resource, ICalendarElement vevent) {

		String conference = vevent.conference;
		if (conference == null || conference.trim().isEmpty()) {

			String baseUrl = resourceSettings.get("url");
			if (!baseUrl.startsWith("http")) {
				baseUrl = "https://" + baseUrl;
			}
			if (!baseUrl.endsWith("/")) {
				baseUrl += "/";
			}

			conference = baseUrl + UUID.randomUUID().toString();
		}
		String description = templateHelper.processTemplate(context, resource, vevent, conference);

		return new VideoConference(conference, description);
	}

}
