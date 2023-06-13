package net.bluemind.lib.elasticsearch.exception;

public class ElasticIndexException extends RuntimeException {

	public ElasticIndexException(String index, Throwable cause) {
		super("Elasticsearch operation failed on index '" + index + "'", cause);
	}
}
