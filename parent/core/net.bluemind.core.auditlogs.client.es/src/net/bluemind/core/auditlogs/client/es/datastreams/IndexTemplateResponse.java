package net.bluemind.core.auditlogs.client.es.datastreams;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.MoreObjects;

public class IndexTemplateResponse {

	public IndexTemplateError error;
	private Map<String, String> tags = Collections.emptyMap();
	private long size = 0;

	public boolean succeeded() {
		return error == null;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(getClass()).add("success", succeeded()).add("error", error).toString();
	}

	public IndexTemplateResponse withTags(Map<String, String> t) {
		this.tags = t;
		return this;
	}

	public IndexTemplateResponse withSize(long size) {
		this.size = size;
		return this;
	}

}
