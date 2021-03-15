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
package net.bluemind.videoconferencing.service.resourcehook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.hook.IResourceHook;
import net.bluemind.videoconferencing.api.IVideoConferenceUids;

public class VideoConferencingResourceHook implements IResourceHook {
	final static Logger logger = LoggerFactory.getLogger(VideoConferencingResourceHook.class);

	@Override
	public void onBeforeDelete(BmContext context, ItemValue<ResourceDescriptor> resource) throws ServerFault {
		if (!IVideoConferenceUids.RESOURCETYPE_UID.equals(resource.value.typeIdentifier)) {
			return;
		}

		// delete settings container
		try {
			context.provider().instance(IContainers.class).delete(resource.uid + "-settings-container");
		} catch (ServerFault e) {
			logger.warn(e.getMessage(), e);
		}
	}

}
