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
package net.bluemind.core.validator.tests;

import org.junit.Assert;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;

public class CalledValidator implements IValidator<Called> {
	private final BmContext context;

	/**
	 * @param context
	 */
	public CalledValidator(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(Called obj) throws ServerFault {
		Assert.assertEquals("test", context.getSecurityContext().getSubject());

		if (obj.name == null) {
			throw new ServerFault("null", ErrorCode.INVALID_PARAMETER);
		}
	}

	@Override
	public void update(Called previous, Called obj) throws ServerFault {
		Assert.assertEquals("test", context.getSecurityContext().getSubject());

		if (!previous.name.equals(obj.name)) {
			throw new ServerFault("name must not change");
		}
	}
}
