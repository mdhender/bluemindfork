/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.restbus.api.gwt;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.restbus.api.gwt.internal.RespHandler;

public class RestBusImpl implements RestBus {

	public static final RestBusImpl get() {
		return new RestBusImpl();
	}

	@Override
	public final native void addListener(OnlineListener listener)
	/*-{
    $wnd['restClient'].addListener(function(b) {
      listener.@net.bluemind.restbus.api.gwt.RestBus.OnlineListener::status(Z)(b);
    });
	}-*/;

	@Override

	public void sendMessage(GwtRestRequest request, AsyncHandler<GwtRestResponse> responseHandler) {
		sendMessageImpl(request, new RespHandler(responseHandler));
	}

	private final native void sendMessageImpl(GwtRestRequest request, RespHandler responseHandler)
	/*-{

    var responseHandlerWrapper;
    if (responseHandler) {
      responseHandlerWrapper = function(reply) {
        if (request['method'] == 'register') {
          responseHandler.@net.bluemind.restbus.api.gwt.internal.RespHandler::onReply(Lcom/google/gwt/core/client/JavaScriptObject;)({'body':reply});
        } else {
          responseHandler.@net.bluemind.restbus.api.gwt.internal.RespHandler::onReply(Lcom/google/gwt/core/client/JavaScriptObject;)(reply);
        }
      }
    }
    $wnd['restClient'].sendMessage(request, responseHandlerWrapper);
	}-*/;

}
