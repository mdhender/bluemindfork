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
 * @fileoverview Image bubble plugin.
 *
 */


goog.provide('bluemind.ui.editor.plugins.ImageBubblePlugin');

goog.require('bluemind.ui.editor.IConfigurablePlugin');
goog.require('goog.dom');
goog.require('goog.dom.TagName');
goog.require('goog.editor.Command');
goog.require('goog.editor.plugins.AbstractBubblePlugin');
goog.require('goog.string.Unicode');
goog.require('goog.ui.editor.Bubble');



/**
 * Property bubble plugin for images.
 *
 * @implements {bluemind.ui.editor.IConfigurablePlugin}
 * @constructor
 * @extends {goog.editor.plugins.AbstractBubblePlugin}
 */
bluemind.ui.editor.plugins.ImageBubblePlugin = function() {
  goog.base(this);
};
goog.inherits(bluemind.ui.editor.plugins.ImageBubblePlugin,
    goog.editor.plugins.AbstractBubblePlugin);


/**
 * Id for 'edit' link.
 * @type {string}
 * @private
 */
bluemind.ui.editor.plugins.ImageBubblePlugin.EDIT_ID_ = 'image-bubble-edit';


/**
 * Id for 'remove' link.
 * @type {string}
 * @private
 */
bluemind.ui.editor.plugins.ImageBubblePlugin.REMOVE_ID_ = 'image-bubble-remove';


/**
 * @type {*} Plugin's option
 * private
 */
bluemind.ui.editor.plugins.ImageBubblePlugin.prototype.options_;

/** @override */
bluemind.ui.editor.plugins.ImageBubblePlugin.prototype.getOptions = function() {
  return this.options_;
};

/** @override */
bluemind.ui.editor.plugins.ImageBubblePlugin.prototype.setOptions = function(options) {
  this.options_ = options;
};

/** @override */
bluemind.ui.editor.plugins.ImageBubblePlugin.prototype.
    getBubbleTargetFromSelection = function(selectedElement) {
  var bubbleTarget = goog.dom.getAncestorByTagNameAndClass(selectedElement,
      goog.dom.TagName.IMG);
  return bubbleTarget;
};


/** @override */
bluemind.ui.editor.plugins.ImageBubblePlugin.prototype.createBubbleContents =
    function(bubbleContainer) {
  goog.dom.appendChild(bubbleContainer,
      bubbleContainer.ownerDocument.createTextNode(
      bluemind.ui.editor.messages.MSG_BUBBLE_IMAGE + goog.string.Unicode.NBSP));

  this.createLink(bluemind.ui.editor.plugins.ImageBubblePlugin.EDIT_ID_,
      bluemind.ui.editor.messages.MSG_BUBBLE_CHANGE, this.editImage_, bubbleContainer);

  goog.dom.appendChild(bubbleContainer,
      bubbleContainer.ownerDocument.createTextNode(
      goog.string.Unicode.NBSP));

  this.createLink(bluemind.ui.editor.plugins.ImageBubblePlugin.REMOVE_ID_,
      bluemind.ui.editor.messages.MSG_BUBBLE_REMOVE, this.removeImage_, bubbleContainer);
};


/** @override */
bluemind.ui.editor.plugins.ImageBubblePlugin.prototype.getBubbleType =
    function() {
  return goog.dom.TagName.IMG;
};


/** @override */
bluemind.ui.editor.plugins.ImageBubblePlugin.prototype.getBubbleTitle =
    function() {
  return bluemind.ui.editor.messages.MSG_BUBBLE_IMAGE;
};


/**
 * Removes the image associated with the bubble.
 * @private
 */
bluemind.ui.editor.plugins.ImageBubblePlugin.prototype.removeImage_ =
    function() {
  this.getFieldObject().dispatchBeforeChange();

  goog.dom.removeNode(this.getTargetElement());

  this.closeBubble();

  this.getFieldObject().dispatchChange();
};


/**
 * Opens image editor for the image associated with the bubble.
 * @private
 */
bluemind.ui.editor.plugins.ImageBubblePlugin.prototype.editImage_ =
    function() {
  var imageNode = this.getTargetElement();
  this.closeBubble();
  this.getFieldObject().execCommand(goog.editor.Command.IMAGE, imageNode);
};
