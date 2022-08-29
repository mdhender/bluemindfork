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
package net.bluemind.domain.service.internal;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.mailbox.persistence.MailboxStore;

public class DomainSettingsMailQuotaValidator implements IValidator<DomainSettings> {

	public final static class Factory implements IValidatorFactory<DomainSettings> {
		@Override
		public Class<DomainSettings> support() {
			return DomainSettings.class;
		}

		@Override
		public IValidator<DomainSettings> create(BmContext context) {
			return new DomainSettingsMailQuotaValidator(context);
		}
	}

	private BmContext context;
	private List<String> quotaDomainKey = Arrays.asList(DomainSettingsKeys.mailbox_default_publicfolder_quota.name(),
			DomainSettingsKeys.mailbox_default_user_quota.name(),
			DomainSettingsKeys.mailbox_max_publicfolder_quota.name(), DomainSettingsKeys.mailbox_max_user_quota.name());

	public DomainSettingsMailQuotaValidator(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(DomainSettings settings) throws ServerFault {
		if (!quotaDomainKey.stream().anyMatch(settings.settings::containsKey)) {
			return;
		}

		checkQuota("user", Optional.empty(), settings, DomainSettingsKeys.mailbox_max_user_quota.name(),
				DomainSettingsKeys.mailbox_default_user_quota.name());
		checkQuota("mailshare", Optional.empty(), settings, DomainSettingsKeys.mailbox_max_publicfolder_quota.name(),
				DomainSettingsKeys.mailbox_default_publicfolder_quota.name());
	}

	@Override
	public void update(DomainSettings oldValue, DomainSettings newValue) throws ServerFault {
		if (!quotaDomainKey.stream()
				.anyMatch(key -> !StringUtils.equals(newValue.settings.get(key), oldValue.settings.get(key)))) {
			return;
		}

		checkQuota("user", Optional.of(oldValue), newValue, DomainSettingsKeys.mailbox_max_user_quota.name(),
				DomainSettingsKeys.mailbox_default_user_quota.name());
		checkQuota("mailshare", Optional.of(oldValue), newValue,
				DomainSettingsKeys.mailbox_max_publicfolder_quota.name(),
				DomainSettingsKeys.mailbox_default_publicfolder_quota.name());
	}

	private void checkQuota(String kind, Optional<DomainSettings> oldValue, DomainSettings newValue, String quotaMaxKey,
			String quotaDefaultKey) {
		int quotaMax = getQuota(newValue.settings, quotaMaxKey);
		int quotaDefault = getQuota(newValue.settings, quotaDefaultKey);

		checkQuota(kind, quotaMax, quotaDefault);

		if (oldValue.isPresent() && quotaMax != 0 && quotaMax != getQuota(oldValue.get().settings, quotaMaxKey)) {
			checkMailboxQuota(newValue.domainUid, quotaMax);
		}
	}

	private void checkMailboxQuota(String domainUid, int quotaMax) {
		try {
			if (new MailboxStore(context.getDataSource(),
					new ContainerStore(context, context.getDataSource(), SecurityContext.SYSTEM).get(domainUid))
							.isQuotaGreater(quotaMax)) {
				throw new ServerFault(String.format("At least one mailbox quota is greater than %d", quotaMax),
						ErrorCode.INVALID_PARAMETER);
			}
		} catch (SQLException e) {
			throw new ServerFault("Unable to check if new quota max is greater than already assigned", e);
		}
	}

	private void checkQuota(String kind, int quotaMax, int quotaDefault) {
		if (quotaMax != 0 && quotaMax < quotaDefault) {
			throw new ServerFault(
					String.format("Default %s quota is greater than quota max (%d > %d)", kind, quotaDefault, quotaMax),
					ErrorCode.INVALID_PARAMETER);
		}
	}

	private int getQuota(Map<String, String> settings, String keyName) {
		if (!settings.containsKey(keyName) || settings.get(keyName) == null) {
			return 0;
		}

		Integer quota = null;
		try {
			quota = Integer.valueOf(settings.get(keyName));
		} catch (NumberFormatException nfs) {
			throw new ServerFault(
					String.format("Invalid %s: %s - Must be an integer greater than 0", keyName, settings.get(keyName)),
					ErrorCode.INVALID_PARAMETER);
		}

		return quota;
	}
}
