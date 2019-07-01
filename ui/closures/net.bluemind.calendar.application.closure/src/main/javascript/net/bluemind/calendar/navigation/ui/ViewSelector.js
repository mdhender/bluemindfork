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

goog.provide('net.bluemind.calendar.navigation.ui.ViewSelector');

goog.require('net.bluemind.calendar.navigation.templates');
goog.require('net.bluemind.net.OnlineHandler');
goog.require('goog.soy');
goog.require('goog.ui.PopupMenu');
goog.require('net.bluemind.calendar.navigation.events.EventType');
goog.require('net.bluemind.calendar.navigation.ui.DeleteViewDialog');
goog.require('net.bluemind.calendar.navigation.ui.SaveViewDialog');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
/**
 * @author mehdi
 *
 */
net.bluemind.calendar.navigation.ui.ViewSelector = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  /** @meaning calendar.view.myViews*/
  var MSG_VIEWS = goog.getMsg('My views');
  var child = new goog.ui.MenuButton(MSG_VIEWS);
  child.setId('menu-view');
  this.addChild(child, true);
  child.getMenu();
  child.setPositionElement(child.getElementByClass(goog.getCssName('goog-menu-button-dropdown')));
  
  child = new net.bluemind.calendar.navigation.ui.SaveViewDialog();
  child.setId('save-view');
  this.addChild(child, true);
  child = new net.bluemind.calendar.navigation.ui.DeleteViewDialog();
  child.setId('delete-view');
  this.addChild(child, true);
  this.selected_ = null;
};
goog.inherits(net.bluemind.calendar.navigation.ui.ViewSelector, goog.ui.Component);

/**
 * Menu item
 * 
 * @type {Object}
 */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.selected_;

net.bluemind.calendar.navigation.ui.ViewSelector.prototype.currentViews_ = [];

/**
 * @private
 */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.initializeMenu_ = function() {
  var menu = this.getChild('menu-view');
  menu.getMenu().removeChildren(true);

  /** @meaning calendar.view.goDefault */
  var MSG_GO_DEFAULT = goog.getMsg('Back to Home');
  var item = new goog.ui.MenuItem(MSG_GO_DEFAULT);
  item.setId('go-default');
  menu.addItem(item);
  /** @meaning calendar.view.updateDefault */
  var MSG_UPDATE_DEFAULT = goog.getMsg('Set as Home');
  item = new goog.ui.MenuItem(MSG_UPDATE_DEFAULT);
  item.setId('update-default');
  menu.addItem(item);
  item = new goog.ui.MenuSeparator();
  menu.addItem(item);
  /** @meaning calendar.view.save */
  var MSG_SAVE_VIEW = goog.getMsg('Save this view');
  item = new goog.ui.MenuItem(MSG_SAVE_VIEW);
  item.setId('save-view');
  menu.addItem(item);
  /** @meaning calendar.view.delete */
  var MSG_DELETE_VIEW = goog.getMsg('Delete this view');
  item = new goog.ui.MenuItem(MSG_DELETE_VIEW);
  item.setId('delete-view');
  item.setEnabled(goog.isDefAndNotNull(this.selected_) && !this.selected_['isDefault']);
  menu.addItem(item);
  item = new goog.ui.MenuSeparator();
  menu.addItem(item);
  goog.array.forEach(this.getModel() || [], function(view) {
    if (view.isDefault) {
      menu.getMenu().getChild('go-default').setModel(view);
      menu.getMenu().getChild('update-default').setModel(view);
    } else {
      var item = new goog.ui.MenuItem(view.label);
      item.setId(view.uid);
      item.setModel(view);
      menu.addItem(item);
    }
  }, this);
}

/**
 * @param {goog.event} cv selected view.
 */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.onMenuAction_ = function(e) {
  var item = e.target;
  var model = item.getModel();
  switch (item.getId()) {
  case 'go-default':
    this.dispatchEvent(new net.bluemind.calendar.navigation.events.ShowViewEvent(model.uid));
    break;
  case 'update-default':
    this.dispatchEvent(new net.bluemind.calendar.navigation.events.SaveViewEvent(model.uid, model.label));
    break;
  case 'save-view':
    this.showAddDialog_();
    break;
  case 'delete-view':
    this.showDeleteDialog_();
    break;
  default:
    if (model.uid) {
      this.dispatchEvent(new net.bluemind.calendar.navigation.events.ShowViewEvent(model.uid));
    }
  }

};

/**
 * set selected view
 * 
 * @param {Object} cv selected view.
 */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.setSelected = function(view) {
  this.selected_ = view['value'];
  var deleteViewEntry = this.getChild('menu-view').getMenu().getChild('delete-view');
  deleteViewEntry.setEnabled(goog.isDefAndNotNull(view) && !this.selected_['isDefault']);
  
  this.getChild('save-view').setModel({
    selected : view['uid'],
    views : this.getModel()
  });

  this.getChild('delete-view').setModel(view['uid']);
};

/**
 * Selected view
 * 
 * @return {Object} selected view.
 */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.getSelected = function() {
  return this.selected_;
};

/** @override */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.createDom = function() {
  goog.base(this, 'createDom');
  goog.dom.classlist.add(this.getElement(), goog.getCssName('view-selector'));
};

/** @override */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  
  var menu = this.getChild('menu-view');
  this.getHandler().listen(menu.getMenu(), goog.ui.Component.EventType.ACTION, this.onMenuAction_);

  menu.setEnabled(net.bluemind.net.OnlineHandler.getInstance().isOnline());

  /** @meaning general.offline.notAvailable */
  var MSG_ONLINE_ONLY = goog.getMsg('You must be online to use this feature.');
  var title = net.bluemind.net.OnlineHandler.getInstance().isOnline() ? '' : MSG_ONLINE_ONLY;
  menu.getElement().title = title;
  
};

/**
 * Show add view dialog
 * 
 * @private
 */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.showAddDialog_ = function() {
  this.getChild('save-view').setVisible(true);
};

/**
 * Show delete view dialog
 * 
 * @private
 */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.showDeleteDialog_ = function() {
  this.getChild('delete-view').setVisible(true);
};

/**
 * Show my calendar
 * 
 * @private
 */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.showMyCalendar_ = function() {
  this.dispatchEvent(net.bluemind.calendar.navigation.events.EventType.SHOW_MY_CALENDAR);
};

/** @override */
net.bluemind.calendar.navigation.ui.ViewSelector.prototype.setModel = function(views) {
  goog.base(this, 'setModel', views);
  
  this.initializeMenu_();
  
  this.getChild('save-view').setModel({
    selected : this.selected_,
    views : views
  });
}
