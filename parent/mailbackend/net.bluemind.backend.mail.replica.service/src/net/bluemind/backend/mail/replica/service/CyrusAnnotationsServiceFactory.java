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
package net.bluemind.backend.mail.replica.service;

import net.bluemind.backend.mail.replica.api.ICyrusReplicationAnnotations;
import net.bluemind.backend.mail.replica.persistence.AnnotationStore;
import net.bluemind.backend.mail.replica.service.internal.CyrusAnnotationsService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class CyrusAnnotationsServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<ICyrusReplicationAnnotations> {

	@Override
	public Class<ICyrusReplicationAnnotations> factoryClass() {
		return ICyrusReplicationAnnotations.class;
	}

	@Override
	public ICyrusReplicationAnnotations instance(BmContext context, String... params) throws ServerFault {
		AnnotationStore as = new AnnotationStore(context.getDataSource());
		return new CyrusAnnotationsService(context, as);
	}

}
