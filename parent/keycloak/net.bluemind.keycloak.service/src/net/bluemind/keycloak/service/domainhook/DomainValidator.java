/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.keycloak.service.domainhook;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.domain.api.Domain;
import net.bluemind.keycloak.utils.KerberosConfigHelper;

public class DomainValidator implements IValidator<Domain> {
	private final BmContext context;

	public DomainValidator(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(Domain domain) {
	}

	@Override
	public void update(Domain oldValue, Domain newValue) {
		KerberosConfigHelper.checkKerberosConf(context, newValue);
	}

	public final static class Factory implements IValidatorFactory<Domain> {
		@Override
		public Class<Domain> support() {
			return Domain.class;
		}

		@Override
		public IValidator<Domain> create(BmContext context) {
			return new DomainValidator(context);
		}
	}
}
