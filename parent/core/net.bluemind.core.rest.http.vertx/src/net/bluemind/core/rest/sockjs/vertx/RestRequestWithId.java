package net.bluemind.core.rest.sockjs.vertx;

import java.util.List;
import java.util.Optional;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import net.bluemind.core.rest.base.RestRequest;

public class RestRequestWithId extends RestRequest {
	public RestRequestWithId(String id, String origin, List<String> remoteAddresses, String verb, MultiMap headers,
			String path, MultiMap params, Buffer body) {
		super(origin, remoteAddresses, safeValue(verb), headers, path, params, body, null);
		this.id = Optional.ofNullable(id);
		this.verb = verb;
	}

	public final Optional<String> id;
	public final String verb;

	private static HttpMethod safeValue(String v) {
		try {
			return io.vertx.core.http.HttpMethod.valueOf(v);
		} catch (Exception e) {
			return io.vertx.core.http.HttpMethod.OTHER;
		}
	}
}
