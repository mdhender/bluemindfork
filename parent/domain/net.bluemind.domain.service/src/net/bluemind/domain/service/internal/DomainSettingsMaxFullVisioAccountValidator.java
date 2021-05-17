/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.role.api.BasicRoles;

public class DomainSettingsMaxFullVisioAccountValidator extends DomainSettingsMaxAccountValidator
		implements IValidator<DomainSettings> {

	public final static class Factory implements IValidatorFactory<DomainSettings> {

		@Override
		public Class<DomainSettings> support() {
			return DomainSettings.class;
		}

		@Override
		public IValidator<DomainSettings> create(BmContext context) {
			return new DomainSettingsMaxFullVisioAccountValidator(context);
		}

	}

	private BmContext context;

	public DomainSettingsMaxFullVisioAccountValidator(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(DomainSettings settings) throws ServerFault {
		super.create(context, settings, DomainSettingsKeys.domain_max_fullvisio_accounts.name(),
				BasicRoles.ROLE_DOMAIN_MAX_VALUES);
	}

	@Override
	public void update(DomainSettings oldValue, DomainSettings newValue) throws ServerFault {
		super.update(context, oldValue, newValue, DomainSettingsKeys.domain_max_fullvisio_accounts.name(),
				BasicRoles.ROLE_DOMAIN_MAX_VALUES);
	}

}
