package net.bluemind.system.api.hot.upgrade;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.utils.JsonUtils;

@BMApi(version = "3")
public class HotUpgradeTask {
	public int id;
	public String operation;
	public String parameters;
	public HotUpgradeTaskStatus status;
	public int failure = 0;
	public Date createdAt;
	public Date updatedAt;
	public HotUpgradeTaskExecutionMode executionMode;
	public int retryCount = 3;
	public int retryDelaySeconds = 0; // TimeUnit.SECONDS
	public boolean reportFailure = false;

	public HotUpgradeTask() {
		executionMode = HotUpgradeTaskExecutionMode.DIRECT;
	}

	private Map<String, Object> deserializedParameters;

	public HotUpgradeTask succeed() {
		this.status = HotUpgradeTaskStatus.SUCCESS;
		return this;
	}

	public HotUpgradeTask failed() {
		this.status = HotUpgradeTaskStatus.FAILURE;
		this.failure++;
		return this;
	}

	public Map<String, Object> getParameters() {
		if (Objects.isNull(deserializedParameters)) {
			deserializedParameters = JsonUtils.readMap(parameters, String.class, Object.class);
		}
		return deserializedParameters;
	}

	public String getParameterAsString(String name) {
		return (String) getParameters().get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getParameterAsList(String name) {
		return (List<T>) getParameters().get(name);
	}

	public HotUpgradeTask setParameters(Map<String, Object> parameters) {
		this.parameters = JsonUtils.asString(parameters);
		return this;
	}

	public String groupName() {
		return this.updatedAt.toInstant().toEpochMilli() + "-" + this.operation;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HotUpgradeTask [id=").append(id).append(", operation=").append(operation)
				.append(", parameters=").append(parameters).append(", status=").append(status).append(", failure=")
				.append(failure).append(", retryCount=").append(retryCount).append(", retryDelay=").append(retryDelaySeconds)
				.append(", reportFailure=").append(reportFailure).append("s, createdAt=").append(createdAt)
				.append(", updatedAt=").append(updatedAt).append("]");
		return builder.toString();
	}

}
