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
package net.bluemind.videoconferencing.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.videoconferencing.api.IVideoConferencing;
import net.bluemind.videoconferencing.service.internal.VideoConferencingService;

public class VideoConferencingServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IVideoConferencing> {

	@Override
	public Class<IVideoConferencing> factoryClass() {
		return IVideoConferencing.class;
	}

	@Override
	public IVideoConferencing instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}

		String domainUid = params[0];

		return new VideoConferencingService(context, domainUid);
	}

}
