/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.filehosting.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.system.api.GlobalSettings;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.nginx.NginxService;
import net.bluemind.system.service.IGlobalSettingsObserver;

public class FileHostingGlobalSettingsHook implements IValidator<GlobalSettings>, IGlobalSettingsObserver {
	private static Logger logger = LoggerFactory.getLogger(FileHostingGlobalSettingsHook.class);
	private static final long DEFAULT_FILEHOSTING_SIZE = 100 * 1024 * 1024;

	public static class FileHostingGlobalSettingsValidatorFactory implements IValidatorFactory<GlobalSettings> {
		@Override
		public Class<GlobalSettings> support() {
			return GlobalSettings.class;
		}

		@Override
		public IValidator<GlobalSettings> create(BmContext context) {
			return new FileHostingGlobalSettingsHook();
		}
	}

	@Override
	public void create(GlobalSettings settings) {
		validate(settings);
	}

	@Override
	public void update(GlobalSettings previous, GlobalSettings modifications) throws ServerFault {
		validate(modifications);
	}

	@Override
	public void onUpdated(BmContext context, GlobalSettings previous, GlobalSettings updated) throws ServerFault {
		if (!updated.settings.containsKey(GlobalSettingsKeys.filehosting_max_filesize.name())) {
			return;
		}

		long fileHostingMaxSize = DEFAULT_FILEHOSTING_SIZE;
		try {
			fileHostingMaxSize = Long
					.parseLong(updated.settings.get(GlobalSettingsKeys.filehosting_max_filesize.name()));
		} catch (NumberFormatException nfs) {
			logger.warn("Invalid {} value '{}', use {} as default...",
					GlobalSettingsKeys.filehosting_max_filesize.name(),
					updated.settings.get(GlobalSettingsKeys.filehosting_max_filesize.name()), DEFAULT_FILEHOSTING_SIZE);
		}

		new NginxService().updateFileHostingMaxSize(fileHostingMaxSize);
	}

	@Override
	public void onDeleted(BmContext context, GlobalSettings previous, String key) throws ServerFault {
		if (!key.contentEquals(GlobalSettingsKeys.filehosting_max_filesize.name())) {
			return;
		}

		logger.info("Deleting key {}, use {} as default...", key, DEFAULT_FILEHOSTING_SIZE);
		new NginxService().updateFileHostingMaxSize(DEFAULT_FILEHOSTING_SIZE);
	}

	private void validate(GlobalSettings modifications) {
		if (!modifications.settings.containsKey(GlobalSettingsKeys.filehosting_max_filesize.name())) {
			return;
		}

		String maxSizeFH = modifications.settings.get(GlobalSettingsKeys.filehosting_max_filesize.name());
		try {
			checkFileHostingMaxSize(Long.parseLong(maxSizeFH));
		} catch (NumberFormatException e) {
			throw new ServerFault("Filehosting max size must be numeric", ErrorCode.INVALID_PARAMETER);
		}
	}

	private void checkFileHostingMaxSize(long globalFHMaxSize) throws ServerFault {
		if (globalFHMaxSize < 0) {
			throw new ServerFault("Filehosting max size must be greater than or equal to 0");
		}

		if (globalFHMaxSize == 0) {
			// if max size is 0, it means there is no max size limit defined
			return;
		}

		// check if a max size defined in domain settings is incompatible with a new max
		// file global settings
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		List<String> domainUids = provider.instance(IDomains.class).all().stream().map(itemDomain -> itemDomain.uid)
				.collect(Collectors.toList());

		List<String> problematicDomains = new ArrayList<String>();
		problematicDomains = domainUids.stream().filter(domainUid -> !domainUid.equals("global.virt"))
				.filter(domainUid -> {
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
