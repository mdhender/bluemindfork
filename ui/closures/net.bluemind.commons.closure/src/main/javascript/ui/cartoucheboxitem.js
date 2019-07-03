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
 * @fileoverview Widget composed of a text and a close button that has the form
 * of a cartouche.
 **/


goog.provide('bluemind.ui.CartoucheBoxItem');

goog.require('bluemind.ui.style.CartoucheBoxItemRenderer');
goog.require('goog.ui.Control');
goog.require('goog.ui.FlatMenuButtonRenderer');
goog.require('goog.ui.Button');

/**
 * Class representing an cartouche box item in a cartouche.
 *
 * @param {goog.ui.ControlContent} content Text caption or DOM structure to
 *     display as the content of the item (use to add icons or styling to
 *     items).
 * @param {*=} opt_model Data/model associated with the cartouche.
 * @param {bluemind.ui.style.CartoucheBoxItemRenderer=} opt_renderer Optional renderer.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper used for
 *     document interactions.
 * @constructor
 * @extends {goog.ui.Control}
 */
bluemind.ui.CartoucheBoxItem =
  function(content, opt_model, opt_renderer, opt_domHelper) {
  goog.base(this, content, opt_renderer ||
      bluemind.ui.style.CartoucheBoxItemRenderer.getInstance(), opt_domHelper);
  if (opt_model) {
    this.setValue(opt_model);
  }
  this.setSupportedState(goog.ui.Component.State.READONLY, true);
  this.setSupportedState(goog.ui.Component.State.SELECTED, true);
  this.setDispatchTransitionEvents(goog.ui.Component.State.SELECTED, true);
  var button = new goog.ui.Button('', goog.ui.FlatButtonRenderer.getInstance());
  button.setId('close');
  this.addChild(button, true);
};
goog.inherits(bluemind.ui.CartoucheBoxItem, goog.ui.Control);


/**
 * Map of DOM IDs to child controls.  Each key is the DOM ID of a child
 * control's root element; each value is a reference to the child control
 * itself.  Used for looking up the child control corresponding to a DOM
 * node in O(1) time.
 * @type {Object}
 * @private
 */
bluemind.ui.CartoucheBoxItem.prototype.childMap_ = null;

/**
 * Set if the item can be edited.
 * @param {boolean} readonly Read only or not.
 */
bluemind.ui.CartoucheBoxItem.prototype.setReadOnly = function(readonly) {
  if (this.isTransitionAllowed(goog.ui.Component.State.READONLY, readonly)) {
    this.setState(goog.ui.Component.State.READONLY, readonly);
    this.setSupportedState(goog.ui.Component.State.SELECTED, !readonly);
    this.getChild('close').setVisible(!readonly);
    this.getChild('close').setEnabled(!readonly);
  }  
};

/**
 * Returns true if the component is read only, false otherwise.
 * @return {boolean} Whether the component is read only.
 */
bluemind.ui.CartoucheBoxItem.prototype.isReadOnly = function() {
  return this.hasState(goog.ui.Component.State.READONLY);
};

/**
 * Returns the value associated with the cartouche item.  The default implementation
 * returns the model object associated with the item (if any), or its caption.
 * @return {*} Value associated with the cartouche item, if any, or its caption.
 */
bluemind.ui.CartoucheBoxItem.prototype.getValue = function() {
  var model = this.getModel();
  return model != null ? model : this.getCaption();
};


/**
 * Sets the value associated with the cartouche item.  The default implementation
 * stores the value as the model of the cartouche item.
 * @param {*} value Value to be associated with the cartouche item.
 */
bluemind.ui.CartoucheBoxItem.prototype.setValue = function(value) {
  this.setModel(value);
};

/** @override */
bluemind.ui.CartoucheBoxItem.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  if (this.isInDocument()) {
    this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
  }
};


/** @override */
bluemind.ui.CartoucheBoxItem.prototype.setContent = function(content) {
  goog.base(this, 'setContent', content);
  if (this.isInDocument()) {
    this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
  }
};

/**
 * Sets the cartouche item to be selectable or not.  Set to true for list items
 * that represent selectable options.
 * @param {boolean} selectable Whether the list item is selectable.
 */
bluemind.ui.CartoucheBoxItem.prototype.setSelectable = function(selectable) {
  this.setSupportedState(goog.ui.Component.State.SELECTED, selectable);
};

/**
 * Returns the text caption of the component while ignoring accelerators.
 * @override
 */
bluemind.ui.CartoucheBoxItem.prototype.getCaption = function() {
  var content = this.getContent();
  if (goog.isArray(content)) {
    var caption = goog.array.map(content, function(node) {
      return goog.dom.getRawTextContent(node);
    }).join('');
    return goog.string.collapseBreakingSpaces(caption);
  }
  return goog.base(this, 'getCaption');
};

/** @override */
bluemind.ui.CartoucheBoxItem.prototype.addChildAt = function(control, index, opt_render) {
  goog.base(this, 'addChildAt', control, index, opt_render);
  if (control.isInDocument() && this.isInDocument()) {
    this.registerChildId_(control);
  }
};

/** @override */
bluemind.ui.CartoucheBoxItem.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.forEachChild(function(child) {
    if (child.isInDocument()) {
      this.registerChildId_(child);
    }
  }, this);  
  var handler = this.getHandler();
  handler.listen(this.getChild('close'), goog.ui.Component.EventType.ACTION, this.handleClose_);
};

/**
 * Creates a DOM ID for the child control and registers it to an internal
 * hash table to be able to find it fast by id.
 * @param {goog.ui.Component} child The child control. Its root element has
 *     to be created yet.
 * @private
 */
bluemind.ui.CartoucheBoxItem.prototype.registerChildId_ = function(child) {
  // Map the DOM ID of the control's root element to the control itself.
  var childElem = child.getElement();

  // If the control's root element doesn't have a DOM ID assign one.
  var id = childElem.id || (childElem.id = child.getId());

  // Lazily create the child element ID map on first use.
  if (!this.childMap_) {
    this.childMap_ = {};
  }
  this.childMap_[id] = child;
};

/**
 * Handles action on the close button.
 * @param {goog.events.Event} e Mouse event to handle.
 */
bluemind.ui.CartoucheBoxItem.prototype.handleClose_ = function(e) {
  if (this.getParent()) {
    this.getParent().removeChild(this, true);
  }
  this.dispose();  
};

/**
 * Returns the child control that owns the given DOM node, or null if no such
 * control is found.
 * @param {Node} node DOM node whose owner is to be returned.
 * @return {goog.ui.Control?} Control hosted in the container to which the node
 *     belongs (if found).
 * @protected
 */
bluemind.ui.CartoucheBoxItem.prototype.getOwnerControl = function(node) {
  // Ensure that this container actually has child controls before
  // looking up the owner.
  if (this.childMap_) {
    var elem = this.getElement();
    while (node && node !== elem) {
      var id = node.id;
      if (id in this.childMap_) {
        return this.childMap_[id];
      }
      node = node.parentNode;
    }
  }
  return null;
};


/**
 * Possible cartouche states.
 * @type {goog.ui.Component.State.<number>}
 */
goog.ui.Component.State.READONLY = /** @type {goog.ui.Component.State.<number>} */ (0x1000);
