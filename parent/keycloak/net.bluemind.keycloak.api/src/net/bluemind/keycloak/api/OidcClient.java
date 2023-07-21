package net.bluemind.keycloak.api;

import java.util.List;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class OidcClient {

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
	public String baseUrl;

	public JsonObject toJson() {
		return new JsonObject().put("id", id).put("clientId", clientId).put("publicClient", publicClient)
				.put("secret", secret).put("standardFlowEnabled", standardFlowEnabled)
				.put("directAccessGrantsEnabled", directAccessGrantsEnabled)
				.put("serviceAccountsEnabled", serviceAccountsEnabled).put("rootUrl", rootUrl)
				.put("redirectUris", redirectUris).put("webOrigins", webOrigins).put("baseUrl", baseUrl);
	}
}
