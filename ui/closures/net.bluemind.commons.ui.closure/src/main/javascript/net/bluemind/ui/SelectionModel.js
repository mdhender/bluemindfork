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
 * @fileoverview A model for selection within a list.
 */

goog.provide('net.bluemind.ui.SelectionModel');

goog.require('goog.events.EventTarget');
goog.require('goog.structs.Map');

/**
 * Handle selection within a list. Dispatches a
 * {@link goog.events.EventType.SELECT} event when a selection is made.
 * 
 * @param {function(Object): *=} opt_provider Optional function to extract key
 *                from object.
 * @extends {goog.events.EventTarget}
 * @constructor
 */
net.bluemind.ui.SelectionModel = function(opt_provider) {
  goog.base(this);
  this.keyProvider = opt_provider || function(o) {
    if (typeof o.getId == 'function') {
      return o.getId();
    }
    return o;
  };
  this.selection = new goog.structs.Map();
};
goog.inherits(net.bluemind.ui.SelectionModel, goog.events.EventTarget);

/**
 * Method that give a unique comparable identifier for a selected object.
 * 
 * @type {function(Object): *}
 */
net.bluemind.ui.SelectionModel.prototype.keyProvider;

/**
 * Selected item list.
 * 
 * @type {goog.structs.Map}
 * @protected
 */
net.bluemind.ui.SelectionModel.prototype.selection;

/**
 * Selection handler function. Called with two arguments (the item to be
 * selected or deselected, and a Boolean indicating whether the item is to be
 * selected or deselected).
 * 
 * @type {Function}
 * @private
 */
net.bluemind.ui.SelectionModel.prototype.selectionHandler_ = null;

/**
 * Returns the selection handler function used by the selection model to change
 * the internal selection state of items under its control.
 * 
 * @return {Function} Selection handler function (null if none).
 */
net.bluemind.ui.SelectionModel.prototype.getSelectionHandler = function() {
  return this.selectionHandler_;
};

/**
 * Sets the selection handler function to be used by the selection model to
 * change the internal selection state of items under its control. The function
 * must take two arguments: an item and a Boolean to indicate whether the item
 * is to be selected or deselected. Selection handler functions are only needed
 * if the items in the selection model don't natively support the
 * {@code setSelected(Boolean)} interface.
 * 
 * @param {Function} handler Selection handler function.
 */
net.bluemind.ui.SelectionModel.prototype.setSelectionHandler = function(handler) {
  this.selectionHandler_ = handler;
};

/**
 * Check if an object is selected.
 * 
 * @param {Object} object the object
 * @return {boolean} true if selected, false if not
 */
net.bluemind.ui.SelectionModel.prototype.isSelected = function(object) {
  var key = this.keyProvider(object);
  return this.selection.containsKey(key);
};

/**
 * Set the selected state of an object and fire a
 * {@link goog.events.EventType.SELECT} if the selection has changed. Subclasses
 * should not fire an event in the case where selected is true and the object
 * was already selected, or selected is false and the object was not previously
 * selected.
 * 
 * @param {Object} object the object to select or deselect
 * @param {boolean} selected true to select, false to deselect
 * @return {boolean} True if selection has changed.
 */
net.bluemind.ui.SelectionModel.prototype.setSelected = function(object, selected) {
  var key = this.keyProvider(object);
  if (this.selection.containsKey(key) != selected) {
    this.selectInternal(key, object, selected);
    this.dispatchEvent(goog.events.EventType.SELECT);
    return true;
  }
  return false;
};

/**
 * Invert state of an object
 * 
 * @param {Object} object the object to select or deselect
 * @return {boolean} True if selection has changed.
 */
net.bluemind.ui.SelectionModel.prototype.toggle = function(object) {
  var key = this.keyProvider(object);
  return this.setSelected(object, !this.selection.containsKey(key));
};

/**
 * Proteted helper; selects or deselects the given item based on the value of
 * the {@code select} argument. If a selection handler has been registered (via
 * {@link #setSelectionHandler}, calls it to update the internal selection
 * state of the item. Otherwise, attempts to call {@code setSelected(Boolean)}
 * on the item itself, provided the object supports that interface.
 * 
 * @param {Object} object Item to select or deselect.
 * @param {boolean} select If true, the object will be selected; if false, it
 *                will be deselected.
 * @protected
 */
net.bluemind.ui.SelectionModel.prototype.selectItem = function(object, select) {
  if (object) {
    if (typeof this.selectionHandler_ == 'function') {
      this.selectionHandler_(object, select);
    } else if (typeof object.setSelected == 'function') {
      object.setSelected(select);
    }
  }
};

/**
 * 
 * @param {*} key Key object
 * @param {Object} object the object to select or deselect
 * @param {boolean} selected true to select, false to deselect
 * @protected
 */
net.bluemind.ui.SelectionModel.prototype.selectInternal = function(key, object, selected) {
  if (selected) {
    this.selection.set(key, object);
  } else {
    this.selection.remove(key);
  }
  this.selectItem(object, selected);
};

/**
 * Clear selection.
 */
net.bluemind.ui.SelectionModel.prototype.clear = function() {
  var dispatch = !this.selection.isEmpty();
  this.selection.clear();
  if (dispatch) {
    this.dispatchEvent(goog.events.EventType.SELECT);
  }
};

/**
 * Get selection.
 * 
 * @return {Array.<Object>} Selection values.
 */
net.bluemind.ui.SelectionModel.prototype.getSelection = function() {
  return this.selection.getValues();
};
