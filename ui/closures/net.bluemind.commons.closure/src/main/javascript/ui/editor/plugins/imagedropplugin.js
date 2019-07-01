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
 * @fileoverview A dialog for editing/creating a link.
 *
 */

goog.provide('bluemind.ui.editor.plugins.ImageDropPlugin');

goog.require('bluemind.ui.editor.Image');
goog.require('bluemind.ui.editor.IConfigurablePlugin');
goog.require('goog.editor.Plugin');
goog.require('goog.array');
goog.require('goog.events.FileDropHandler');
goog.require('goog.events.FileDropHandler.EventType');
goog.require('goog.string');

/**
 * Plugin to add a keyboard shortcut for the link command
 * @implements {bluemind.ui.editor.IConfigurablePlugin}
 * @constructor
 * @extends {goog.editor.Plugin}
 */
bluemind.ui.editor.plugins.ImageDropPlugin = function() {
  goog.base(this);

  this.eventHandler_ = new goog.events.EventHandler(this);  
};
goog.inherits(bluemind.ui.editor.plugins.ImageDropPlugin, goog.editor.Plugin);

/**
 * Event handler for this object.
 * @type {goog.events.EventHandler}
 * @private
 */
bluemind.ui.editor.plugins.ImageDropPlugin.prototype.eventHandler_;

/**
 * Handler for drop image file event
 *
 * @type {goog.events.FileDropHandler}
 */
bluemind.ui.editor.plugins.ImageDropPlugin.prototype.fileHandler_;


/**
 * @type {*} Plugin's option
 * private
 */
bluemind.ui.editor.plugins.ImageDropPlugin.prototype.options_;

/** @override */
bluemind.ui.editor.plugins.ImageDropPlugin.prototype.getOptions = function() {
  return this.options_;
};

/** @override */
bluemind.ui.editor.plugins.ImageDropPlugin.prototype.setOptions = function(options) {
  this.options_ = options;
};


/** @override */
bluemind.ui.editor.plugins.ImageDropPlugin.prototype.getTrogClassId = function() {
  return 'ImageDropPlugin';
};


/** @override */
bluemind.ui.editor.plugins.ImageDropPlugin.prototype.disable = function(fieldObject) {
  goog.base(this, 'disable', fieldObject);
  if (this.fileHandler_) {
    this.fileHandler_.dispose();
    this.fileHandler_ = null;
  }
  this.eventHandler_.removeAll();
};

/** @override */
bluemind.ui.editor.plugins.ImageDropPlugin.prototype.enable = function(fieldObject) {
  goog.base(this, 'enable', fieldObject);
  this.fileHandler_ = new goog.events.FileDropHandler(fieldObject.getElement(), true);
  this.eventHandler_.listen(this.fileHandler_, goog.events.EventType.DROP, this.handleDrop_);
};

/**
 * Handles image file drop. 
 * @param {!goog.events.BrowserEvent} e The browser event.
 */
bluemind.ui.editor.plugins.ImageDropPlugin.prototype.handleDrop_ = function(e) {
  // Notify listeners that the editable field's contents are about to change.
  this.getFieldObject().dispatchBeforeChange();
  var files = e.getBrowserEvent().dataTransfer.files;
  this.getFieldObject().focus();
  goog.array.forEach(files, function(file) {
    if (goog.string.startsWith(file.type, 'image')) {
      var img = new bluemind.ui.editor.Image(this.getFieldDomHelper());
      img.loading(true);
      goog.fs.FileReader.readAsDataUrl(file).addCallback(function(url) {
        img.setSrc(url);
      }, this);
      var range = this.getFieldObject().getRange();
      if (range && !range.isCollapsed()) {
        range.removeContents();
      }
      range.replaceContentsWithNode(img.getImage());
      img.placeCursorRightOf();

    }
  }, this);
  // Place cursor to the right of the modified image.

  this.getFieldObject().focus();
  this.getFieldObject().dispatchSelectionChangeEvent();
  this.getFieldObject().dispatchChange();
};


/** @override */
bluemind.ui.editor.plugins.ImageDropPlugin.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.eventHandler_.dispose();
  if (this.fileHandler_ && !this.fileHandler_.isDisposed()) {
    this.fileHandler_.dispose();
    this.fileHandler_ = null;
  }
};
