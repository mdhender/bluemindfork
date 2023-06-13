package net.bluemind.lib.elasticsearch.exception;

import net.bluemind.core.api.fault.ServerFault;

public class ElasticBulkException extends ServerFault {

	public ElasticBulkException(Throwable cause) {
		super("Elasticsearch bulk operation failed", cause);
	}
}
