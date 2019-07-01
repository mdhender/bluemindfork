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
 * @fileoverview A plugin for the ImageDialog.
 *
 */

goog.provide('bluemind.ui.editor.plugins.ImageDialogPlugin');

goog.require('bluemind.ui.editor.ImageDialog');
goog.require('bluemind.ui.editor.IConfigurablePlugin');
goog.require('goog.array');
goog.require('goog.dom.TagName');
goog.require('goog.editor.Command');
goog.require('goog.editor.plugins.AbstractDialogPlugin');
goog.require('goog.events.EventHandler');
goog.require('goog.functions');
goog.require('goog.ui.editor.AbstractDialog.EventType');
goog.require('goog.uri.utils');



/**
 * A plugin that opens the image dialog.
 * @constructor
 * @implements {bluemind.ui.editor.IConfigurablePlugin}
 * @extends {goog.editor.plugins.AbstractDialogPlugin}
 */
bluemind.ui.editor.plugins.ImageDialogPlugin = function() {
  goog.base(this, goog.editor.Command.IMAGE);
  this.eventHandler_ = new goog.events.EventHandler(this);
  this.options_ = {};


};
goog.inherits(bluemind.ui.editor.plugins.ImageDialogPlugin,
    goog.editor.plugins.AbstractDialogPlugin);

/**
 * Event handler for this object.
 * @type {goog.events.EventHandler}
 * @private
 */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.eventHandler_;

/**
 * Image object that the dialog is editing.
 * @type {bluemind.ui.editor.Image}
 * @protected
 */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.image_;


/**
 * @type {*} Plugin's option
 * private
 */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.options_;

/** @override */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.getOptions = function() {
  return this.options_;
};

/** @override */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.setOptions = function(options) {
  this.options_ = options || {};
};

/** @override */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.getTrogClassId =
    goog.functions.constant('ImageDialogPlugin');



/**
 * Handles when the dialog closes.
 * @param {goog.events.Event} e The AFTER_HIDE event object.
 * @override
 * @protected
 */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.handleAfterHide = function(e) {
  goog.base(this, 'handleAfterHide', e);
  this.image_ = null;
};


/**
 * @return {goog.events.EventHandler} The event handler.
 * @protected
 */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.getEventHandler = function() {
  return this.eventHandler_;
};


/**
 * @return {bluemind.ui.editor.Image} The image being edited.
 * @protected
 */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.getCurrentImage = function() {
  return this.image_;
};


/**
 * Creates a new instance of the dialog and registers for the relevant events.
 * @param {goog.dom.DomHelper} dialogDomHelper The dom helper to be used to
 *     create the dialog.
 * @param {*=} opt_image The target image (should be a bluemind.ui.editor.Image).
 * @return {bluemind.ui.editor.ImageDialog} The dialog.
 * @override
 * @protected
 */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.createDialog = function(
    dialogDomHelper, opt_image) {
  this.image_ = new bluemind.ui.editor.Image(dialogDomHelper, /** @type {HTMLImageElement} */ (opt_image));
  var dialog = new bluemind.ui.editor.ImageDialog(dialogDomHelper, this.image_, this.options_['dataUrlRpc']);
  this.eventHandler_.
      listen(this.image_, goog.events.EventType.CHANGE,
          this.handleChange).
      listen(dialog, goog.ui.editor.AbstractDialog.EventType.CANCEL,
          this.handleCancel_);
  return dialog;
};


/** @override */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.eventHandler_.dispose();
};


/**
 * Handles the OK event from the dialog by updating the image in the field.
 * @param {goog.events.Event} e OK event object.
 * @protected
 */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.handleChange = function(e) {
  // First restore the selection so we can manipulate the editable field's
  // content according to what was selected.  
  this.restoreOriginalSelection();
  // Notify listeners that the editable field's contents are about to change.
  this.getFieldObject().dispatchBeforeChange();
  var img = e.target; 
  if (img.isNew()) {
    this.getFieldObject().focus();
    var range = this.getFieldObject().getRange();
    if (range && !range.isCollapsed()) {
      range.removeContents();
    }
    var image = range.replaceContentsWithNode(img.getImage());
    img.image_ = image;
  }

  // Place cursor to the right of the modified image.
  img.placeCursorRightOf();
  this.getFieldObject().focus();


  this.getFieldObject().dispatchSelectionChangeEvent();
  this.getFieldObject().dispatchChange();

  this.eventHandler_.removeAll();
};


/**
 * Handles the CANCEL event from the dialog by clearing the image if needed.
 * @param {goog.events.Event} e Event object.
 * @private
 */
bluemind.ui.editor.plugins.ImageDialogPlugin.prototype.handleCancel_ = function(e) {
  this.image_ = null;
  this.eventHandler_.removeAll();
};
