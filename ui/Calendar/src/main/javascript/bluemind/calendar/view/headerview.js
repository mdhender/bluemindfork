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
 * An MVP-style view class for application header.
 */

goog.provide('bluemind.cal.view.HeaderView');

goog.require('bluemind.ui.Banner');
goog.require('bluemind.ui.SearchField');
goog.require('bluemind.ui.NotificationArea');
goog.require('net.bluemind.cal.template.header');
goog.require('goog.ui.Component');
goog.require('goog.ui.Component.EventType');
goog.require('goog.string');


/**
 * An MVP-style view class for folder screens.
 *
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.cal.view.HeaderView = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.banner_ = new bluemind.ui.Banner(opt_domHelper);
  this.search_ = new bluemind.ui.SearchField(null, null, opt_domHelper);
  this.notif_ = new bluemind.ui.NotificationArea(opt_domHelper);
  this.notif_.setId('notification');
  this.addChild(this.notif_);
  this.addChild(this.banner_);
  this.addChild(this.search_);
};
goog.inherits(bluemind.cal.view.HeaderView, goog.ui.Component);

/**
 * Search widget.
 *
 * @type {bluemind.ui.SearchField}
 * @private
 */
bluemind.cal.view.HeaderView.prototype.search_;

/**
 * Notification area widget.
 *
 * @type {bluemind.ui.NotificationArea}
 * @private
 */
bluemind.cal.view.HeaderView.prototype.notif_;

/**
 * Banner widget.
 *
 * @type {bluemind.ui.Banner}
 * @private
 */
bluemind.cal.view.HeaderView.prototype.banner_;

/** @override */
bluemind.cal.view.HeaderView.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  this.banner_.setModel(model);
};

/** @override */
bluemind.cal.view.HeaderView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.decorateInternal(this.getElement());
};

/** @override */
bluemind.cal.view.HeaderView.prototype.decorateInternal = function(el) {
  goog.base(this, 'decorateInternal', el);
// FIXME el.innerHTML = net.bluemind.cal.template.header.main({user: this.getModel().serialize()});
  el.innerHTML = net.bluemind.cal.template.header.main({user: {}});
  this.banner_.decorate(goog.dom.getElementByClass(goog.getCssName('banner'), el));
  this.search_.render(goog.dom.getElementByClass(goog.getCssName('search'), el));
  this.notif_.render(this.getElement());
};

/** @override */
bluemind.cal.view.HeaderView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.search_, goog.ui.Component.EventType.ACTION, this.handleSearchEvent_);
  this.getHandler().listen(this.banner_.getChild('dialer'), goog.ui.Component.EventType.ACTION, this.handleCallEvent_);
};

/**
 * set search field value 
 * @param {string} pattern Search field value 
 */
bluemind.cal.view.HeaderView.prototype.setSearchPattern = function(pattern) {
  var values = pattern.split(new RegExp('[' + this.search_.getSeparators() + ']'));
  this.search_.setValue(values);
};

/**
 * Attempts to handle search action. 
 * @param {goog.events.Event} e Key event to handle.
 * @private
 */
bluemind.cal.view.HeaderView.prototype.handleSearchEvent_ = function(e) {
  var pattern = this.search_.getValue().join(' ');
  if (pattern.length > 0) {
    var loc = this.getDomHelper().getWindow().location;
    loc.hash = "/search/query/" + goog.string.urlEncode(pattern);
  }
};

/**
 * Handle dial action. 
 * @param {bluemind.ui.dialer.CallEvent} e Dial event to handle.
 * @private
 */
bluemind.cal.view.HeaderView.prototype.handleCallEvent_ = function(e) {
  e.type = 'call';
  e.target = this;
  this.dispatchEvent(e);
};

/**
 * Set unread mail
 * @param {number} count Unread mail count
 */
bluemind.cal.view.HeaderView.prototype.setUnreadMail = function(count) {
  this.banner_.setUnreadMail(count);
};

/**
 * Set pending event 
 * @param {number} count pending events count
 */
bluemind.cal.view.HeaderView.prototype.setPendingEvents = function(count) {
  this.banner_.setPendingEvents(count);
};
