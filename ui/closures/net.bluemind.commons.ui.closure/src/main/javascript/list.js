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
 * @fileoverview A list control with a select option.
 *
 */

goog.provide("net.bluemind.ui.List");

goog.require("goog.events.EventType");
goog.require("goog.ui.Container");
goog.require("goog.ui.SelectionModel");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Component.State");
goog.require("goog.ui.Container.Orientation");
goog.require("net.bluemind.ui.ListItem");
goog.require("net.bluemind.ui.style.ListRenderer");


/**
 * A basic list.
 * @param {net.bluemind.ui.style.ListRenderer=} opt_renderer Renderer used to
 *   render or decorate the container; defaults to 
 *   {@link net.bluemind.ui.style.ListRenderer}.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Container}
 */
net.bluemind.ui.List = function(opt_renderer, opt_domHelper) {
  goog.base(this, goog.ui.Container.Orientation.VERTICAL,
      opt_renderer || net.bluemind.ui.style.ListRenderer.getInstance(),
      opt_domHelper);  
  this.selectionModel_ = new goog.ui.SelectionModel();
  this.setFocusable(false);
  this.setOpenFollowsHighlight(false);
  this.items_ = {};
};
goog.inherits(net.bluemind.ui.List, goog.ui.Container);

/**
 * The selection model controlling the items in the menu.
 * @type {goog.ui.SelectionModel}
 * @private
 */
net.bluemind.ui.List.prototype.selectionModel_;

/**
 * Map of item IDs to item components.  Used for constant-time
 * random access to item components by ID.  
 * @type {Object.<string, net.bluemind.ui.ListItem>}
 * @private
 */
net.bluemind.ui.List.prototype.items_;


/** @override */
net.bluemind.ui.List.prototype.addChildAt = function(control, index, opt_render) {
  goog.base(this, 'addChildAt', control, index, opt_render);
  if (control instanceof net.bluemind.ui.ListItem) {
    control.setList(this);
  }
};

/** @override */
net.bluemind.ui.List.prototype.removeChild = function(control, opt_unrender) {
  var r = goog.base(this, 'removeChild', control, opt_unrender);
  if (control instanceof net.bluemind.ui.ListItem) {
    control.setList(null);
  }  
  return r;
};

/**
 * Set the given item as selected if it exists and is a child of the container;
 * otherwise un-select the currently selected item.
 * @param {goog.ui.Control} item Item to set as selected.
 */
net.bluemind.ui.List.prototype.setSelected = function(item) {
  if (!goog.isObject(item) && goog.isString(item)) {
    item = this.getItem(item);
  }
  if (item == null || item.isSupportedState(goog.ui.Component.State.SELECTED)) {
    this.selectionModel_.setSelectedItem(item);
  }
};


/**
 * Set the item at the given 0-based index (if any) as selected.  If another 
 * item was previously selected, it is un-selected.
 * @param {number} index Index of item to set as selected (-1 removes the 
 *   current select).
 */
net.bluemind.ui.List.prototype.setSelectedIndex = function(index) {
  if (this.selectionModel_) {
    this.setSelected(
      /** @type {goog.ui.Control} */ (this.selectionModel_.getItemAt(index)));
  }
  this.setHighlightedIndex(index);
};  

/**
 * Returns the currently selected item (if any).
 * @return {goog.ui.Control?} Selected item (null if none).
 */
net.bluemind.ui.List.prototype.getSelected = function() {
  return this.selectionModel_ ?
      /** @type {goog.ui.Control} */ (this.selectionModel_.getSelectedItem()) :
      null;
};

/**
 * Returns the index of the currently selected option.
 * @return {number} 0-based index of the currently selected option (-1 if none).
 */
net.bluemind.ui.List.prototype.getSelectedIndex = function() {
  return this.selectionModel_ ? this.selectionModel_.getSelectedIndex() : -1;
};

/**
 * @return {goog.ui.SelectionModel} The selection model.
 * @protected
 */
net.bluemind.ui.List.prototype.getSelectionModel = function() {
  return this.selectionModel_;
};

/**
 * Handles selection change events dispatched by the selection model.
 * @param {goog.events.Event} e Selection event to handle.
 */
net.bluemind.ui.List.prototype.handleSelectionChange = function(e) {
};

/** 
 * Handles {@link goog.ui.Component.EventType.ACTION} events dispatched by
 * the list item clicked by the user.  Updates the selection model.
 * @param {goog.events.Event} e Action event to handle.
 */
net.bluemind.ui.List.prototype.handleActionItem = function(e) {
  this.setSelected(/** @type {net.bluemind.ui.ListItem} */ (e.target));
};

/** @override */
net.bluemind.ui.List.prototype.exitDocument = function() {
  goog.base(this, 'exitDocument');
};

/** @override */
net.bluemind.ui.List.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.selectionModel_,
    goog.events.EventType.SELECT, this.handleSelectionChange);
  this.getHandler().listen(this,
    goog.ui.Component.EventType.ACTION, this.handleActionItem);
};

/** @override */
net.bluemind.ui.List.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');

  if (this.selectionModel_) {
    this.selectionModel_.dispose();
    this.selectionModel_ = null;
  }
};

/**
 * Notify list that the given item has been added or had been updated
 * @param {net.bluemind.ui.ListItem} item New item that has just been added.
 */
net.bluemind.ui.List.prototype.addItem = function(item) {
  if (item.isSupportedState(goog.ui.Component.State.SELECTED)) {
    this.selectionModel_.addItem(item);
    this.items_[item.getId()] = item;
  }
};


/**
 * Notify list that the given item is being removed from the
 * list.
 * @param {net.bluemind.ui.ListItem} item Item being removed.
 */
net.bluemind.ui.List.prototype.removeItem = function(item) {
  this.selectionModel_.removeItem(item);
  this.items_[item.getId()] = null;
  delete this.items_[item.getId()];
};

/**
 * Get an item from the list tree.
 * @param {string} uid Item uid.
 * @return {net.bluemind.ui.ListItem} Return the item with the given id.
 */
net.bluemind.ui.List.prototype.getItem = function(uid) {
  return this.items_[uid];
};
