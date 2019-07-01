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
 * @fileoverview List of cartouche. 
 */

goog.provide('bluemind.ui.CartoucheBox');

goog.require('bluemind.ui.CartoucheBoxItem');
goog.require('bluemind.ui.style.CartoucheBoxRenderer');
goog.require('goog.ui.SelectionModel');
goog.require('goog.ui.Container');

/**
 * A CartoucheBox control.
 * @param {bluemind.ui.style.CartoucheBoxRenderer=} opt_renderer Renderer used to
 *   render or decorate the container; defaults to 
 *   {@link bluemind.ui.style.CartoucheBoxRenderer}.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @extends {goog.ui.Container}
 * @constructor
 */
bluemind.ui.CartoucheBox = function(opt_renderer, opt_domHelper) {
  goog.base(this, goog.ui.Container.Orientation.VERTICAL,
      opt_renderer || bluemind.ui.style.CartoucheBoxRenderer.getInstance(),
      opt_domHelper);  
  this.selectionModel_ = new goog.ui.SelectionModel();
  this.setFocusable(false);
};
goog.inherits(bluemind.ui.CartoucheBox, goog.ui.Container);

/**
 * The selection model controlling the items in the cartouche box.
 * @type {goog.ui.SelectionModel}
 * @private
 */
bluemind.ui.CartoucheBox.prototype.selectionModel_;


/** 
 * Get the input value that have generated this box.
 * @return {string} value
 **/
bluemind.ui.CartoucheBoxItem.prototype.getInputValue = function() {
  return "" + this.getValue();
};

/** @override */
bluemind.ui.CartoucheBox.prototype.addChildAt = function(control, index, opt_render) {
  goog.base(this, 'addChildAt', control, index, opt_render);
  this.selectionModel_.addItemAt(control, index);
  this.getHandler().listen(control, goog.ui.Component.EventType.SELECT, this.handleSelect_);
  this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
};

/** @override */
bluemind.ui.CartoucheBox.prototype.removeChild = function(control, opt_unrender) {
  var r = goog.base(this, 'removeChild', control, opt_unrender);
  this.selectionModel_.removeItem(r);
  this.getHandler().unlisten(r, goog.ui.Component.EventType.SELECT);
  this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
  return r;
};


/** @override */
bluemind.ui.CartoucheBox.prototype.highlightHelper = function(fn, startIndex) {
  if (startIndex == -1 && this.getSelectedIndex() >= 0) {
    startIndex = this.getSelectedIndex();
  }
  return goog.base(this, 'highlightHelper', fn, startIndex);
};


/**
 *
 *
 */
bluemind.ui.CartoucheBox.prototype.handleSelect_ = function(e) {
  var control = e.target;
  this.selectionModel_.setSelectedItem(control);
};

/** @override */
bluemind.ui.CartoucheBox.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var handler = this.getHandler();
  handler.listen(this.getDomHelper().getDocument(),
      goog.events.EventType.MOUSEDOWN, this.onDocClicked_);  
  //this.getHandler().listen(this.selectionModel_,
  //  goog.events.EventType.SELECT, this.handleSelectionChange);
};


/**
 * Set the given item as selected if it exists and is a child of the container;
 * otherwise un-select the currently selected item.
 * @param {goog.ui.Control} item Item to set as selected.
 */
bluemind.ui.CartoucheBox.prototype.setSelected = function(item) {
  if (this.selectionModel_) {
    this.selectionModel_.setSelectedItem(item);
  }  
  this.setHighlighted(item);
};


/**
 * Set the item at the given 0-based index (if any) as selected.  If another 
 * item was previously selected, it is un-selected.
 * @param {number} index Index of item to set as selected (-1 removes the 
 *   current select).
 */
bluemind.ui.CartoucheBox.prototype.setSelectedIndex = function(index) {
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
bluemind.ui.CartoucheBox.prototype.getSelected = function() {
  return this.selectionModel_ ?
      /** @type {goog.ui.Control} */ (this.selectionModel_.getSelectedItem()) :
      null;
};

/**
 * Returns the index of the currently selected option.
 * @return {number} 0-based index of the currently selected option (-1 if none).
 */
bluemind.ui.CartoucheBox.prototype.getSelectedIndex = function() {
  return this.selectionModel_ ? this.selectionModel_.getSelectedIndex() : -1;
};

/** @override */
bluemind.ui.CartoucheBox.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');

  if (this.selectionModel_) {
    this.selectionModel_.dispose();
    this.selectionModel_ = null;
  }
};

/**
 * Event handler for when the document is clicked.
 * @param {goog.events.BrowserEvent} e The browser event.
 * @private
 */
bluemind.ui.CartoucheBox.prototype.onDocClicked_ = function(e) {
  if (!goog.dom.contains(this.getElement(), /** @type {Node} */ (e.target))) {
    this.selectionModel_.setSelectedItem(null);
  }
};

/**
 * Delete a child (remove and dispose).
 * @param {bluemind.ui.CartoucheBoxItem} child Item to delete.
 */
bluemind.ui.CartoucheBox.prototype.deleteChild = function(child) {
  this.removeChild(child, true);
  child.dispose();
};

/**
 * Return the last child of the cartouche box 
 * @return {goog.ui.Control} Last cartouche; null if none.
 */
bluemind.ui.CartoucheBox.prototype.getLastChild = function() {
  var count = this.getChildCount();
  return this.getChildAt(count - 1);
};
