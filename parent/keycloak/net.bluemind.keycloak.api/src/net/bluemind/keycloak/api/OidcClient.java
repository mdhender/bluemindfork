package net.bluemind.keycloak.api;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class OidcClient {
	public boolean enabled;
	public String id;
	public String clientId;
	public boolean publicClient = false;
	public String secret;
	public boolean standardFlowEnabled = true;
	public boolean directAccessGrantsEnabled = true;
	public boolean serviceAccountsEnabled = false;
	public String rootUrl;
	public Set<String> redirectUris;
	public Set<String> webOrigins;
	public Map<String, String> attributes;
	public String baseUrl;

	@Override
	public int hashCode() {
		return Objects.hash(attributes, baseUrl, clientId, directAccessGrantsEnabled, enabled, id, publicClient,
				redirectUris, rootUrl, secret, serviceAccountsEnabled, standardFlowEnabled, webOrigins);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OidcClient other = (OidcClient) obj;
		return Objects.equals(attributes, other.attributes) && Objects.equals(baseUrl, other.baseUrl)
				&& Objects.equals(clientId, other.clientId)
				&& directAccessGrantsEnabled == other.directAccessGrantsEnabled && enabled == other.enabled
				&& Objects.equals(id, other.id) && publicClient == other.publicClient
				&& Objects.equals(redirectUris, other.redirectUris) && Objects.equals(rootUrl, other.rootUrl)
				&& Objects.equals(secret, other.secret) && serviceAccountsEnabled == other.serviceAccountsEnabled
				&& standardFlowEnabled == other.standardFlowEnabled && Objects.equals(webOrigins, other.webOrigins);
	}

	@Override
	public String toString() {
		return "OidcClient [enabled=" + enabled + ", id=" + id + ", clientId=" + clientId + ", publicClient="
				+ publicClient + ", secret=" + secret + ", standardFlowEnabled=" + standardFlowEnabled
				+ ", directAccessGrantsEnabled=" + directAccessGrantsEnabled + ", serviceAccountsEnabled="
				+ serviceAccountsEnabled + ", rootUrl=" + rootUrl + ", redirectUris=" + redirectUris + ", webOrigins="
				+ webOrigins + ", attributes=" + attributes + ", baseUrl=" + baseUrl + "]";
	}
}
