package net.bluemind.lib.elasticsearch.datastreams;

public class IndexTemplateError {
	public String message;

	public IndexTemplateError() {
	}

	public IndexTemplateError(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return message;
	}
}
