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

/**
 * @fileoverview
 * 
 * Bluemind application banner.
 */

goog.provide("net.bluemind.ui.Notification");

goog.require("goog.Timer");
goog.require("goog.dom.classes");
goog.require("goog.dom.classlist");
goog.require("goog.ui.Component");
goog.require("goog.ui.Control");
goog.require("net.bluemind.ui.NotificationTemplate");// FIXME - unresolved
// required symbol

/**
 * Simple widget to display notifications
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Control}
 */
net.bluemind.ui.Notification = function(opt_domHelper) {
  goog.base(this, undefined, undefined, opt_domHelper);
};
goog.inherits(net.bluemind.ui.Notification, goog.ui.Control);

/** @override */
net.bluemind.ui.Notification.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getDomHelper().getDocument(), 'ui-notification', this.handleNotification_);
};

/**
 * @param {*} e
 */
net.bluemind.ui.Notification.prototype.handleNotification_ = function(e) {
  var event = e.getBrowserEvent();
  var msg = new goog.ui.Component();
  this.addChild(msg, true);
  goog.dom.classlist.add(msg.getElement(), goog.getCssName('notificationMsg'));
  msg.getElement().innerHTML = net.bluemind.ui.NotificationTemplate.message({
    type : event['detail']['type'],
    msg : event['detail']['message']
  });

  goog.Timer.callOnce(function() {
    this.hideFirst_();
  }, 3000, this);
}

/** @override */
net.bluemind.ui.Notification.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.decorateInternal(this.getElement());
};

/** @override */
net.bluemind.ui.Notification.prototype.decorateInternal = function(el) {
  goog.base(this, 'decorateInternal', el);
  goog.dom.classlist.add(el, goog.getCssName('notification'));
};

/**
 * Hide bara
 */
net.bluemind.ui.Notification.prototype.hideFirst_ = function() {
  this.removeChildAt(0, true);
};
