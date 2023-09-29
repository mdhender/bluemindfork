package net.bluemind.lib.elasticsearch.exception;

import net.bluemind.core.api.fault.ServerFault;

public class ElasticDocumentException extends ServerFault {

	public ElasticDocumentException(String index, Throwable cause) {
		super("Elasticsearch operation failed on index '" + index + "'", cause);
	}

	public ElasticDocumentException(String index, String reason) {
		super("Elasticsearch operation failed on index '" + index + "': " + reason);
	}
}
