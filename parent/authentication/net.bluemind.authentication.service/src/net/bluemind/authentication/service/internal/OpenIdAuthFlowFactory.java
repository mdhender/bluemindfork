/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.authentication.service.internal;

import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.ExternalSystem.AuthKind;

public class OpenIdAuthFlowFactory {

	public static IOpenIdAuthFlow getFlow(BmContext context, AuthKind kind) {
		switch (kind) {
		case OPEN_ID_PKCE:
			return new OpenIdPkceFlow(context);
		default:
			throw new IllegalArgumentException("Cannot find OpenID flow for AuthKind " + kind.name());
		}
	}

}
