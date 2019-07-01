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
goog.provide("net.bluemind.ui.form.LongTextField");

goog.require("goog.ui.Textarea");
goog.require("net.bluemind.ui.form.FormField");

/**
 * @constructor
 * @param {goog.ui.ControlContent} label
 * @param {goog.ui.ControlRenderer} opt_renderer
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {net.bluemind.ui.form.FormField}
 */
net.bluemind.ui.form.LongTextField = function(label, opt_renderer, opt_domHelper) {
  net.bluemind.ui.form.FormField.call(this, label, opt_renderer, opt_domHelper);
  this.addClassName(goog.getCssName('field-textarea'));
}
goog.inherits(net.bluemind.ui.form.LongTextField, net.bluemind.ui.form.FormField);

/** @override */
net.bluemind.ui.form.LongTextField.prototype.createField = function() {
  var field = new goog.ui.Textarea();
  field.setId('field');
  this.addChild(field);
  field.render(this.getElementByClass(goog.getCssName('field-base-field')));
};

/** @override */
net.bluemind.ui.form.LongTextField.prototype.getValue = function() {
  return this.getChild('field').getValue();
};

/** @override */
net.bluemind.ui.form.LongTextField.prototype.setValue = function(value) {
  return this.getChild('field').setValue(value || "");
};
