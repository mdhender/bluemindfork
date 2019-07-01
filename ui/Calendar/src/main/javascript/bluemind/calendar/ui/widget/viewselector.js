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
 * @fileoverview View selector component.
 */

goog.provide('bluemind.calendar.ui.widget.ViewSelector');

goog.require('bluemind.calendar.event.template');
goog.require('bluemind.calendar.model.CalendarView');
goog.require('bluemind.calendar.model.CalendarViewHome');
goog.require('bluemind.calendar.template.i18n');
goog.require('bluemind.net.OnlineHandler');
goog.require('goog.soy');
goog.require('goog.ui.PopupMenu');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.widget.ViewSelector = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.menu_ = new goog.ui.PopupMenu();
  this.selected_ = null;
};
goog.inherits(bluemind.calendar.ui.widget.ViewSelector, goog.ui.Component);

/**
 * Menu
 * @type {goog.ui.PopupMenu}
 */
bluemind.calendar.ui.widget.ViewSelector.prototype.menu_;

/**
 * Menu item
 * @type {bluemind.calendar.model.CalendarView}
 */
bluemind.calendar.ui.widget.ViewSelector.prototype.selected_;

/**
 * Get Menu
 * @return {goog.ui.PopupMenu} menu_.
 */
bluemind.calendar.ui.widget.ViewSelector.prototype.getMenu = function() {
  return this.menu_;
};

/**
 * set selected view
 * @param {bluemind.calendar.model.CalendarView} cv selected view.
 */
bluemind.calendar.ui.widget.ViewSelector.prototype.setSelected = function(cv) {
  this.selected_ = cv;
  var r = this.menu_.getChildAt(1);
  if (cv == null) {
    r.setEnabled(false);
    this.getHandler().unlisten(goog.dom.getElement('view-delete-action'),
      goog.events.EventType.CLICK,
      this.showDeleteDialog_);
  } else {
    r.setEnabled(true);
    this.getHandler().listen(goog.dom.getElement('view-delete-action'),
      goog.events.EventType.CLICK,
      this.showDeleteDialog_);
  }
};

/**
 * Selected view
 * @return {bluemind.calendar.model.CalendarView} selected view.
 */
bluemind.calendar.ui.widget.ViewSelector.prototype.getSelected = function() {
  return this.selected_;
};

/** @inheritDoc */
bluemind.calendar.ui.widget.ViewSelector.prototype.createDom = function() {
  this.element_ = goog.soy.renderAsElement(
    bluemind.calendar.template.viewSelector);
  this.setElementInternal(/** @type {Element} */ (this.element_));

};

/** @inheritDoc */
bluemind.calendar.ui.widget.ViewSelector.prototype.enterDocument =
  function() {
  bluemind.calendar.ui.widget.ViewSelector.superClass_.enterDocument.call(
    this);

  this.getHandler().listen(goog.dom.getElement('view-add-action'),
    goog.events.EventType.CLICK,
    this.showAddDialog_);

  this.getHandler().listen(goog.dom.getElement('view-my-calendar-action'),
    goog.events.EventType.CLICK,
    this.showMyCalendar_);

  this.menu_.setToggleMode(true);
  this.menu_.decorate(goog.dom.getElement('view-menu'));
  this.menu_.attach(goog.dom.getElement('view-button'),
    goog.positioning.Corner.BOTTOM_LEFT,
    goog.positioning.Corner.TOP_LEFT);

  this.menu_.setEnabled(bluemind.net.OnlineHandler.getInstance().isOnline());
  var title = bluemind.net.OnlineHandler.getInstance().isOnline() ?
    '' : bluemind.calendar.template.i18n.onlineOnly();
  this.menu_.getElement().title = title;
  this.getHandler().listen(bluemind.net.OnlineHandler.getInstance(), 'offline',
    function() {
    this.menu_.setEnabled(false);
    this.menu_.getElement().title =
      bluemind.calendar.template.i18n.onlineOnly();
  });
  this.getHandler().listen(bluemind.net.OnlineHandler.getInstance(), 'online',
    function() {
    this.refreshViewSelector();
    this.menu_.setEnabled(true);
    this.setSelected(this.selected_);
    this.menu_.getElement().title = '';
  });

  this.refreshViewSelector();
  this.setSelected(null);
};

/**
 * Show add view dialog
 * @private
 */
bluemind.calendar.ui.widget.ViewSelector.prototype.showAddDialog_ =
  function() {
  bluemind.calendar.Controller.getInstance().saveViewDialog();
};

/**
 * Show delete view dialog
 * @private
 */
bluemind.calendar.ui.widget.ViewSelector.prototype.showDeleteDialog_ =
  function() {
  bluemind.calendar.Controller.getInstance().removeViewDialog();
};

/**
 * Show my calendar
 * @private
 */
bluemind.calendar.ui.widget.ViewSelector.prototype.showMyCalendar_ =
  function() {
  bluemind.calendar.Controller.getInstance().showMyCalendar();
};

/**
 * Refresh view selector
 */
bluemind.calendar.ui.widget.ViewSelector.prototype.refreshViewSelector =
  function() {
  if (bluemind.net.OnlineHandler.getInstance().isOnline()) {
    bluemind.calendar.model.CalendarViewHome.getInstance()
      .getViews(this.refreshCallback_);
  }
};

/**
 * Refresh view selector callback
 * @param {Object} r Response object.
 * @private
 */
bluemind.calendar.ui.widget.ViewSelector.prototype.refreshCallback_ =
  function(r) {
  var views = r['views'];
  bluemind.savedViews = new goog.structs.Map();

  // Clear current saved views
  var menu = bluemind.view.getViewSelector().getMenu();
  var toRemove = new Array();
  menu.forEachChild(function(c, idx) {
    if (idx >= 5) {
      goog.array.insert(toRemove, c);
    }
  });
  goog.array.forEach(toRemove, function(r) {
    this.removeChild(r, true);
  }, menu);

  if (views.length > 0) {
    goog.array.forEach(views, function(v) {
      bluemind.calendar.Controller.getInstance().registerCalendarView(v);
    });
  }
};

