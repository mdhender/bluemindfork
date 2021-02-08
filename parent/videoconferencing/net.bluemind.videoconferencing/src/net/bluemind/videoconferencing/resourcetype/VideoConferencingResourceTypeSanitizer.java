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
package net.bluemind.videoconferencing.resourcetype;

import java.util.Optional;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.videoconferencing.api.IVideoConferenceUid;

public class VideoConferencingResourceTypeSanitizer implements ISanitizer<ResourceTypeDescriptor> {

	@Override
	public void create(ResourceTypeDescriptor obj) {
	}

	@Override
	public void update(ResourceTypeDescriptor current, ResourceTypeDescriptor obj) {

		Optional<Property> type = current.properties.stream().filter(p -> p.id.equals(IVideoConferenceUid.UID + "-type")
				&& p.type == ResourceTypeDescriptor.Property.Type.String && p.label.equals("Type")).findFirst();

		if (type.isPresent()) {
			type = obj.properties.stream()
					.filter(p -> p.id.equals(IVideoConferenceUid.UID + "-type")
							&& p.type == ResourceTypeDescriptor.Property.Type.String && p.label.equals("Type"))
					.findFirst();
			if (!type.isPresent()) {
				Property p = new Property();
				p.id = IVideoConferenceUid.UID + "-type";
				p.label = "Type";
				p.type = Property.Type.String;
				obj.properties.add(p);
			}
		}

	}

	public static class VideoConferencingResourceTypeSanitizerFactory
			implements ISanitizerFactory<ResourceTypeDescriptor> {

		@Override
		public Class<ResourceTypeDescriptor> support() {
			return ResourceTypeDescriptor.class;
		}

		@Override
		public ISanitizer<ResourceTypeDescriptor> create(BmContext context) {
			return new VideoConferencingResourceTypeSanitizer();
		}

	}

}
