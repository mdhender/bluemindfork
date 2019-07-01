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
 * @fileoverview Interface for container model management.
 */

goog.provide("net.bluemind.container.service.ContainerObserver");
goog.provide("net.bluemind.container.service.ContainersObserver");
goog.provide("net.bluemind.container.service.ContainersObserver.EventType");

goog.require("goog.array");
goog.require("goog.events");
goog.require("goog.events.Event");
goog.require("goog.events.EventTarget");
goog.require("goog.structs.Map");

/**
 * Service provider object for containers
 * 
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.container.service.ContainersObserver = function() {
  goog.base(this);
  /** @private {goog.debug.Logger} */
  this.logger_ = goog.log.getLogger('net.bluemind.container.service.ContainersObserver');
  this.containersByType = new goog.structs.Map();
  this.observers = new goog.structs.Map();
  this.online_ = false;
  restClient.addListener(goog.bind(this.onOnline_, this));
};
goog.inherits(net.bluemind.container.service.ContainersObserver, goog.events.EventTarget);

/**
 * @private
 */
net.bluemind.container.service.ContainersObserver.prototype.onOnline_ = function() {
  this.online_ = restClient.online();
  if (this.online_) {
    var old = this.containersByType.clone();
    this.containersByType = new goog.structs.Map();
    old.forEach(function(value, key) {
      this.observerContainers(key, value);
    }, this);
  }
}

/**
 * @param {string} type
 * @param {Array.<Object>} containers
 */
net.bluemind.container.service.ContainersObserver.prototype.observerContainers = function(type, containers) {

  goog.log.info(this.logger_, "Register type :" + type);
  goog.log.info(this.logger_, function() {
    return goog.debug.deepExpose(containers);
  });
  if (!this.online_) {
    this.observers = new goog.structs.Map();
    this.containersByType.set(type, containers);
  } else {
    var old = this.containersByType.get(type) || [];
    var toAdd = goog.array.filter(containers, function(c) {
      return !this.observers.containsKey(c);
    }, this);

    var toRemove = goog.array.filter(old, function(c) {
      return !goog.array.contains(containers, c);
    });

    goog.array.forEach(toRemove, function(c) {
      var obs = this.observers.get(c);
      if (obs) {
        obs.unregister();
        this.observers.remove(c);
      }
    }, this);
    goog.array.forEach(toAdd, function(c) {
      var obs = new net.bluemind.container.service.ContainerObserver(type, c, this);
      if (restClient.online()) {
        obs.register();
      }
      this.observers.set(c, obs);
    }, this);
    this.containersByType.set(type, containers);
  }
}

/**
 * @constructor
 * @param {string} type
 * @param {Object} container
 * @param {goog.events.EventTarget} eventTarget
 */
net.bluemind.container.service.ContainerObserver = function(type, container, eventTarget) {
  /** @private {goog.debug.Logger} */
  this.logger_ = goog.log.getLogger('net.bluemind.container.service.ContainerObserver');
  this.type = type;
  this.container = container;
  this.changeListener = goog.bind(this.containerChanged, this);
  this.registred = false;
  this.eventTarget = eventTarget;
}
/**
 * @type {string}
 * @private
 */
net.bluemind.container.service.ContainerObserver.prototype.type;
/**
 * @type {Object}
 * @private
 */
net.bluemind.container.service.ContainerObserver.prototype.container;
/**
 * @type {function()}
 */
net.bluemind.container.service.ContainerObserver.prototype.changeListener;
/**
 * @type {boolean}
 * @private
 */
net.bluemind.container.service.ContainerObserver.prototype.registred;
/**
 * @type {goog.events.EventTarget}
 * @private
 */
net.bluemind.container.service.ContainerObserver.prototype.eventTarget;
/**
 * On container changed
 */
net.bluemind.container.service.ContainerObserver.prototype.containerChanged = function() {
  var e = new goog.events.Event(net.bluemind.container.service.ContainersObserver.EventType.CHANGE);
  e.container = this.container;
  e.containerType = this.type;
  goog.log.info(this.logger_, "Fire changed ");
  goog.log.info(this.logger_, function() {
    return goog.debug.deepExpose(e);
  });
  this.eventTarget.dispatchEvent(e);
}

/**
 * Register the observer on vertx bus
 */
net.bluemind.container.service.ContainerObserver.prototype.register = function() {
  restClient.sendMessage({
    "method" : "register",
    "params" : {},
    "path" : "bm." + this.type + ".hook." + this.container + ".changed"
  }, this.changeListener);

  this.registred = true;
}

/**
 * Unregister the observer on vertx bus
 */
net.bluemind.container.service.ContainerObserver.prototype.unregister = function() {
  if (this.registred) {
    restClient.sendMessage({
      "method" : "unregister",
      "params" : {},
      "path" : "bm." + this.type + ".hook." + this.container + ".changed"
    }, this.changeListener);
  }
}
/**
 * @enum {string}
 */
net.bluemind.container.service.ContainersObserver.EventType = {
  CHANGE : goog.events.getUniqueId('changed')
};
