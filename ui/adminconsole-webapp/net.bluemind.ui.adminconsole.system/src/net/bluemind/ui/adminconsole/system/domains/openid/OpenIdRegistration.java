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
package net.bluemind.ui.adminconsole.system.domains.openid;

public class OpenIdRegistration {

	public String systemIdentifier;
	public String endpoint;
	public String applicationId;
	public String applicationSecret;
	public String tokenEndpoint;

	public OpenIdRegistration(String systemIdentifier, String endpoint, String applicationId, String applicationSecret,
			String tokenEndpoint) {
		this.systemIdentifier = systemIdentifier;
		this.endpoint = endpoint;
		this.applicationId = applicationId;
		this.applicationSecret = applicationSecret;
		this.tokenEndpoint = tokenEndpoint;
	}

}
