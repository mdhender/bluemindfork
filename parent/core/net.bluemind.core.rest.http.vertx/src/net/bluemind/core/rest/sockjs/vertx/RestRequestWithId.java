package net.bluemind.core.rest.sockjs.vertx;

import java.util.List;
import java.util.Optional;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;

import net.bluemind.core.rest.base.RestRequest;

public class RestRequestWithId extends RestRequest {
	public RestRequestWithId(String id, String origin, List<String> remoteAddresses, String method, MultiMap headers,
			String path, MultiMap params, Buffer body) {
		super(origin, remoteAddresses, method, headers, path, params, body, null);
		this.id = Optional.ofNullable(id);
	}

	public final Optional<String> id;
}
