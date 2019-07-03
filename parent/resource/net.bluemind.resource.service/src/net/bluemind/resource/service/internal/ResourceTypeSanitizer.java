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

import java.util.ArrayList;
import java.util.UUID;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;

public class ResourceTypeSanitizer implements ISanitizer<ResourceTypeDescriptor> {

	@Override
	public void create(ResourceTypeDescriptor obj) throws ServerFault {
		sanitize(obj);
	}

	@Override
	public void update(ResourceTypeDescriptor current, ResourceTypeDescriptor obj) throws ServerFault {
		sanitize(obj);
	}

	private void sanitize(ResourceTypeDescriptor obj) {
		if (obj.properties == null) {
			obj.properties = new ArrayList<>();
		}

		for (ResourceTypeDescriptor.Property prop : obj.properties) {
			if (prop.id == null) {
				prop.id = UUID.randomUUID().toString();
			}
		}
	}

	public static class ResourceTypeSanitizerFactory implements ISanitizerFactory<ResourceTypeDescriptor> {

		@Override
		public Class<ResourceTypeDescriptor> support() {
			return ResourceTypeDescriptor.class;
		}

		@Override
		public ISanitizer<ResourceTypeDescriptor> create(BmContext context) {
			return new ResourceTypeSanitizer();
		}

	}
}
