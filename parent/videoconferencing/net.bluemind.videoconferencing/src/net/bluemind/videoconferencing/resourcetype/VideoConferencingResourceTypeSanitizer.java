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

import java.util.List;
import java.util.Optional;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property.Type;
import net.bluemind.videoconferencing.api.IVideoConferencing;

public class VideoConferencingResourceTypeSanitizer implements ISanitizer<ResourceTypeDescriptor> {

	private static final List<IVideoConferencing> providers = getProviders();

	@Override
	public void create(ResourceTypeDescriptor obj) {
	}

	@Override
	public void update(ResourceTypeDescriptor current, ResourceTypeDescriptor obj) {

		providers.forEach(provider -> {
			Optional<Property> type = current.properties.stream().filter(
					p -> p.id.equals(provider.id() + "-type") && p.type == Type.String && p.label.equals("Type"))
					.findFirst();

			if (type.isPresent()) {
				type = obj.properties.stream().filter(
						p -> p.id.equals(provider.id() + "-type") && p.type == Type.String && p.label.equals("Type"))
						.findFirst();
				if (!type.isPresent()) {
					Property p = new Property();
					p.id = provider.id() + "-type";
					p.label = "Type";
					p.type = Property.Type.String;
					obj.properties.add(p);
				}
			}

		});

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

	private static List<IVideoConferencing> getProviders() {
		RunnableExtensionLoader<IVideoConferencing> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.videoconferencing", "provider", "provider", "impl");
	}

}
