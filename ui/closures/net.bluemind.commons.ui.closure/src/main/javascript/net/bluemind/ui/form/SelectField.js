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
goog.provide("net.bluemind.ui.form.SelectField");

goog.require("goog.ui.MenuItem");
goog.require("goog.ui.Select");
goog.require("net.bluemind.ui.form.FormField");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} label
 * @param {Map} options
 * @param {goog.ui.ControlRenderer} opt_renderer
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {net.bluemind.ui.form.FormField}
 */
net.bluemind.ui.form.SelectField = function(label, options, opt_renderer, opt_domHelper) {
  net.bluemind.ui.form.FormField.call(this, label, opt_renderer, opt_domHelper);
  this.addClassName(goog.getCssName('field-select'));
  this.options_ = options;

}
goog.inherits(net.bluemind.ui.form.SelectField, net.bluemind.ui.form.FormField);
/**
 * @private
 * @type {*}
 */
net.bluemind.ui.form.SelectField.prototype.options_;

/** @override */
net.bluemind.ui.form.SelectField.prototype.createField = function() {
  var field = new goog.ui.Select(this.label);
  field.setId('field');
  this.addChild(field);
  field.addClassName(goog.getCssName('goog-button-base'));
  field.addClassName(goog.getCssName('goog-select'));
  for ( var value in this.options_) {
    field.addItem(new goog.ui.MenuItem(this.options_[value], value));
  }
  field.render(this.getElementByClass(goog.getCssName('field-base-field')));
};

/** @override */
net.bluemind.ui.form.SelectField.prototype.getValue = function() {
  return this.getChild('field').getValue();
};

/** @override */
net.bluemind.ui.form.SelectField.prototype.setValue = function(value) {
  return this.getChild('field').setValue(value);
};