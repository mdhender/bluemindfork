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
 * @fileoverview Plugin to switch to a Wysiwyt editor.
 *
 */

goog.provide('bluemind.ui.editor.plugins.EditHTMLPlugin');

goog.require('bluemind.ui.editor.IConfigurablePlugin');
goog.require('goog.async.Delay');
goog.require('goog.dom');
goog.require('goog.dom.TagName');
goog.require('goog.editor.Field.EventType');
goog.require('goog.editor.Plugin');
goog.require('goog.editor.Command');
goog.require('goog.events.EventHandler');
goog.require('goog.events.InputHandler');
goog.require('goog.events.InputHandler.EventType');
goog.require('goog.style');

/**
 * Plugin to switch to a Wysiwyt editor. This can be used to edit html content
 * or to switch as a plain text editor.
 * @implements {bluemind.ui.editor.IConfigurablePlugin}
 * @constructor
 * @extends {goog.editor.Plugin}
 */
bluemind.ui.editor.plugins.EditHTMLPlugin = function() {
  goog.base(this);
  this.eventHandler_ = new goog.events.EventHandler(this);  
  this.delayedChange_ = new goog.async.Delay(this.updateFieldContents_, 250, this);
};
goog.inherits(bluemind.ui.editor.plugins.EditHTMLPlugin, goog.editor.Plugin);


/**
 * Event handler for this object.
 * @type {goog.events.EventHandler}
 * @private
 */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.eventHandler_;

/**
 * Handler textearea events
 *
 * @type {goog.events.InputHandler}
 */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.textHandler_;

/**
 * textearea element
 *
 * @type {Element}
 */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.textarea_;

/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.getTrogClassId = function() {
  return 'EditHTMLPlugin';
};

/**
 * @type {*} Plugin's option
 * private
 */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.options_;

/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.getOptions = function() {
  return this.options_;
};


/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.setOptions = function(options) {
  this.options_ = options;
};

/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.execCommandInternal = function() { 
  if (goog.style.isElementShown(this.textarea_)) {
    this.updateFieldContents_();
    goog.style.setElementShown(this.textarea_, false);
    goog.style.setElementShown(this.field_, true);
  } else {
    this.updateTextSize_();
    this.updateTextContents_();
    goog.style.setElementShown(this.field_, false);
    goog.style.setElementShown(this.textarea_, true);
  }
  this.getFieldObject().dispatchCommandValueChange([goog.editor.Command.EDIT_HTML]);
};

/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.disable = function(fieldObject) {
  goog.base(this, 'disable', fieldObject);
  this.textHandler_.dispose();
  this.textHandler_ = null;  
  this.eventHandler_.removeAll();
  this.delayedChange_.stop();

};


/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.registerFieldObject = function(fieldObject) {
  goog.base(this, 'registerFieldObject', fieldObject);
  var dom = goog.dom.getDomHelper(fieldObject.getOriginalElement());
  this.field_ = dom.getElement(fieldObject.getOriginalElement().id);
  this.textarea_ = dom.createDom(goog.dom.TagName.TEXTAREA, {style: this.field_.style.cssText});
  this.textarea_.className = this.field_.className;
  this.textarea_.style.boxSizing = 'border-box';
  goog.style.setElementShown(this.textarea_, false);
  dom.insertSiblingBefore(this.textarea_, this.field_);
};

/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.enable = function(fieldObject) {
  goog.base(this, 'enable', fieldObject);
  this.textHandler_ = new goog.events.InputHandler(this.textarea_);
  this.eventHandler_.listen(this.textHandler_, goog.events.InputHandler.EventType.INPUT, this.updateFieldContents_);
  this.eventHandler_.listen(fieldObject, goog.editor.Field.EventType.DELAYEDCHANGE, this.updateTextContents_);
};


bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.delayFieldChange_ = function() {
  if (goog.style.isElementShown(this.textarea_)) {
    this.delayedChange_.start();
  }
};

bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.updateFieldContents_ = function() {
  this.getFieldObject().setHtml(false, this.textarea_.value, false);
};

bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.updateTextContents_ = function() {
  if (goog.style.isElementShown(this.field_)) {
    this.textarea_.value = this.getFieldObject().getCleanContents();
  }
};

bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.updateTextSize_ = function() {
  var size = goog.style.getSize(this.field_);
  if (size.width > 0 && size.height > 0) {
    goog.style.setSize(this.textarea_, goog.style.getSize(this.field_));
  }
};

/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.eventHandler_.dispose();
  this.delayedChange_.stop();
  this.delayedChange_.dispose();
  if (this.textHandler_ && !this.textHandler_.isDisposed()) {
    this.textHandler_.dispose();
    this.textHandler_ = null;
  }
};

/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.isSupportedCommand =
    function(command) {
  return command == goog.editor.Command.EDIT_HTML;
};

/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.queryCommandValue = function(command) {
  var state = null;
  if (command == goog.editor.Command.EDIT_HTML) {
    state = goog.style.isElementShown(this.textarea_);
  }
  return state;
};

/** @override */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.activeOnUneditableFields = function() {
  return true;
};

/**
 * Sets the contents of the field.
 * @param {?string} html html to insert.  If html=null, then this defaults
 *    to a nsbp for mozilla and an empty string for IE.
 */
bluemind.ui.editor.plugins.EditHTMLPlugin.prototype.setValue = function(html) {
  if (goog.style.isElementShown(this.field_)) {
    this.getFieldObject().setHtml(false, html);
  } else {
    this.textarea_.value = html;
  this.getFieldObject().setHtml(false, html, false);
  }
};


