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
package net.bluemind.videoconferencing.service.calendar;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.videoconferencing.api.IVideoConferencing;

public class VEventVideoConferencingSanitizer implements ISanitizer<VEventSeries> {

	private IVideoConferencing videoConferencingService;
	private BmContext context;
	private Container container;

	public VEventVideoConferencingSanitizer(BmContext context, Container container) {
		this.context = context;
		this.container = container;
		videoConferencingService = context.provider().instance(IVideoConferencing.class,
				context.getSecurityContext().getContainerUid());

	}

	@Override
	public void create(VEventSeries evt) {
		if (isMasterVersionAndHasAttendees(evt)) {
			videoConferencingService.add(evt.main);
		}
	}

	@Override
	public void update(VEventSeries old, VEventSeries current) {
		if (isMasterVersionAndHasAttendees(old)) {
			videoConferencingService.update(old.main, current.main);
		}
	}

	// mostly a copy from VEventSeriesSanitizer
	private boolean isMasterVersionAndHasAttendees(final VEventSeries evt) throws ServerFault {
		return evt.meeting() && evt.master(context.getSecurityContext().getContainerUid(), container.owner);
	}

	public static class Factory implements ISanitizerFactory<VEventSeries> {

		@Override
		public Class<VEventSeries> support() {
			return VEventSeries.class;
		}

		@Override
		public ISanitizer<VEventSeries> create(BmContext context, Container container) {
			return new VEventVideoConferencingSanitizer(context, container);
		}

	}

}
