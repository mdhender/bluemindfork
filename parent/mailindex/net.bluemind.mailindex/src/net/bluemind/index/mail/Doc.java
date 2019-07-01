package net.bluemind.index.mail;

import java.util.List;
import java.util.Map;

public final class Doc {

	private final List<String> body;
	private final Map<String, Object> values;

	public Doc(List<String> body, Map<String, Object> values) {
		this.body = body;
		this.values = values;
	}

	public List<String> getBody() {
		return body;
	}

	public Map<String, Object> getValues() {
		return values;
	}

}
