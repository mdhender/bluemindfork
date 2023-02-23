/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Required;

@BMApi(version = "3")
public class SmimeCertClient {

	@Required
	public String serialNumber;
	@Required
	public String issuer;

	public static SmimeCertClient create(String serialNumber, String issuer) {
		SmimeCertClient revocation = new SmimeCertClient();
		revocation.serialNumber = serialNumber;
		revocation.issuer = issuer;
		return revocation;
	}

	@Override
	public String toString() {
		return "SmimeCertClient [serialNumber=" + serialNumber + ", issuer=" + issuer + "]";
	}

}
