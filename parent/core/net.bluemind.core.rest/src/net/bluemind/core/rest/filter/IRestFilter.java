package net.bluemind.core.rest.filter;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;

public interface IRestFilter {

	public AsyncHandler<RestResponse> preAuthorization(RestRequest request, AsyncHandler<RestResponse> responseHandler);

	public AsyncHandler<RestResponse> authorized(RestRequest request, SecurityContext context,
			AsyncHandler<RestResponse> responseHandler);

}
