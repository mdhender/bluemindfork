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
goog.provide("net.bluemind.ui.form.PhotoField");

goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.ui.form.FormField");
goog.require("net.bluemind.ui.form.templates");
/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} label
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {net.bluemind.ui.form.FormField}
 */
net.bluemind.ui.form.PhotoField = function(label, opt_domHelper) {
  net.bluemind.ui.form.FormField.call(this, label, opt_domHelper);
  this.addClassName(goog.getCssName('field-photo'));
}
goog.inherits(net.bluemind.ui.form.PhotoField, net.bluemind.ui.form.FormField);

/** @override */
net.bluemind.ui.form.PhotoField.prototype.createField = function() {
  var field = new goog.ui.Control();
  field.setSupportedState(goog.ui.Component.State.FOCUSED, false);
  field.setId('field');
  this.addChild(field, true);
  this.getHandler().listen(field, goog.ui.Component.EventType.ACTION, function(e) {
    e.preventDefault;
    this.dispatchEvent('add-photo');
  })
};

/** @override */
net.bluemind.ui.form.PhotoField.prototype.createPhotoField = function() {
	this.getChild('field').getElement().innerHTML = net.bluemind.ui.form.templates.photo();
};

/** @override */
net.bluemind.ui.form.PhotoField.prototype.createIconField = function() {
	this.getChild('field').getElement().innerHTML = '<i class="fa fa-5x fa-user"></i>';
};

/** @override */
net.bluemind.ui.form.PhotoField.prototype.createFormField = function() {
  this.createField();
};

/** @override */
net.bluemind.ui.form.PhotoField.prototype.getValue = function() {
  return this.getModel();
};

/** @override */
net.bluemind.ui.form.PhotoField.prototype.setValue = function(value) {
  this.setModel(value);
  if (value){
	  this.createPhotoField();
	  var img = goog.dom.getChildren(this.getChild('field').getElement())[0];
	  img.src = value;
  } else {
	  this.createIconField();
  }
};
