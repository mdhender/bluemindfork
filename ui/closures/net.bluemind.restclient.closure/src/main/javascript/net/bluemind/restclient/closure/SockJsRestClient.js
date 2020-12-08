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

goog.provide('net.bluemind.restclient.SockJsRestClient');
goog.require('goog.Timer');

/**
 * @constructor
 * @param {string} url
 */
net.bluemind.restclient.SockJsRestClient = function(url) {
  this.url_ = url;
  this.sockJSConn_ = null;
  this.handlerMap_ = {};
  this.replyHandlers_ = {};
  this.eventHandlers_ = {};
  this.state = net.bluemind.restclient.SockJsRestClient.CONNECTING;
  this.lastConnectAttempt = -1;
}

/**
 * @type {string}
 * @private
 */
net.bluemind.restclient.SockJsRestClient.prototype.url_ = null;

/**
 * @type {SockJS}
 * @private
 */
net.bluemind.restclient.SockJsRestClient.prototype.sockJSConn_ = null;

/**
 * @type {Array}
 */
net.bluemind.restclient.SockJsRestClient.prototype.handlerMap_ = null;

/**
 * @type {Array}
 */
net.bluemind.restclient.SockJsRestClient.prototype.replyHandlers_ = [];

/**
 * @type {Array}
 */
net.bluemind.restclient.SockJsRestClient.prototype.replyHandlers_ = [];

/**
 * @type {boolean}
 */
net.bluemind.restclient.SockJsRestClient.prototype.connected_ = undefined;

/**
 * @type {number}
 */
net.bluemind.restclient.SockJsRestClient.prototype.lastConnectAttempt = -1;

/**
 * @export
 */
net.bluemind.restclient.SockJsRestClient.prototype.start = function() {
  this.listeners_ = [];
  var that = this;
  this.doConnect_();
}

/**
 * @export
 */
net.bluemind.restclient.SockJsRestClient.prototype.sendMessage = function(restRequest, responseHandler) {
  if( this.sockJSConn_ == null ) {
    if( responseHandler != null) {
    var resp = { "body": {"errorCode": "FAILURE", errorType: "ServerFault", message: "restClient not available"},
        "statusCode" : 418};
    responseHandler(resp);
    } else {
      console.log("try to send msg but sockJS not available",restRequest);
    }
    return;
  }
  if(responseHandler) {
    restRequest['requestId'] = net.bluemind.restclient.SockJsRestClient.makeUUID();
  }

  if( !restRequest['headers']) {
    restRequest['headers'] = {};
  }
  
  if(!restRequest['headers']["X-BM-ApiKey"] ) {
    restRequest['headers']["X-BM-ApiKey"] = goog.global["bmcSessionInfos"]["sid"];
  }

  var str = JSON.stringify(restRequest);
  
  if (responseHandler) {
    if (restRequest["method"] == "register") {
      this.eventHandlers_[restRequest['path']] = net.bluemind.restclient.SockJsRestClient.copyHandlers(this.eventHandlers_[restRequest['path']],  responseHandler);
    } else if (restRequest["method"] == "unregister") {
      delete this.eventHandlers_[restRequest['path']];
    } else {
      this.replyHandlers_[restRequest['requestId']] = responseHandler;
    }
  }
  this.sockJSConn_.send(str);
}

/** 
 * @private
 * param {array} handlers
 */
net.bluemind.restclient.SockJsRestClient.copyHandlers = function(handlers, handler) {
  if( !handlers) {
    handlers = [];
  } 
  var copy = handlers.slice(0);
  copy.push(handler);
  return copy;
}
/**
 * @export
 * @param {function()} listener
 */
net.bluemind.restclient.SockJsRestClient.prototype.addListener = function(listener) {
  this.listeners_.push(listener);
  listener.apply(null, [ this.connected_ ]);
}

/**
 * @export
 * @return {boolean}
 */
net.bluemind.restclient.SockJsRestClient.prototype.online = function() {
  return this.connected_;
}

/**
 * @private
 */
net.bluemind.restclient.SockJsRestClient.prototype.doConnect_ = function() {
  if (this.state = net.bluemind.restclient.SockJsRestClient.CONNECTING) {
    return;
  }
  this.lastConnectAttempt = goog.now();
  this.state = net.bluemind.restclient.SockJsRestClient.CONNECTING;
  this.createSockJSConn_();
  console.log("restclient : connecting...");
}

/**
 * @private
 */
