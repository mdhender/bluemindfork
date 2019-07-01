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
package net.bluemind.resource.service.internal;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.email.EmailHelper;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.ResourceDescriptor.PropertyValue;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;

public class ResourceValidator {

	public void validate(ResourceDescriptor descriptor) throws ServerFault {
		ParametersValidator.notNull(descriptor);
		ParametersValidator.notNull(descriptor.properties);
		ParametersValidator.notNullAndNotEmpty(descriptor.label);
		ParametersValidator.notNullAndNotEmpty(descriptor.typeIdentifier);
		ParametersValidator.notEmpty(descriptor.emails);

		ParametersValidator.notNullAndNotEmpty(descriptor.dataLocation);
		ParametersValidator.notNull(descriptor.emails);
		EmailHelper.validate(descriptor.emails);

		for (ResourceDescriptor.PropertyValue prop : descriptor.properties) {
			validePropertyValue(prop);
		}

	}

	private void validePropertyValue(ResourceDescriptor.PropertyValue prop) throws ServerFault {
		ParametersValidator.notNullAndNotEmpty(prop.propertyId);
	}

	public void validatePropertiesValue(ResourceDescriptor descriptor, ResourceTypeDescriptor type) throws ServerFault {
		for (PropertyValue prop : descriptor.properties) {
			Property p = type.property(prop.propertyId);
			if (p == null) {
				continue;
			}

			if (prop.value == null || prop.value.length() == 0) {
				continue;
			}

			switch (p.type) {
			case Boolean:
				if (!prop.value.equals("true") && !prop.value.equals("false")) {
					throw new ServerFault("property " + prop.propertyId + " not valid : " + prop.value);

				}
				break;
			case Number:
				try {
					Long.parseLong(prop.value);
				} catch (NumberFormatException e) {
					throw new ServerFault("property " + prop.propertyId + " not valid : " + prop.value);
				}
				break;
			case String:
			default:
				break;
			}
		}
	}
}
