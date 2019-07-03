/**
 * BEGIN LICENSE
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

/**
 * @fileoverview This event wrapper will dispatch an event when the user click
 *               on a link. Link can either be area or a element with an href
 *               value that does not point on a inner anchor (href='#xxxx').
 * 
 * This class aims to help to intercept all click on link element not handled by
 * javascript code. This class also allow to register protocol handler that will
 * be called on link referring to a specific protocol.
 * 
 */

goog.provide("net.bluemind.events.LinkHandler");
goog.provide("net.bluemind.events.LinkHandler.ProtocolHandler");

goog.require("goog.Uri");
goog.require("goog.dom");
goog.require("goog.events");
goog.require("goog.events.EventTarget");
goog.require("goog.events.EventType");
goog.require("goog.structs.Map");

/**
 * This event handler allows you to catch link events.
 * 
 * @param {Element|Document} element The element to listen to the links event
 *          on. All
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.events.LinkHandler = function(element) {
  goog.base(this);

  this.uri_ = new goog.Uri(window.location.href).setFragment('');

  this.element_ = element;

  this.listenKey_ = goog.events.listen(this.element_, goog.events.EventType.CLICK, this);

  this.protocolHandlers_ = new goog.structs.Map();

};
goog.inherits(net.bluemind.events.LinkHandler, goog.events.EventTarget);

/**
 * This is the element that we will listen to click events on.
 * 
 * @type {Element|Document}
 * @private
 */
net.bluemind.events.LinkHandler.prototype.element_;

/**
 * This is the document base URI.
 * 
 * @type {goog.Uri}
 * @private
 */
net.bluemind.events.LinkHandler.prototype.uri_;

/**
 * The key returned from the goog.events.listen.
 * 
 * @type {?goog.events.Key}
 * @private
 */
net.bluemind.events.LinkHandler.prototype.listenKey_;

/**
 * Map of the registred protocol handler.
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.events.LinkHandler.prototype.protocolHandlers_;

/** @override */
net.bluemind.events.LinkHandler.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  goog.events.unlistenByKey(this.listenKey_);
  delete this.listenKey_;
};

/**
 * Handles the events on the element.
 * 
 * @param {goog.events.BrowserEvent} e The underlying browser event.
 */
net.bluemind.events.LinkHandler.prototype.handleEvent = function(e) {
  var element = e.target;
  var link = goog.dom.getAncestor(element, this.isLink_, true);
  if (link) {
    var uri = this.uri_.resolve(goog.Uri.resolve(this.uri_, link.href));
    if (this.isExternal_(uri)) {
      var h = this.protocolHandlers_.get(uri.getScheme());
      if (h) {
        if (h.handleUri(uri)) {
          e.preventDefault();
        }
      }
    }
  }
};

/**
 * Register a new protocol handler.
 * 
 * @param {string| Array.<string>} protocols Protocols to handle.
 * @param {net.bluemind.events.LinkHandler.ProtocolHandler} handler Protocol
 *          handler to register.
 */
net.bluemind.events.LinkHandler.prototype.registerProtocolHandler = function(protocols, handler) {
  if (!goog.isArray(protocols)) {
    protocols = [ protocols ];
  }
  ;
  for (var i = 0; i < protocols.length; i++) {
    var protocol = protocols[i];
    if (!this.protocolHandlers_.get(protocol)) {
      this.protocolHandlers_.set(protocol, handler);
    } else {
      throw Error('A protocol handler is already registered for ' + protocol);
    }
  }
};

/**
 * Register a new protocol handler.
 * 
 * @param {string} protocol Protocol to stop handling.
 */
net.bluemind.events.LinkHandler.prototype.unregisterProtocolHandler = function(protocol) {
  this.protocolHandlers_.remove(protocol);
};

/**
 * Return the handler associated to the specific protocol.
 * 
 * @param {string} protocol Protocol to stop handling.
 * @return {net.bluemind.events.LinkHandler.ProtocolHandler} protocol Protocol
 *         handler.
 */
net.bluemind.events.LinkHandler.prototype.getProtocolHandler = function(protocol) {
  return this.protocolHandlers_.get(protocol);
};

/**
 * Return true if the given element is a valid link and his in the search scope.
 * 
 * @param {Node | null} e Tested element.
 * @return {boolean} true if the element is a link
 */
net.bluemind.events.LinkHandler.prototype.isLink_ = function(e) {
  return (e.nodeName == 'A' || e.nodeName == 'AREA') && e.hasAttribute('href');
};

/**
 * Return true if the uri is an internal link.
 * 
 * @param {goog.Uri} uri Tested uri.
 * @return {boolean} true if the uri is an external link
 */
net.bluemind.events.LinkHandler.prototype.isExternal_ = function(uri) {
  if (!uri.hasFragment()) {
    return true;
  }
  if (uri.clone().setFragment('').toString() != this.uri_.toString()) {
    return true;
  }
  return false;
};

/**
 * Handle link matching the pattern #protocol:value
 * 
 * @constructor
 */
net.bluemind.events.LinkHandler.ProtocolHandler = function() {
};

/**
 * Handle the link. The return value tell to the default action associated to
 * this link should be prevented or if the browser should handle it.
 * 
 * @param {goog.Uri} uri URI to handle.
 * @return {boolean} True if the default action should be prevented, false else.
 */
net.bluemind.events.LinkHandler.ProtocolHandler.prototype.handleUri = goog.abstractMethod;
