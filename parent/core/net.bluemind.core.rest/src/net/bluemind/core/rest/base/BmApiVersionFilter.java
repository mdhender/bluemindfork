package net.bluemind.core.rest.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.rest.filter.RestFilterAdapter;

public class BmApiVersionFilter extends RestFilterAdapter {

	private static Logger logger = LoggerFactory.getLogger(BmApiVersionFilter.class);

	@Override
	public AsyncHandler<RestResponse> preAuthorization(RestRequest request,
			AsyncHandler<RestResponse> responseHandler) {

		String clientVersion = request.headers.get("X-BM-ClientVersion");
		String coreVersion = BMVersion.getVersion();
		if (!isDevMode(clientVersion, coreVersion) && clientVersion != null && !coreVersion.equals(clientVersion)) {
			String msg = String.format("CORE called with wrong version, clientVersion : %s, coreVersion %s",
					clientVersion, coreVersion);
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

	private boolean isDevMode(String clientVersion, String coreVersion) {
		return (clientVersion != null && clientVersion.contains("qualifier")) || coreVersion.contains("qualifier");
	}
}
