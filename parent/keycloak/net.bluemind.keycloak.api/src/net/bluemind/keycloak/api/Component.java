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

public abstract class Component {
	public static final String PROVIDER_TYPE = "org.keycloak.storage.UserStorageProvider";

	@BMApi(version = "3")
	public enum ProviderId {
		Bluemind, kerberos;
	}

	@BMApi(version = "3")
	public enum CachePolicy {
		DEFAULT, EVICT_DAILY, EVICT_WEEKLY, MAX_LIFESPAN, NO_CACHE;
	}

	public Boolean enabled = true;

	public final ProviderId providerId;
	public final String providerType;
	public String id;
	public String parentId;
	public String name;

	public CachePolicy cachePolicy = CachePolicy.DEFAULT;

	protected Component(ProviderId providerId, String providerType) {
		this.providerId = providerId;
		this.providerType = providerType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(cachePolicy, enabled, id, name, parentId, providerId, providerType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Component other = (Component) obj;
		return cachePolicy == other.cachePolicy && Objects.equals(enabled, other.enabled)
				&& Objects.equals(id, other.id) && Objects.equals(name, other.name)
				&& Objects.equals(parentId, other.parentId) && providerId == other.providerId
				&& Objects.equals(providerType, other.providerType);
	}
}
