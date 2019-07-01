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
 * @fileoverview
 * Allow to pass notification message.
 */

goog.provide('bluemind.events.NotificationHandler');
goog.provide('bluemind.events.NotificationHandler.EventType');
goog.provide('bluemind.events.NotificationHandler.Event');

goog.require('goog.events.Event');
goog.require('goog.events.EventTarget');

/**
 * Bus object to pass notification message.
 *
 * @extends {goog.events.EventTarget}
 * @constructor
 */
bluemind.events.NotificationHandler = function() {
  goog.base(this);
};
goog.inherits(bluemind.events.NotificationHandler, goog.events.EventTarget);

/**
 * Last notification message
 * @type {{message: string, type: bluemind.events.NotificationHandler.EventType}} Notification message
 */
bluemind.events.NotificationHandler.prototype.lastNotification_;

/**
 * Notify an error.
 * @param {string} msg Notification message 
 */
bluemind.events.NotificationHandler.prototype.error = function(msg) {
  this.notify_(bluemind.events.NotificationHandler.EventType.ERROR, msg);
};

/**
 * Notify an notice.
 * @param {string} msg Notification message 
 */
bluemind.events.NotificationHandler.prototype.notice = function(msg) {
  this.notify_(bluemind.events.NotificationHandler.EventType.NOTICE, msg);
};

/**
 * Notify an info.
 * @param {string} msg Notification message 
 */
bluemind.events.NotificationHandler.prototype.info = function(msg) {
  this.notify_(bluemind.events.NotificationHandler.EventType.INFO, msg);
};

/**
 * Notify an ok message.
 * @param {string} msg Notification message 
 */
bluemind.events.NotificationHandler.prototype.ok = function(msg) {
  this.notify_(bluemind.events.NotificationHandler.EventType.OK, msg);
};

/**
 * Notify an ok message.
 * @param {bluemind.events.NotificationHandler.EventType} type Notification type 
 * @param {string} msg Notification message 
 * @private
 */
bluemind.events.NotificationHandler.prototype.notify_ = function(type, msg) {
  this.dispatchEvent(new bluemind.events.NotificationHandler.Event(type, msg));
  this.lastNotification_ = {message: msg, type: type};
};

/**
 * Return the last notification message
 * @return {{message: string, type: bluemind.events.NotificationHandler.EventType}} Notification message
 */
bluemind.events.NotificationHandler.prototype.getLastNotification = function() {
  return this.lastNotification_;
};

/**
 * Constants for event names.
 * @enum {string}
 */
bluemind.events.NotificationHandler.EventType = {
  ERROR: 'error',
  NOTICE: 'notice',
  INFO: 'info',
  OK: 'ok'
}


/**
 * Object representing a notification event. 
 * @param {string} type Event Type.
 * @param {string} msg Notification message. 
 * @constructor
 * @extends {goog.events.Event}
 */
bluemind.events.NotificationHandler.Event = function(type, msg) {
  goog.base(this, type);
  this.msg = msg;
};
goog.inherits(bluemind.events.NotificationHandler.Event, goog.events.Event);

/**
 * Message
 * @type {string}
 */
bluemind.events.NotificationHandler.Event.prototype.msg;
