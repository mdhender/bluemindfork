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
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;

public class ResourceTypesValidator {

	public void validate(ResourceTypeDescriptor descriptor) throws ServerFault {
		ParametersValidator.notNull(descriptor);
		ParametersValidator.notNull(descriptor.properties);
		ParametersValidator.notNullAndNotEmpty(descriptor.label);
		for (ResourceTypeDescriptor.Property prop : descriptor.properties) {
			valideProperty(prop);
		}

	}

	private void valideProperty(Property prop) throws ServerFault {
		ParametersValidator.notNullAndNotEmpty(prop.id);
		ParametersValidator.notNullAndNotEmpty(prop.label);
		ParametersValidator.notNull(prop.type);
	}

}
