package net.bluemind.keycloak.api;

import java.util.List;
import java.util.Map;

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
	public List<String> redirectUris;
	public List<String> webOrigins;
	public Map<String, String> attributes;
	public String baseUrl;
}
