package net.bluemind.metrics.core.tick.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.metrics.core.tick.ITickDashboardProvider;
import net.bluemind.server.api.Server;

public class ChronografClient implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ChronografClient.class);
	private final String apiEndpoint;
	private final JsonHttpHelper jsonHelper;

	public ChronografClient(ItemValue<Server> chronografServer) {
		this.apiEndpoint = "http://" + chronografServer.value.address() + ":8888/tick/chronograf/v1";
		this.jsonHelper = new JsonHttpHelper();
	}

	public List<DashInfos> getExistingDashboards()
			throws JsonProcessingException, IOException, InterruptedException, ExecutionException, TimeoutException {
		List<DashInfos> existingDashboards = new ArrayList<>();
		String request = apiEndpoint + "/dashboards";
		JsonObject json = jsonHelper.get(request);
		JsonArray dashBoards = json.getJsonArray("dashboards");
		if (dashBoards != null) {
			for (int i = 0; i < dashBoards.size(); i++) {
				JsonObject dashBoard = dashBoards.getJsonObject(i);
				existingDashboards
						.add(new DashInfos(dashBoard.getString("name"), Integer.parseInt(dashBoard.getString("id"))));
			}
		}
		return existingDashboards;
	}

	public void annotate(String name, Date start, Date end, Map<String, String> tags) {
		JsonObject annot = new JsonObject();
		annot.put("id", UUID.randomUUID().toString());
		annot.put("text", name).put("type", "");
		BmDateTime startDT = BmDateTimeWrapper.fromTimestamp(start.getTime());
		BmDateTime endDT = end != null ? BmDateTimeWrapper.fromTimestamp(end.getTime()) : startDT;
		annot.put("startTime", startDT.iso8601).put("endTime", endDT.iso8601);
		if (!tags.isEmpty()) {
			JsonObject tagJs = new JsonObject();
			tags.entrySet().forEach(e -> {
				tagJs.put(e.getKey(), e.getValue());
			});
			annot.put("tags", tagJs);
		}
		try {
			jsonHelper.sendPost(apiEndpoint + "/sources/0/annotations", annot);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

	}

	public void createOrUpdateDashboard(ITickDashboardProvider dashboard) {
		try {
			List<DashInfos> existingDashboards = getExistingDashboards();
			JsonObject json = dashboard.jsonContent();
			try {
				for (DashInfos i : existingDashboards) {
					if (dashboard.name().equals(i.name)) {
						String request = apiEndpoint + "/dashboards/" + i.id;
						jsonHelper.sendPut(request, json);
						logger.info("Chronograf dashboard {} updated.", i.name);
						return;
					}
				}
				String request = apiEndpoint + "/dashboards";
				jsonHelper.sendPost(request, json);
				logger.info("Chronograf dashboard {} created.", dashboard.name());
			} catch (Exception e) {
				logger.warn("Unable to create chronograf dashboard", e);
			}
		} catch (Exception e) {
			logger.warn("Error while setting chronograf dashboards", e);
		}
	}

	public void deleteDashboard(String dashboardName) {
		try {
			List<DashInfos> existingDashboards = getExistingDashboards();
			for (DashInfos i : existingDashboards) {
				if (i.name.equals(dashboardName)) {
					String request = apiEndpoint + "/dashboards/" + i.id;
					jsonHelper.delete(request);
					logger.info("Chronograf dashboard {} removed", i.name);
					return;
				}
			}
		} catch (Exception e) {
			logger.warn("Error while removing chronograf dashboard {}", dashboardName, e);
		}
	}

	@Override
	public void close() {
		jsonHelper.close();

	}
}
