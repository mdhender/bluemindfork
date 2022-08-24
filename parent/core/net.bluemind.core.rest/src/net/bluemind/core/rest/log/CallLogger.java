package net.bluemind.core.rest.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;

public class CallLogger {

	public static final Logger logger = LoggerFactory.getLogger(CallLogger.class);
	private RestRequest request;
	private long startTime;
	private String component;

	private CallLogger(String component, RestRequest request) {
		this.component = component;
		this.request = request;
	}

	public static CallLogger start(String component, RestRequest request) {
		return new CallLogger(component, request).start();
	}

	public CallLogger start() {
		startTime = System.nanoTime();
		return this;
	}

	protected void logResponse(RestResponse value) {
		long time = System.nanoTime();
		long elapsed = (time - startTime) / (1000000);
		if (elapsed > 500) {
			logger.warn("{} call {} took {}ms", component, request, elapsed);
		}
	}

	public AsyncHandler<RestResponse> responseHandler(AsyncHandler<RestResponse> handler) {
		return new AsyncHandler<RestResponse>() {

			@Override
			public void success(RestResponse value) {
				logResponse(value);
				handler.success(value);
			}

			@Override
			public void failure(Throwable e) {
				logResponse(null);
				handler.failure(e);
			}

		};
	}

}
