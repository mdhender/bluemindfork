/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.addressbook.service.internal;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;

public class AddressBookDescriptorValidator implements IValidator<AddressBookDescriptor> {

	public static class Factory implements IValidatorFactory<AddressBookDescriptor> {

		@Override
		public Class<AddressBookDescriptor> support() {
			return AddressBookDescriptor.class;
		}

		@Override
		public IValidator<AddressBookDescriptor> create(BmContext context) {
			return new AddressBookDescriptorValidator();
		}

	}

	@Override
	public void create(AddressBookDescriptor obj) throws ServerFault {
		validate(obj);
	}

	@Override
	public void update(AddressBookDescriptor oldValue, AddressBookDescriptor newValue) throws ServerFault {
		validate(newValue);
	}

	private void validate(AddressBookDescriptor obj) throws ServerFault {
		ParametersValidator.notNull(obj);
		ParametersValidator.notNullAndNotEmpty(obj.name);
		ParametersValidator.notNull(obj.settings);
	}

}
