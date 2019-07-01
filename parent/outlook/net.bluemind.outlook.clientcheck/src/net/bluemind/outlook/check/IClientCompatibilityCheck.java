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
package net.bluemind.outlook.check;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.base.RestRequest;

public interface IClientCompatibilityCheck {

	public ClientCompatibility isCompatible(RestRequest request, SecurityContext securityContext);

	public static class ClientCompatibility {
		public final boolean compatible;
		public final String message;

		public ClientCompatibility(boolean compatible, String message) {
			this.compatible = compatible;
			this.message = message;
		}

		public static ClientCompatibility Ok() {
			return new ClientCompatibility(true, null);
		}
	}

}
