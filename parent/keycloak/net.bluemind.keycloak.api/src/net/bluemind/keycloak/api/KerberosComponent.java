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
public class KerberosComponent extends Component {
	public String kerberosRealm;
	public String serverPrincipal;
	public String keyTab;
	public Boolean debug = Boolean.FALSE;
	public Boolean allowPasswordAuthentication = Boolean.FALSE;
	public Boolean updateProfileFirstLogin = Boolean.FALSE;

	public KerberosComponent() {
		super(ProviderId.kerberos, PROVIDER_TYPE);
		enabled = false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(allowPasswordAuthentication, debug, kerberosRealm, keyTab,
				serverPrincipal, updateProfileFirstLogin);
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
		KerberosComponent other = (KerberosComponent) obj;
		return Objects.equals(allowPasswordAuthentication, other.allowPasswordAuthentication)
				&& Objects.equals(debug, other.debug) && Objects.equals(kerberosRealm, other.kerberosRealm)
				&& Objects.equals(keyTab, other.keyTab) && Objects.equals(serverPrincipal, other.serverPrincipal)
				&& Objects.equals(updateProfileFirstLogin, other.updateProfileFirstLogin);
	}

	@Override
	public String toString() {
		return "KerberosComponent [kerberosRealm=" + kerberosRealm + ", serverPrincipal=" + serverPrincipal
				+ ", keyTab=" + keyTab + ", debug=" + debug + ", allowPasswordAuthentication="
				+ allowPasswordAuthentication + ", updateProfileFirstLogin=" + updateProfileFirstLogin + ", enabled="
				+ enabled + ", providerId=" + providerId + ", providerType=" + providerType + ", id=" + id
				+ ", parentId=" + parentId + ", name=" + name + ", cachePolicy=" + cachePolicy + "]";
	}
}
