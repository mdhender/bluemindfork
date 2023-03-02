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
package net.bluemind.authentication.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class AccessTokenInfo {

	public TokenStatus status;
	public String externalAuthEndPointUrl;
	public String internalRedirectUrl;
	public String applicationId;
	public String state;
	public String codeChallenge;
	public String codeChallengeMethod;
	public String responseType;
	public String scope = "openid";
	public String url;

	@BMApi(version = "3")
	public enum TokenStatus {
		NO_TOKEN_NEEDED, TOKEN_OK, TOKEN_NOT_VALID
	}

	public static AccessTokenInfo noTokenNeeded() {
		AccessTokenInfo info = new AccessTokenInfo();
		info.status = TokenStatus.NO_TOKEN_NEEDED;
		return info;
	}

	public static AccessTokenInfo tokenValid() {
		AccessTokenInfo info = new AccessTokenInfo();
		info.status = TokenStatus.TOKEN_OK;
		return info;
	}

	public static AccessTokenInfo tokenNotValid(String externalAuthEndPointUrl, String internalRedirectUrl,
			String applicationId, String state, String codeChallenge, String codeChallengeMethod, String responseType,
			String url) {
		AccessTokenInfo info = new AccessTokenInfo();
		info.status = TokenStatus.TOKEN_NOT_VALID;
		info.externalAuthEndPointUrl = externalAuthEndPointUrl;
		info.internalRedirectUrl = internalRedirectUrl;
		info.applicationId = applicationId;
		info.state = state;
		info.codeChallenge = codeChallenge;
		info.codeChallengeMethod = codeChallengeMethod;
		info.responseType = responseType;
		info.url = url;
		return info;
	}

}