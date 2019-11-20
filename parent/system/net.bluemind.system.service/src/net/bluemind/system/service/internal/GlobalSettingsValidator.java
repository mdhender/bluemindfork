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
package net.bluemind.system.service.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.system.api.GlobalSettingsKeys;

public class GlobalSettingsValidator {

	public void check(Map<String, String> settings) throws ServerFault {
		try {
			String maxSizeFH = settings.get(GlobalSettingsKeys.filehosting_max_filesize.name());
			if (maxSizeFH != null) {
				int globalFHMaxSize = Integer.parseInt(maxSizeFH);
				checkFileHostingMaxSize(globalFHMaxSize);
			}
		} catch (NumberFormatException e) {
			throw new ServerFault("Filehosting max size must be numeric", ErrorCode.INVALID_PARAMETER);
		}
	}

	private void checkFileHostingMaxSize(int globalFHMaxSize) throws ServerFault {

		// if max size is 0, it means there is no max size limit defined
		if (globalFHMaxSize > 0) {
			// check if a max size defined in domain settings is incompatible with a new max
			// file global settings
			ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

			List<String> domainUids = provider.instance(IDomains.class).all().stream().map(itemDomain -> itemDomain.uid)
					.collect(Collectors.toList());

			List<String> problematicDomains = new ArrayList<String>();
			problematicDomains = domainUids.stream().filter(domainUid -> {
				String domainFHMaxSize = provider.instance(IDomainSettings.class, domainUid).get()
						.get(GlobalSettingsKeys.filehosting_max_filesize.name());
				return domainFHMaxSize != null && Integer.parseInt(domainFHMaxSize) > globalFHMaxSize;
			}).collect(Collectors.toList());

			if (problematicDomains.size() > 0) {
				String error = "Can't set global settings for Filehosting (max file size) : the value of the following domains are greater than the global one: ";
				throw new ServerFault(error.concat(String.join(", ", problematicDomains.toArray(new String[0]))),
						ErrorCode.INVALID_PARAMETER);
			}
		}
	}

}
