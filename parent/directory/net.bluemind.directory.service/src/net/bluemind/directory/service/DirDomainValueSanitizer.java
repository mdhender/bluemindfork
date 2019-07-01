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
package net.bluemind.directory.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.server.api.IServer;

public class DirDomainValueSanitizer implements ISanitizer<DirDomainValue<?>> {

	public static final class Factory implements ISanitizerFactory<DirDomainValue<?>> {

		@Override
		public Class<DirDomainValue<?>> support() {
			return (Class<DirDomainValue<?>>) ((Class<?>) DirDomainValue.class);
		}

		@Override
		public ISanitizer<DirDomainValue<?>> create(BmContext context) {
			return new DirDomainValueSanitizer(context);
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(DirDomainValueSanitizer.class);
	private BmContext context;

	public DirDomainValueSanitizer(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(DirDomainValue<?> obj) throws ServerFault {
		assignMailServer(obj);

	}

	@Override
	public void update(DirDomainValue<?> current, DirDomainValue<?> obj) throws ServerFault {
		assignMailServer(obj);

	}

	private void assignMailServer(DirDomainValue<?> obj) throws ServerFault {
		if (obj == null || obj.value == null) {
			return;
		}

		if (obj.value.dataLocation == null) {

			List<String> assignedServers = context.provider().instance(IServer.class, InstallationId.getIdentifier())
					.byAssignment(obj.domainUid, "mail/imap");

			if (!assignedServers.isEmpty()) {
				obj.value.dataLocation = assignedServers.get(0);
			} else {
				logger.warn("no imap server found for domain {} ", obj.domainUid);
			}
		}
	}

}
