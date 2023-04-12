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
goog.provide("net.bluemind.ui.form.TextField");

goog.require("goog.ui.LabelInput");
goog.require("net.bluemind.ui.form.FormField");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} label
 * @param {goog.ui.ControlRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {net.bluemind.ui.form.FormField}
 */
net.bluemind.ui.form.TextField = function(label, opt_renderer, opt_domHelper) {
  net.bluemind.ui.form.FormField.call(this, label, opt_renderer, opt_domHelper);
  this.addClassName(goog.getCssName('field-text'));
  this.readOnly = false;
}
goog.inherits(net.bluemind.ui.form.TextField, net.bluemind.ui.form.FormField);

/** @override */
net.bluemind.ui.form.TextField.prototype.createField = function() {
  var field = new goog.ui.LabelInput(this.label);
  field.setId('field');
  this.addChild(field);
  field.render(this.getElementByClass(goog.getCssName('field-base-field')));
};

net.bluemind.ui.form.TextField.prototype.focus = function() {
  this.getChild('field').getElement().focus();
}

/** @override */
net.bluemind.ui.form.TextField.prototype.getValue = function() {
  return this.getChild('field').getValue();
};

/** @override */
net.bluemind.ui.form.TextField.prototype.setValue = function(value) {
  this.getChild('field').setValue(value || "");
};

net.bluemind.ui.form.TextField.prototype.setReadOnly = function(value) {
  if (value != this.readOnly) {
    this.readOnly = value;
    this.applyReadOnly_();
  }
};
net.bluemind.ui.form.TextField.prototype.applyReadOnly_ = function() {
  if (this.isInDocument()) {
    if (this.readOnly) {
      this.getChild('field').getElement().setAttribute('readonly', 'true');
    } else if (this.getChild('field').getElement().hasAttribute('readonly')) {
      this.getChild('field').getElement().removeAttribute('readonly');
    }
  }
};
/** @override */
net.bluemind.ui.form.TextField.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.applyReadOnly_();
};


