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
 * @fileoverview A class for representing items in lists.
 * @see net.bluemind.ui.List
 * 
 */

goog.provide("net.bluemind.ui.ListItem");

goog.require("goog.array");
goog.require("goog.string");
goog.require("goog.ui.Control");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.ui.style.ListItemRenderer");

/**
 * Class representing an item in a list.
 * 
 * @param {goog.ui.ControlContent} content Text caption or DOM structure to
 *          display as the content of the item (use to add icons or styling to
 *          lists).
 * @param {*=} opt_model Data/model associated with the list item.
 * @param {net.bluemind.ui.style.ListItemRenderer=} opt_renderer Optional
 *          renderer.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper used for
 *          document interactions.
 * @constructor
 * @extends {goog.ui.Control}
 */
net.bluemind.ui.ListItem = function(content, opt_model, opt_renderer, opt_domHelper) {
  goog.base(this, content, opt_renderer || net.bluemind.ui.style.ListItemRenderer.getInstance(), opt_domHelper);
  this.setValue(opt_model);
  this.setSupportedState(goog.ui.Component.State.SELECTED, true);
};
goog.inherits(net.bluemind.ui.ListItem, goog.ui.Control);

/**
 * Tooltip
 * 
 * @protected {string}
 */
net.bluemind.ui.ListItem.prototype.tooltip_;

/**
 * Parent list
 * 
 * @protected {net.bluemind.ui.List}
 */
net.bluemind.ui.ListItem.prototype.list;

/**
 * Previous selectable sibling in list
 * 
 * @protected {net.bluemind.ui.ListItem}
 */
net.bluemind.ui.ListItem.prototype.previous;

/**
 * Previous selectable sibling in list
 * 
 * @protected {net.bluemind.ui.ListItem}
 */
net.bluemind.ui.ListItem.prototype.next;

/**
 * Returns the value associated with the list item. The default implementation
 * returns the model object associated with the item (if any), or its caption.
 * 
 * @return {*} Value associated with the list item, if any, or its caption.
 */
net.bluemind.ui.ListItem.prototype.getValue = function() {
  var model = this.getModel();
  return model != null ? model : this.getCaption();
};

/**
 * Sets the value associated with the list item. The default implementation
 * stores the value as the model of the list item.
 * 
 * @param {*} value Value to be associated with the list item.
 */
net.bluemind.ui.ListItem.prototype.setValue = function(value) {
  this.setModel(value);
};

/**
 * Sets the list item to be selectable or not. Set to true for list items that
 * represent selectable options.
 * 
 * @param {boolean} selectable Whether the list item is selectable.
 */
net.bluemind.ui.ListItem.prototype.setSelectable = function(selectable) {
  this.setSupportedState(goog.ui.Component.State.SELECTED, selectable);
};

/**
 * Returns the text caption of the component while ignoring accelerators.
 * 
 * @override
 */
net.bluemind.ui.ListItem.prototype.getCaption = function() {
  var content = this.getContent();
  if (goog.isArray(content)) {
    var caption = goog.array.map(content, function(node) {
      return goog.dom.getRawTextContent(node);
    }).join('');
    return goog.string.collapseBreakingSpaces(caption);
  }
  return goog.base(this, 'getCaption');
};

/**
 * Method used to set the list control on the item.
 * 
 * @param {net.bluemind.ui.List} list The item parent list.
 */
net.bluemind.ui.ListItem.prototype.setList = function(list) {
  if (list != this.list) {
    if (list == null) {
      this.list.removeItem(this);
    } else {
      list.addItem(this);
    }
    this.list = list;
    this.forEachChild(function(child) {
      if (child instanceof net.bluemind.ui.ListItem) {
        child.setList(list);
      }
    });
  }
};

/**
 * Returns the tooltip for the button.
 * 
 * @return {string|undefined} Tooltip text (undefined if none).
 */
net.bluemind.ui.ListItem.prototype.getTooltip = function() {
  return this.tooltip_;
};

/**
 * Sets the tooltip for the button, and updates its DOM.
 * 
 * @param {string} tooltip New tooltip text.
 */
net.bluemind.ui.ListItem.prototype.setTooltip = function(tooltip) {
  this.tooltip_ = tooltip;
  this.getRenderer().setTooltip(this.getElement(), tooltip);
};