net.bluemind.restclient.SockJsRestClient.prototype.createSockJSConn_ = function() {
  var that = this;
  var sockJSConn = new SockJS(this.url_, undefined, undefined);

  sockJSConn.onheartbeat = function() {
	console.log("restclient: sockJSConn hearbeat");
  }

  sockJSConn.onopen = function() {
    console.log("restclient: sockJSConn onopen");
    that.sockJSConn_ = sockJSConn;
    that.state = net.bluemind.restclient.SockJsRestClient.OPEN;
    that.handleConnect_();
  };

  sockJSConn.onclose = function() {
    console.log("restclient: sockJSConn onclose");
    that.state = net.bluemind.restclient.SockJsRestClient.CLOSED;
    that.sockJSConn_ = null;
    that.handleDisconnect_();
  };

  sockJSConn.onmessage = function(e) {
    var msg = e.data;
    var json = JSON.parse(msg);
    var id = json["requestId"];
    var handler = that.replyHandlers_[id];
    if (handler) {
      delete that.replyHandlers_[id];
      handler(json);
    } else {
      var eventHandler = that.eventHandlers_[id];
      if (eventHandler) {
        console.log("handlers ",eventHandler);
        goog.array.forEach(eventHandler, function(f) {
          f(json["body"]);
        });
      } else {
        console.log("no handler for ",json);
      }
    }
  }

}

/**
 * @private
 */
net.bluemind.restclient.SockJsRestClient.prototype.handleConnect_ = function() {
  if (this.connected_ != true && this.state == net.bluemind.restclient.SockJsRestClient.OPEN) {
    this.connected_ = true;

    var that = this;

    this.sockJSConn_.onheartbeat = function() {
        if (that.hearbeatId) {
            goog.Timer.clear(that.hearbeatId);
        }

        that.hearbeatId = goog.Timer.callOnce(function() {
	    console.log("hearbeat fail, disconnect!");
            this.hearbeatId = null;
            this.sockJSConn_.close();
        }, 1000 * 100, that);

    };

    this.sockJSConn_.onheartbeat();

    this.sendMessage({
      "method" : "GET",
      "path" : "/api/auth/ping",
      "headers" : {
        "X-BM-ApiKey" : goog.global["bmcSessionInfos"]["sid"]
      },
      "params" : {}
    }, function(res) {
      if (res["statusCode"] == 200) {
        that.notify_(true);
      } else {
        that.notify_(true, false);
      }
    });

  }
}

/**
 * @private
 */
net.bluemind.restclient.SockJsRestClient.prototype.handleDisconnect_ = function() {
  console.log("disconnect, rplyHandlers ",this.replyHandlers_);
  goog.object.forEach(this.replyHandlers_, function(h) {
    console.log("send error to ",h);
    var resp = { "body": {"errorCode": "FAILURE", errorType: "ServerFault", message: "restClient disconnected"},
    "statusCode" : 418};
    h(resp);
  });
  this.replyHandlers_ = [];

  try {
    if (this.sockJSConn_) {
      this.sockJSConn_.close();
    }
  } catch (e) {
    console.log("error during close", e);
  }

  if (this.hearbeatId) {
      goog.Timer.clear(this.hearbeatId);
  }


  this.sockJSConn_ = null;
  if (this.connected_ != false) {
    this.connected_ = false;
    this.notify_(false);
  }

  if( goog.now() - this.lastConnectAttempt > 15) {
    console.log("direct reconnect");
    this.doConnect_();
  } else {
    goog.Timer.callOnce(function() {
      console.log("temporized reconnect");
      this.doConnect_();
      }, 1000, this);
    }
};

/**
 * @private
 */
net.bluemind.restclient.SockJsRestClient.makeUUID = function() {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(a, b) {
    return b = Math.random() * 16, (a == "y" ? b & 3 | 8 : b | 0).toString(16)
  });
}
/**
 * @private
 */
net.bluemind.restclient.SockJsRestClient.prototype.notify_ = function(state) {

  if (state == true) {
    console.log("fire connected to eventbus");
    goog.array.forEach(this.listeners_, function(l) {
      try {
        l.apply(null, [ state ]);
      } catch (e) {
        console.log("error during going online", e);
      }
    });
  } else {
      console.log("fire disconnected from eventbus ( state "+this.connected_+")");
      goog.array.forEach(this.listeners_, function(l) {
        try {
          l.apply(null, [ state ]);
        } catch (e) {
          console.log("error during going offline", e);
        }
      });
  }
}
