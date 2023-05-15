/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.api.auth;

public enum AuthDomainProperties {
	AUTH_TYPE, //

	CAS_URL, //

	KRB_AD_DOMAIN, KRB_AD_IP, KRB_KEYTAB, //

	OPENID_HOST, OPENID_REALM, OPENID_CLIENT_ID, OPENID_CLIENT_SECRET, OPENID_AUTHORISATION_ENDPOINT,
	OPENID_TOKEN_ENDPOINT, OPENID_END_SESSION_ENDPOINT, OPENID_JWKS_URI, OPENID_ISSUER;
}
