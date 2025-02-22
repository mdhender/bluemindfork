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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Strings;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.service.internal.IInCoreDomainSettings;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.videoconferencing.api.VideoConference;

public abstract class TemplateBasedVideoConferencingProvider {

	private static final VideoConferencingTemplateHelper templateHelper = new VideoConferencingTemplateHelper();

	public VideoConference getConferenceInfo(BmContext context, Map<String, String> resourceSettings,
			ItemValue<ResourceDescriptor> resource, VEvent vevent) {
		return getConferenceInfo(context, resourceSettings, resource, vevent, new HashMap<>());
	}

	public VideoConference getConferenceInfo(BmContext context, Map<String, String> resourceSettings,
			ItemValue<ResourceDescriptor> resource, VEvent vevent, Map<String, String> props) {

		String conference = vevent.conference;
		String conferenceId = vevent.conferenceId;

		if (Strings.isNullOrEmpty(conference)) {
			String baseUrl = resourceSettings.get("url");
			if (!baseUrl.startsWith("http")) {
				baseUrl = "https://" + baseUrl;
			}
			if (!baseUrl.endsWith("/")) {
				baseUrl += "/";
			}
			if (Strings.isNullOrEmpty(conferenceId)) {
				conferenceId = UUID.randomUUID().toString();
			}
			conference = baseUrl + conferenceId;
		}
		String description = templateHelper.processTemplate(context, resource, vevent, conference, props);

		return new VideoConference(conferenceId, conference, description);
	}

	public abstract void deleteConference(BmContext context, Map<String, String> resourceSettings, String conferenceId);

	public void setExternalUrl(BmContext context, String domainUid, Map<String, String> resourceSettings) {
		IServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		String url = Optional
				.ofNullable(provider.instance(IInCoreDomainSettings.class, domainUid).getExternalUrl()
						.orElseGet(() -> provider.instance(ISystemConfiguration.class).getValues().values
								.get(SysConfKeys.external_url.name())))
				.orElseThrow(() -> new ServerFault("External URL missing"));

		resourceSettings.put("url", url + "/visio/");
	}

}
