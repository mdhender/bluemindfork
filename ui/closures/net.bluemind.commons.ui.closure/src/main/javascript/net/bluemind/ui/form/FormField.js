/*
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
goog.provide("net.bluemind.ui.form.FormField");

goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.ui.Control");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.ui.form.templates");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} label
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {goog.ui.Control}
 */
net.bluemind.ui.form.FormField = function(label, opt_domHelper) {
  var renderer = goog.ui.ControlRenderer.getCustomRenderer(goog.ui.ControlRenderer, goog.getCssName('field-base'));
  goog.base(this, '', renderer, opt_domHelper);
  this.setHandleMouseEvents(false);
  this.setSupportedState(goog.ui.Component.State.FOCUSED, false);
  this.label = label;
}
goog.inherits(net.bluemind.ui.form.FormField, goog.ui.Control);

/**
 * Field label
 * 
 * @type {goog.ui.ControlContent} label
 * @protected
 */
net.bluemind.ui.form.FormField.prototype.label;

/** @override */
net.bluemind.ui.form.FormField.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.createFormField();
};

/** @override */
net.bluemind.ui.form.FormField.prototype.canDecorate = function(element) {
  return false;
};

/**
 * Create form field
 * 
 * @protected
 */
net.bluemind.ui.form.FormField.prototype.createFormField = function() {
  this.getElement().innerHTML = net.bluemind.ui.form.templates.field();
  this.createLabel();
  this.createField();
};

/**
 * Set form field label
 * 
 * @protected
 */
net.bluemind.ui.form.FormField.prototype.createLabel = function() {
  var el = this.getElementByClass(goog.getCssName('field-base-label'));
  if (el && this.label) {
    if (goog.isString(this.label)) {
      this.getDomHelper().setTextContent(el, this.label);
    } else {
      var childHandler = function(child) {
        if (child) {
          var doc = goog.dom.getOwnerDocument(el);
          el.appendChild(goog.isString(child) ? doc.createTextNode(child) : child);
        }
      };
      if (goog.isArray(this.label)) {
        goog.array.forEach(this.label, childHandler);
      } else if (goog.isArrayLike(this.label) && !('nodeType' in this.label)) {
        goog.array.forEach(goog.array.clone((this.label)), childHandler);
      } else {
        childHandler(this.label);
      }
    }
  }
};

/**
 * Set form field field
 * 
 */
net.bluemind.ui.form.FormField.prototype.createField = goog.abstractMethod;

/**
 * Set field value
 * 
 */
net.bluemind.ui.form.FormField.prototype.setValue = goog.abstractMethod;

/**
 * Get field value
 * 
 * @return {*} Field value
 */
net.bluemind.ui.form.FormField.prototype.getValue = goog.abstractMethod;
