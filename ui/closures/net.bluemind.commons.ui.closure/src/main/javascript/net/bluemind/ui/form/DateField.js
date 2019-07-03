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
goog.provide("net.bluemind.ui.form.DateField");

goog.require("goog.ui.InputDatePicker");
goog.require("net.bluemind.date.Date");// FIXME - unresolved required symbol
goog.require("net.bluemind.ui.form.FormField");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} label
 * @param {goog.ui.ControlRenderer} opt_renderer
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {net.bluemind.ui.form.FormField}
 */
net.bluemind.ui.form.DateField = function(label, formatter, parser, opt_renderer, opt_domHelper) {
  net.bluemind.ui.form.FormField.call(this, label, opt_renderer, opt_domHelper);
  this.addClassName(goog.getCssName('field-date'));
  this.formatter = formatter;
  this.parser = parser;
}
goog.inherits(net.bluemind.ui.form.DateField, net.bluemind.ui.form.FormField);

/**
 * @type {goog.i18n.DateTimeFormat}
 */
net.bluemind.ui.form.DateField.prototype.formatter;
/**
 * @type {goog.i18n.DateTimeParse}
 */
net.bluemind.ui.form.DateField.prototype.parser;

/** @override */
net.bluemind.ui.form.DateField.prototype.createField = function() {
  var field = new goog.ui.InputDatePicker(this.formatter, this.parser);
  field.setId('field');
  this.addChild(field);
  field.render(this.getElementByClass(goog.getCssName('field-base-field')));
};

/** @override */
net.bluemind.ui.form.DateField.prototype.getValue = function() {
  var date = this.getChild('field').getDate();
  if (date != null) {
    date = new net.bluemind.date.Date(date);
  }
  return date;
};

/** @override */
net.bluemind.ui.form.DateField.prototype.setValue = function(value) {
  return this.getChild('field').setDate(value);
};
