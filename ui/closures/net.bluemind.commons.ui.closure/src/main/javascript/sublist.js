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
 * @fileoverview A class for representing zippable container for items in lists.
 * @see net.bluemind.ui.List
 * 
 */

goog.provide("net.bluemind.ui.SubList");

goog.require("goog.ui.AnimatedZippy");
goog.require("goog.ui.Component.State");
goog.require("goog.ui.Zippy.Events");
goog.require("net.bluemind.ui.ListItem");
goog.require("net.bluemind.ui.style.SubListRenderer");

/**
 * 
 * Class representing a sublist that can be added as an item to other list.
 * 
 * @param {goog.ui.ControlContent} content Text caption or DOM structure to
 *          display as the content of the item (use to add icons or styling to
 *          lists).
 * @param {net.bluemind.ui.style.ListItemRenderer=} opt_renderer Optional
 *          renderer.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper used for
 *          document interactions.
 * @constructor
 * @extends {net.bluemind.ui.ListItem}
 */
net.bluemind.ui.SubList = function(content, opt_renderer, opt_domHelper) {
  goog.base(this, content, null, opt_renderer
      || net.bluemind.ui.style.SubListRenderer.getInstance(), opt_domHelper);
  this.setSelectable(false);
  this.setSupportedState(goog.ui.Component.State.OPENED, true);
  this.setState(goog.ui.Component.State.OPENED, true);
  this.setAutoStates(goog.ui.Component.State.OPENED, false);
};
goog.inherits(net.bluemind.ui.SubList, net.bluemind.ui.ListItem);

/**
 * @type {goog.ui.Zippy}
 */
net.bluemind.ui.SubList.prototype.zippy_;

/**
 * Returns the DOM element into which component caption are to be rendered,
 * or null if the control itself hasn't been rendered yet.  
 * @return {Element} Element to contain child elements (null if none).
 */
net.bluemind.ui.SubList.prototype.getCaptionElement = function() {
  return this.renderer_.getCaptionElement(this.getElement());
};


/** @override */
net.bluemind.ui.SubList.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.setVisible(this.hasChildren());
  if (!this.zippy_) {
    var head = this.getCaptionElement().parentElement;
    var content = this.getContentElement();
    var open = this.hasState(goog.ui.Component.State.OPENED);
    this.zippy_ = new goog.ui.AnimatedZippy(head, content, open);
    this.getHandler().listen(this.zippy_, goog.ui.Zippy.Events.TOGGLE,
        this.toggle_);
  }
};

/**
 * Sync sublist visible state with zippy.
 * 
 * @param {goog.ui.ZippyEvent} e Toggle event
 * @private
 */
net.bluemind.ui.SubList.prototype.toggle_ = function(e) {
  this.setOpen(e.expanded);
};

/**
 * Hide the sublist.
 */
net.bluemind.ui.SubList.prototype.collapse = function() {
  this.setOpen(false);
};

/**
 * Show the sublist.
 */
net.bluemind.ui.SubList.prototype.expand = function() {
  this.setOpen(true);
};

/** @override */
net.bluemind.ui.SubList.prototype.setOpen = function(visible) {
  goog.base(this, 'setOpen', visible);
  if (this.isInDocument()) {
    // Should use zippy to expand ?
  }
};

/** @override */
net.bluemind.ui.SubList.prototype.addChildAt = function(child, index,
    opt_render) {
  goog.base(this, 'addChildAt', child, index, opt_render);
  if (child instanceof net.bluemind.ui.ListItem) {
    var prev = /** @type {net.bluemind.ui.ListItem} */
    (this.getChildAt(index - 1));
    var next = /** @type {net.bluemind.ui.ListItem} */
    (this.getChildAt(index + 1));
    child.previous = prev;
    child.next = next;
    if (prev) {
      prev.next = child;
    }
    if (next) {
      next.previous = child;
    }
    if (this.list) {
      child.setList(this.list);
    }

    this.setVisible(true);
  }
};

net.bluemind.ui.SubList.prototype.removeChild = function(childNode,
    opt_unrender) {
  // In reality, this only accepts BaseNodes.
  var child = /** @type {net.bluemind.ui.ListItem} */
  (childNode);

  // if we remove selected or tree with the selected we should select this
  goog.base(this, 'removeChild', childNode, opt_unrender);

  if (child.previous) {
    child.previous.next = child.next;
  }
  if (child.next) {
    child.next.previous = child.previous;
  }
  child.setList(null);
  this.setVisible(this.hasChildren());
  return child;
};
