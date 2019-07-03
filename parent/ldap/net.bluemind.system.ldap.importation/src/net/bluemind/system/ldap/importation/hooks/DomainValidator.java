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
package net.bluemind.system.ldap.importation.hooks;

import java.util.Locale;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.ldap.importation.internal.tools.LdapParametersValidator;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class DomainValidator implements IValidator<Domain> {
	private final BmContext context;

	/**
	 * @param context
	 */
	public DomainValidator(BmContext context) {
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sanitizer.ISanitizer#create(java.lang.Object)
	 */
	@Override
	public void create(Domain domain) throws ServerFault {
		LdapParametersValidator.validate(domain.properties, new Locale(context.getSecurityContext().getLang()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sanitizer.ISanitizer#sanitize(java.lang.Object)
	 */
	@Override
	public void update(Domain previous, Domain domain) throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()) {
			LdapParametersValidator.noChanges(previous.properties, domain.properties);
			return;
		}

		LdapParametersValidator.validate(domain.properties, new Locale(context.getSecurityContext().getLang()));
	}
}
