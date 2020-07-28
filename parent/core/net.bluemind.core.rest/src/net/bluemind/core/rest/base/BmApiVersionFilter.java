package net.bluemind.core.rest.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.AsciiString;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.rest.filter.RestFilterAdapter;

public class BmApiVersionFilter extends RestFilterAdapter {

	private static Logger logger = LoggerFactory.getLogger(BmApiVersionFilter.class);

	private static final String CORE_VERSION = BMVersion.getVersion();
	private static final boolean DEV_MODE = CORE_VERSION.contains("qualifier");
	private static final CharSequence VERSION_HEADER = AsciiString.cached("x-bm-clientversion");

	@Override
	public AsyncHandler<RestResponse> preAuthorization(RestRequest request,
			AsyncHandler<RestResponse> responseHandler) {

		String clientVersion = request.headers.get(VERSION_HEADER);
		if (!isDevMode(clientVersion) && clientVersion != null && !CORE_VERSION.equals(clientVersion)) {
			String msg = String.format("CORE called with wrong version, clientVersion : %s, coreVersion %s",
					clientVersion, CORE_VERSION);
			logger.warn(msg);

			return new AsyncHandler<RestResponse>() {

				@Override
				public void success(RestResponse value) {
					value.headers.add("X-BM-WarnMessage", msg);
					responseHandler.success(value);
				}

				@Override
				public void failure(Throwable e) {
					responseHandler.failure(e);
				}

			};
		} else {
			return responseHandler;
		}
	}

	private boolean isDevMode(String clientVersion) {
		return (clientVersion != null && clientVersion.contains("qualifier")) || DEV_MODE;
	}
}
