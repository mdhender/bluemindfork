package net.bluemind.lib.elasticsearch.exception;

public class ElasticTaskException extends Exception {

	public ElasticTaskException(Throwable cause) {
		super("Elasticsearch bulk operation failed", cause);
	}

	public ElasticTaskException(String reason) {
		super(reason);
	}
}
