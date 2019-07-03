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
package net.bluemind.core.sanitizer.tests;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;

public class CalledSanitizer implements ISanitizer<Called> {
	private final BmContext context;

	/**
	 * @param context
	 */
	public CalledSanitizer(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(Called obj) throws ServerFault {
		obj.name = "sanitized-" + obj.name + " context-" + context.getSecurityContext().getSubject();
	}

	@Override
	public void update(Called previous, Called obj) throws ServerFault {
		obj.name = "sanitized-" + obj.name + "-" + previous.name + " context-"
				+ context.getSecurityContext().getSubject();
	}
}
