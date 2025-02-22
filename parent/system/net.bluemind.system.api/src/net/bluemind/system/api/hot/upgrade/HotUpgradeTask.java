package net.bluemind.system.api.hot.upgrade;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.GwtIncompatible;
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
	public boolean mandatory = false;
	public List<HotUpgradeStepEvent> events;

	public HotUpgradeTask() {
		executionMode = HotUpgradeTaskExecutionMode.DIRECT;
		events = new ArrayList<>();
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

	@GwtIncompatible
	public Map<String, Object> getParameters() {
		if (Objects.isNull(deserializedParameters)) {
			deserializedParameters = JsonUtils.readMap(parameters, String.class, Object.class);
		}
		return deserializedParameters;
	}

	@GwtIncompatible
	public String getParameterAsString(String name) {
		return (String) getParameters().get(name);
	}

	@GwtIncompatible
	@SuppressWarnings("unchecked")
	public <T> List<T> getParameterAsList(String name) {
		return (List<T>) getParameters().get(name);
	}

	@GwtIncompatible
	public HotUpgradeTask setParameters(Map<String, Object> parameters) {
		this.parameters = JsonUtils.asString(parameters);
		return this;
	}

	@GwtIncompatible
	public String groupName() {
		return this.createdAt.toInstant().toEpochMilli() + "-" + this.operation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HotUpgradeTask other = (HotUpgradeTask) obj;
		return id == other.id;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HotUpgradeTask [id=").append(id).append(", operation=").append(operation)
				.append(", parameters=").append(parameters).append(", status=").append(status).append(", failure=")
				.append(failure).append(", retryCount=").append(retryCount).append(", retryDelay=")
				.append(retryDelaySeconds).append(", reportFailure=").append(reportFailure).append("s, createdAt=")
				.append(createdAt).append(", updatedAt=").append(updatedAt).append("]");
		return builder.toString();
	}

}
