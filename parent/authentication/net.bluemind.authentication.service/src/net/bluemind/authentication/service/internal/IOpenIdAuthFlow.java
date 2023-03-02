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

import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.AccessTokenInfo;
import net.bluemind.authentication.service.OpenIdContext;
import net.bluemind.system.api.ExternalSystem;

public interface IOpenIdAuthFlow {

	public AccessTokenInfo initalizeOpenIdAuthentication(ExternalSystem extSystem);

	public JsonObject getAccessTokenByCode(String code, OpenIdContext openIdContext);

	public void storeAccessToken(String userUid, String systemIdentifier, JsonObject jwtToken);

	public void storeRefreshToken(OpenIdContext openIdContext, String refreshToken);

}
