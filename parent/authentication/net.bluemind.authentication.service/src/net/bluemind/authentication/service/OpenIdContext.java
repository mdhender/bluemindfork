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
package net.bluemind.authentication.service;

import net.bluemind.system.api.ExternalSystem.AuthKind;

public class OpenIdContext {

	public final String state;
	public final String codeChallenge;
	public final String codeChallengeMethod;
	public final String domain;
	public final String userUid;
	public final String systemIdentifier;
	public final String tokenEndpoint;
	public final String internalRedirectUrl;
	public final String applicationId;
	public final String clientSecret;
	public final String codeVerifier;
	public final AuthKind authKind;

	public OpenIdContext(String state, String codeChallenge, String codeChallengeMethod, String domain, String userUid,
			String systemIdentifier, String tokenEndpoint, String internalRedirectUrl, String applicationId,
			String clientSecret, String codeVerifier, AuthKind authKind) {
		this.state = state;
		this.codeChallenge = codeChallenge;
		this.codeChallengeMethod = codeChallengeMethod;
		this.domain = domain;
		this.userUid = userUid;
		this.systemIdentifier = systemIdentifier;
		this.tokenEndpoint = tokenEndpoint;
		this.internalRedirectUrl = internalRedirectUrl;
		this.applicationId = applicationId;
		this.clientSecret = clientSecret;
		this.codeVerifier = codeVerifier;
		this.authKind = authKind;
	}

}
