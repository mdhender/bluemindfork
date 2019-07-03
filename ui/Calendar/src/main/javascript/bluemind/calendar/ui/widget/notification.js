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
 * @fileoverview Notification bara componnent.
 */

goog.provide('bluemind.calendar.ui.widget.Notification');

goog.require('bluemind.calendar.notification.template');
goog.require('bluemind.events.NotificationHandler.EventType');
goog.require('goog.Timer');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.widget.Notification = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.timer_ = new goog.Timer(3000);
  this.getHandler().listen(this.timer_,
    goog.Timer.TICK,
    this.hide, false, this);
};
goog.inherits(bluemind.calendar.ui.widget.Notification, goog.ui.Component);

/**
 * Notification timer
 */
bluemind.calendar.ui.widget.Notification.prototype.timer_;

/** @inheritDoc */
bluemind.calendar.ui.widget.Notification.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.decorateInternal(this.getElement());
};

/** @inheritDoc */
bluemind.calendar.ui.widget.Notification.prototype.decorateInternal =
  function(el) {
  goog.base(this, 'decorateInternal', el);
  goog.dom.classes.add(el, goog.getCssName('notification'));
  el.innerHTML = bluemind.calendar.notification.template.simple();
  goog.style.showElement(el, false);
};


/** @override */
bluemind.calendar.ui.widget.Notification.prototype.setModel = function(model) {
  if (this.isInDocument() && this.getModel()) {
    this.getHandler().unlisten(this.getModel(), [
      bluemind.events.NotificationHandler.EventType.ERROR,
      bluemind.events.NotificationHandler.EventType.NOTICE,
      bluemind.events.NotificationHandler.EventType.INFO,
      bluemind.events.NotificationHandler.EventType.OK
      ], this.notify_);
  }
  goog.base(this, 'setModel', model);
  if (this.isInDocument()) {
    this.getHandler().listen(this.getModel(), [
      bluemind.events.NotificationHandler.EventType.ERROR,
      bluemind.events.NotificationHandler.EventType.NOTICE,
      bluemind.events.NotificationHandler.EventType.INFO,
      bluemind.events.NotificationHandler.EventType.OK
      ], this.notify_);
    if (model.getLastNotification()) {
      var n = model.getLastNotification();
      this.show(n.message);
    }
  }
};

/** @override */
bluemind.calendar.ui.widget.Notification.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  if (this.getModel()) {
    this.getHandler().listen(this.getModel(), [
      bluemind.events.NotificationHandler.EventType.ERROR,
      bluemind.events.NotificationHandler.EventType.NOTICE,
      bluemind.events.NotificationHandler.EventType.INFO,
      bluemind.events.NotificationHandler.EventType.OK
      ], this.notify_);
    if (this.getModel().getLastNotification()) {
      var n = this.getModel().getLastNotification();
      this.show(n.message);
    }
  }
};

/**
 * Notify an event message
 * @param {goog.events.Event} e Notification event.
 * @private
 */
bluemind.calendar.ui.widget.Notification.prototype.notify_ = function(e) {
  var msg = e.target;
  this.show(msg);
};

/**
 * Show bara
 * @param {Text} msg message.
 */
bluemind.calendar.ui.widget.Notification.prototype.show = function(msg) {
  this.timer_.stop();
  goog.style.showElement(this.getElement(), true);
  var el = goog.dom.getElement('notificationMessage');
  el.innerHTML = msg;
  this.timer_.start();
};

/**
 * Hide bara
 */
bluemind.calendar.ui.widget.Notification.prototype.hide = function() {
  goog.style.showElement(this.getElement(), false);
  this.timer_.stop();
};
