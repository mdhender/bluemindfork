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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.keycloak.api;

import java.util.Objects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class BluemindProviderComponent extends Component {
	public String bmDomain;
	public String bmUrl;
	public String bmCoreToken;

	public BluemindProviderComponent() {
		super(ProviderId.Bluemind, PROVIDER_TYPE);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(bmCoreToken, bmDomain, bmUrl);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		BluemindProviderComponent other = (BluemindProviderComponent) obj;
		return Objects.equals(bmCoreToken, other.bmCoreToken) && Objects.equals(bmDomain, other.bmDomain)
				&& Objects.equals(bmUrl, other.bmUrl);
	}
}
