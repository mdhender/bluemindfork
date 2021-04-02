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
package net.bluemind.calendar.service.internal;

import java.util.HashMap;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;

public class CalendarDescriptorSanitizer implements ISanitizer<CalendarDescriptor> {

	public static class Factory implements ISanitizerFactory<CalendarDescriptor> {

		@Override
		public Class<CalendarDescriptor> support() {
			return CalendarDescriptor.class;
		}

		@Override
		public ISanitizer<CalendarDescriptor> create(BmContext context, Container container) {
			return new CalendarDescriptorSanitizer(context);
		}

	}

	private BmContext context;

	public CalendarDescriptorSanitizer(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(CalendarDescriptor obj) throws ServerFault {
		if (obj.settings == null) {
			obj.settings = new HashMap<>();
		}

		if (obj.owner == null) {
			obj.owner = context.getSecurityContext().getSubject();
			obj.domainUid = context.getSecurityContext().getContainerUid();
		}

		if (obj.domainUid == null) {
			obj.domainUid = context.getSecurityContext().getContainerUid();
		}
	}

	@Override
	public void update(CalendarDescriptor current, CalendarDescriptor obj) throws ServerFault {
		if (obj.settings == null) {
			obj.settings = current.settings;
		}

		if (obj.domainUid == null) {
			obj.domainUid = current.domainUid;
		}

		if (obj.owner == null) {
			obj.owner = current.owner;
		}

	}

}
