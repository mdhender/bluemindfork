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

import java.util.HashMap;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;

public class AddressBookDescriptorSanitizer implements ISanitizer<AddressBookDescriptor> {

	public static class Factory implements ISanitizerFactory<AddressBookDescriptor> {

		@Override
		public Class<AddressBookDescriptor> support() {
			return AddressBookDescriptor.class;
		}

		@Override
		public ISanitizer<AddressBookDescriptor> create(BmContext context, Container container) {
			return new AddressBookDescriptorSanitizer();
		}

	}

	@Override
	public void create(AddressBookDescriptor obj) throws ServerFault {
		sanitize(obj);
	}

	@Override
	public void update(AddressBookDescriptor current, AddressBookDescriptor obj) throws ServerFault {
		sanitize(obj);
	}

	private void sanitize(AddressBookDescriptor obj) throws ServerFault {
		if (obj.settings == null) {
			obj.settings = new HashMap<>();
		}
	}

}
