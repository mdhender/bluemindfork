/* BEGIN LICENSE
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
goog.provide("net.bluemind.ui.form.RichTextField");

goog.require("net.bluemind.ui.form.FormField");
goog.require("bluemind.ui.RichText");// FIXME - unresolved required symbol

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} label
 * @param {goog.ui.ControlRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {net.bluemind.ui.form.FormField}
 */
net.bluemind.ui.form.RichTextField = function(label, opt_renderer, opt_domHelper) {
  goog.base(this, label, opt_renderer, opt_domHelper);
  this.addClassName(goog.getCssName('field-richtext'));
}
goog.inherits(net.bluemind.ui.form.RichTextField, net.bluemind.ui.form.FormField);

/** @override */
net.bluemind.ui.form.RichTextField.prototype.createField = function() {
  var field = new bluemind.ui.RichText();
  field.setId('field');
  this.addChild(field);
  field.render(this.getElementByClass(goog.getCssName('field-base-field')));
};

/** @override */
net.bluemind.ui.form.RichTextField.prototype.getValue = function() {
  return this.getChild('field').getValue();
};

/** @override */
net.bluemind.ui.form.RichTextField.prototype.setValue = function(value) {
  return this.getChild('field').setValue(value);
};
