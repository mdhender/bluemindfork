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

import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.domain.api.Domain;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class DomainSanitizerFactory implements ISanitizerFactory<Domain> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sanitizer.ISanitizerFactory#support()
	 */
	@Override
	public Class<Domain> support() {
		return Domain.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sanitizer.ISanitizerFactory#create(net.bluemind.core
	 * .rest.BmContext)
	 */
	@Override
	public ISanitizer<Domain> create(BmContext context, Container container) {
		return new DomainSanitizer(context);
	}

}
