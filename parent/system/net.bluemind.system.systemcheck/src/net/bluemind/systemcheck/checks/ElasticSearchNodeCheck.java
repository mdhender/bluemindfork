package net.bluemind.systemcheck.checks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.system.api.InstallationVersion;

public class ElasticSearchNodeCheck extends AbstractCheck {

	private static final Logger logger = LoggerFactory.getLogger(ElasticSearchNodeCheck.class);

	@Override
	public boolean canCheckWithVersion(InstallationVersion version) {
		return version.databaseVersion.startsWith("4.");
	}

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults result, Map<String, String> collected)
			throws Exception {
		CheckResult cr = ok("check.elasticsearch");

		String elasticSearchServer = Topology.get().any("bm/es").value.address();

		logger.info("Checking ES cluster status of server {}", elasticSearchServer);
		if (!elasticSearchOk(elasticSearchServer)) {
			return error("check.elasticsearch");
		}

		return cr;
	}

	private boolean elasticSearchOk(String server) {
		String url = String.format("http://%s:9200/_cluster/health?timeout=50s", server);
		try {
			URL connectionUrl = new URL(url);
			HttpURLConnection con = (HttpURLConnection) connectionUrl.openConnection();
			con.setRequestMethod("GET");
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String inputLine;
				StringBuffer content = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
				JsonObject ret = new JsonObject(content.toString());
				String status = ret.getString("status");
				return "green".equals(status);
			} finally {
				con.disconnect();
			}
		} catch (Exception e) {
			logger.warn("Cannot check ES cluster status of server {}", server, e);
			return false;
		}
	}

}
