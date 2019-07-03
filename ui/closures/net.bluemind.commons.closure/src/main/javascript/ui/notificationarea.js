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
 *
 * Bluemind application notification area.
 */


goog.provide('bluemind.ui.NotificationArea');

goog.require('bluemind.events.NotificationHandler.EventType');
goog.require('goog.Timer');

/**
 * Bluemind application notification area. 
 *
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.ui.NotificationArea = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.timer_ = new goog.Timer(3000);
  this.visible_ = false;
  this.close_ = this.getDomHelper().createDom('span', goog.getCssName('notification-dismiss'), 'X');
};
goog.inherits(bluemind.ui.NotificationArea, goog.ui.Component);


/**
 * Default CSS class to be applied to the root element of components rendered
 * by this renderer.
 * @type {string}
 */
bluemind.ui.NotificationArea.BASE_CSS = goog.getCssName('notification');

/**
 * Default CSS class to be applied to the root element of components rendered
 * by this renderer.
 * @type {string}
 */
bluemind.ui.NotificationArea.MESSAGE_CSS = goog.getCssName(bluemind.ui.NotificationArea.BASE_CSS, 'message');

/**
 * Is a notificication currently visible.
 * @type {boolean}
 */
bluemind.ui.NotificationArea.prototype.visible_;

/**
 * Button to tismiss stick notification.
 * @type {Element}
 */
bluemind.ui.NotificationArea.prototype.close_;

/**
 * Notification timer
 */
bluemind.ui.NotificationArea.prototype.timer_;

/** @override */
bluemind.ui.NotificationArea.prototype.setModel = function(model) {
  if (this.isInDocument() && this.getModel()) {
    this.getHandler().unlisten(this.getModel(),
      bluemind.events.NotificationHandler.EventType.ERROR, this.notifyError_);
    this.getHandler().unlisten(this.getModel(),
      bluemind.events.NotificationHandler.EventType.NOTICE, this.notifyNotice_);
    this.getHandler().unlisten(this.getModel(),
      bluemind.events.NotificationHandler.EventType.INFO, this.notifyInfo_);
    this.getHandler().unlisten(this.getModel(),
      bluemind.events.NotificationHandler.EventType.OK, this.notifyOk_);
  }
  goog.base(this, 'setModel', model);
  if (this.isInDocument()) {
    this.getHandler().listen(this.getModel(),
      bluemind.events.NotificationHandler.EventType.ERROR, this.notifyError_);
    this.getHandler().listen(this.getModel(),
      bluemind.events.NotificationHandler.EventType.NOTICE, this.notifyNotice_);
    this.getHandler().listen(this.getModel(),
      bluemind.events.NotificationHandler.EventType.INFO, this.notifyInfo_);
    this.getHandler().listen(this.getModel(),
      bluemind.events.NotificationHandler.EventType.OK, this.notifyOk_);    
    if (this.getModel().getLastNotification()) {
      var n = this.getModel().getLastNotification();
      this.notify_(n.message, n.type);
    }
  }
};

 /**
  * @return {bluemind.events.NotificationHandler} Notification model.
  * @override
  */
bluemind.ui.NotificationArea.prototype.getModel;

/** @override */
bluemind.ui.NotificationArea.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.timer_, goog.Timer.TICK, this.hide_);
  if (this.getModel()) {
    this.getHandler().listen(this.getModel(),
      bluemind.events.NotificationHandler.EventType.ERROR, this.notifyError_);
    this.getHandler().listen(this.getModel(),
      bluemind.events.NotificationHandler.EventType.NOTICE, this.notifyNotice_);
    this.getHandler().listen(this.getModel(),
      bluemind.events.NotificationHandler.EventType.INFO, this.notifyInfo_);
    this.getHandler().listen(this.getModel(),
      bluemind.events.NotificationHandler.EventType.OK, this.notifyOk_);
    if (this.getModel().getLastNotification()) {
      var n = this.getModel().getLastNotification();
      switch(n.type) {
        case bluemind.events.NotificationHandler.EventType.ERROR:
          this.notify_(n.message, goog.getCssName(bluemind.ui.NotificationArea.BASE_CSS, 'error'));
          break;
        case bluemind.events.NotificationHandler.EventType.OK:
          this.notify_(n.message, goog.getCssName(bluemind.ui.NotificationArea.BASE_CSS, 'ok'));
          break;
        case bluemind.events.NotificationHandler.EventType.INFO:
          this.notify_(n.message, goog.getCssName(bluemind.ui.NotificationArea.BASE_CSS, 'info'));
          break;
        case bluemind.events.NotificationHandler.EventType.NOTICE:
          this.notify_(n.message, goog.getCssName(bluemind.ui.NotificationArea.BASE_CSS, 'notice'));
          break;
      }
    }    
  }
};

/** @override */
bluemind.ui.NotificationArea.prototype.createDom = function() {
  goog.base(this, 'createDom');
  goog.dom.classlist.add(this.getElement(), bluemind.ui.NotificationArea.BASE_CSS);
};

/**
 * Notify an error
 * @param {goog.events.Event} e Notification event
 */
bluemind.ui.NotificationArea.prototype.notifyError_ = function(e) {
  var msg = e.msg;
  this.notify_(msg, goog.getCssName(bluemind.ui.NotificationArea.BASE_CSS, 'error'), true);
};

/**
 * Notify an notice
 * @param {goog.events.Event} e Notification event
 */
bluemind.ui.NotificationArea.prototype.notifyNotice_ = function(e) {
  var msg = e.msg;
  this.notify_(msg, goog.getCssName(bluemind.ui.NotificationArea.BASE_CSS, 'notice'));
};

/**
 * Notify an information
 * @param {goog.events.Event} e Notification event
 */
bluemind.ui.NotificationArea.prototype.notifyInfo_ = function(e) {
  var msg = e.msg;
  this.notify_(msg, goog.getCssName(bluemind.ui.NotificationArea.BASE_CSS, 'info'));
};

/**
 * Notify an 'ok' message
 * @param {goog.events.Event} e Notification event
 */
bluemind.ui.NotificationArea.prototype.notifyOk_ = function(e) {
  var msg = e.msg;
  this.notify_(msg, goog.getCssName(bluemind.ui.NotificationArea.BASE_CSS, 'ok'));
};

/**
 * Pop a message into the notification area
 * @param {string} msg Notification message
 * @param {string=} opt_css Message css classename
 * @param {boolean=} opt_sticky Prevent message to disappear
 */
bluemind.ui.NotificationArea.prototype.notify_ = function(msg, opt_css, opt_sticky) {
  if (this.visible_) {
    this.hide_();
  }
  var sticky = !!opt_sticky;
  var el = this.getDomHelper().createDom('div', bluemind.ui.NotificationArea.MESSAGE_CSS + ' ' + opt_css, msg); 
  if (sticky) {
    this.getDomHelper().appendChild(el, this.close_);
    this.getHandler().listen(this.close_, goog.events.EventType.CLICK, this.hide_);
  } else {
    this.timer_.start();
  }
  this.getDomHelper().appendChild(this.getElement(), el);
  this.visible_ = true;
};

/**
 * Hide notification
 * @private
 */
bluemind.ui.NotificationArea.prototype.hide_ = function() {
  this.getDomHelper().removeChildren(this.getElement());
  this.getHandler().unlisten(this.close_, goog.events.EventType.CLICK);
  this.timer_.stop();
  this.visible_ = false;
};
