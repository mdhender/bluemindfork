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

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;

public class CalendarDescriptorValidator implements IValidator<CalendarDescriptor> {

	public static class Factory implements IValidatorFactory<CalendarDescriptor> {

		@Override
		public Class<CalendarDescriptor> support() {
			return CalendarDescriptor.class;
		}

		@Override
		public IValidator<CalendarDescriptor> create(BmContext context) {
			return new CalendarDescriptorValidator(context);
		}

	}

	public CalendarDescriptorValidator(BmContext context) {
	}

	@Override
	public void create(CalendarDescriptor obj) throws ServerFault {
		validate(obj);
	}

	@Override
	public void update(CalendarDescriptor current, CalendarDescriptor obj) throws ServerFault {
		validate(obj);
		if (!current.domainUid.equals(obj.domainUid)) {
			throw new ServerFault("cannot change domain of a calendar", ErrorCode.INVALID_PARAMETER);
		}

		if (!current.owner.equals(obj.owner)) {
			throw new ServerFault("cannot change owner of a calendar", ErrorCode.INVALID_PARAMETER);
		}
	}

	private void validate(CalendarDescriptor obj) throws ServerFault {
		ParametersValidator.notNull(obj);
		ParametersValidator.notNullAndNotEmpty(obj.name);
		ParametersValidator.notNullAndNotEmpty(obj.owner);
		ParametersValidator.notNullAndNotEmpty(obj.domainUid);
		ParametersValidator.notNull(obj.settings);
	}

}
