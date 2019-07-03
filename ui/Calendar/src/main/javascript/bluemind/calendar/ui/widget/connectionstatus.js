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
 * @fileoverview online status component.
 */

goog.provide('bluemind.calendar.ui.widget.ConnectionStatus');

goog.require('bluemind.calendar.template');
goog.require('bluemind.calendar.template.i18n');
goog.require('bluemind.net.OnlineHandler');
goog.require('bluemind.sync.SyncEngine');
goog.require('goog.Timer');
goog.require('goog.dom');
goog.require('goog.dom.classes');
goog.require('goog.style');
goog.require('goog.ui.Component');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.widget.ConnectionStatus = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
};
goog.inherits(bluemind.calendar.ui.widget.ConnectionStatus, goog.ui.Component);

/** @inheritDoc */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.createDom = function() {
  this.element_ = goog.soy.renderAsElement(
    bluemind.calendar.template.connectionStatus);
  this.setElementInternal(/** @type {Element} */ (this.element_));
};

/**
 * @param {boolean} online online.
 */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.online_;

/**
 * @return {boolean} online online.
 */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.isOnline = function() {
  return this.online_;
};

/**
 * Switch status
 */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.switchStatus =
  function() {
  this.setStatus(!this.online_);
};

/**
 * Set status
 * @param {boolean} online online status.
 */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.setStatus =
  function(online) {
  bluemind.net.OnlineHandler.getInstance().setEnabled(online);
  this.setCurrentState_();
};

/**
 * Set online
 * @param {Object} e e.
 */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.setOnline =
  function(e) {
  goog.dom.classes.set(this.element_, goog.getCssName('connection-status'));
  goog.dom.classes.add(this.element_, goog.getCssName('online'));
  goog.dom.setProperties(this.element_,
    {'title': bluemind.calendar.template.i18n.onlineStatus()});
  goog.dom.setTextContent(this.element_,
    bluemind.calendar.template.i18n.online());
  this.online_ = true;
  if (e != null) {
    bluemind.notification.show(
      bluemind.calendar.template.i18n.onlineStatus());
  }
};

/**
 * Set offline
 * @param {Object} e e.
 */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.setOffline =
  function(e) {
  goog.dom.classes.set(this.element_, goog.getCssName('connection-status'));
  goog.dom.classes.add(this.element_, goog.getCssName('offline'));
  var text, label;
  if (bluemind.net.OnlineHandler.getInstance().isEnabled()) {
    text = bluemind.calendar.template.i18n.disconnectedStatus();
    label = bluemind.calendar.template.i18n.disconnected();
  } else {
    this.online_ = false;
    text = bluemind.calendar.template.i18n.offlineStatus();
    label = bluemind.calendar.template.i18n.offline();
  }
  goog.dom.setProperties(this.element_, {'title': text});
  goog.dom.setTextContent(this.element_, label);
  if (e != null) {
    bluemind.notification.show(text);
  }
};

/**
 * Set syncing
 */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.setSyncing =
  function() {
  goog.dom.classes.set(this.element_, goog.getCssName('connection-status'));
  goog.dom.classes.add(this.element_, goog.getCssName('sync'));
  goog.dom.setProperties(this.element_,
    {'title': bluemind.calendar.template.i18n.syncStatus()});
};

/**
 * Sync offline icon with the current syncengine status
 * @private
 */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.setCurrentState_ =
  function() {
  if (bluemind.net.OnlineHandler.getInstance().isOnline()) {
    this.setOnline();
  } else {
    this.setOffline();
  }
};

/** @inheritDoc */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.element_, goog.events.EventType.CLICK,
    this.showDialog_);
  this.getHandler().listen(bluemind.net.OnlineHandler.getInstance(), 'online',
    this.setOnline, false, this);
  this.getHandler().listen(bluemind.net.OnlineHandler.getInstance(), 'offline',
    this.setOffline, false, this);

  this.getHandler().listen(bluemind.sync.SyncEngine.getInstance(), 'start',
    this.setSyncing, false, this);
  this.getHandler().listen(bluemind.sync.SyncEngine.getInstance(), 'stop',
    this.setCurrentState_, false, this);
  this.setCurrentState_();
};


/**
 * Display switch connection dialog.
 * @private
 */
bluemind.calendar.ui.widget.ConnectionStatus.prototype.showDialog_ =
  function() {
  bluemind.calendar.Controller.getInstance().connectionStatusDialog(this);
};
