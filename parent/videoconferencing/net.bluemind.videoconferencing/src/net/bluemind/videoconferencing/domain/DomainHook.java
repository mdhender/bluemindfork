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
package net.bluemind.videoconferencing.domain;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.videoconferencing.api.IVideoConferenceUid;

public class DomainHook extends DomainHookAdapter {

	private static final Logger logger = LoggerFactory.getLogger(DomainHook.class);

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {

		IResourceTypes resources = context.su().provider().instance(IResourceTypes.class, domain.uid);
		logger.info("create video conferencing resource type for domain {} : {}", domain.uid, IVideoConferenceUid.UID);

		ResourceTypeDescriptor resourceType = ResourceTypeDescriptor.create("Video Conferencing");
		resourceType.properties = new ArrayList<>();
		Property p = new Property();
		p.id = IVideoConferenceUid.TYPE;
		p.label = "Type";
		p.type = Property.Type.String;
		resourceType.properties.add(p);

		resources.create(IVideoConferenceUid.UID, resourceType);

		// todo set camera icon

	}

}
