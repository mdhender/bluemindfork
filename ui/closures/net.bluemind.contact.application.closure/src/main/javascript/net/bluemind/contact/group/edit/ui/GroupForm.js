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
goog.provide("net.bluemind.contact.group.edit.ui.GroupForm");

goog.require("goog.dom.classlist");
goog.require("net.bluemind.contact.group.edit.ui.MemberField");
goog.require("net.bluemind.ui.form.Form");
goog.require("net.bluemind.ui.form.RichTextField");
goog.require("net.bluemind.ui.form.TextField");
goog.require("net.bluemind.ui.form.TagField");

/**
 * Task Form ui.
 *
 * @param {Object} model Default model
 * @param {net.bluemind.i18n.DateHelper.Formatter} formatter Date formatter.
 * @param {net.bluemind.i18n.DateHelper.Parser} parser Date parser.
 *
 * @constructor
 * @extends {net.bluemind.ui.form.Form}
 */
net.bluemind.contact.group.edit.ui.GroupForm = function(formatter, parser) {
  goog.base(this, formatter, parser);

  /** @meaning contact.vcard.name */
  var MSG_NAME = goog.getMsg('Name');
  var child = new net.bluemind.ui.form.TextField(MSG_NAME);
  child.setId('name');
  this.addChild(child, true);

  /** @meaning contact.vcard.categories */
  var MSG_CATEGORIES = goog.getMsg('Categories');
  child = new net.bluemind.ui.form.TagField(MSG_CATEGORIES);
  child.setId('categories');
  this.addChild(child, true);
  
  /** @meaning contact.vcard.note */
  var MSG_NOTE = goog.getMsg('Note');
  child = new net.bluemind.ui.form.RichTextField(MSG_NOTE);
  child.setId('note');
  this.addChild(child, true);

  /** @meaning contact.group.addMembers */
  var MSG_MEMBERS = goog.getMsg('Add members...');
  child = new net.bluemind.contact.group.edit.ui.MemberField(MSG_MEMBERS);
  child.setId('members');
  this.addChild(child, true);
};
goog.inherits(net.bluemind.contact.group.edit.ui.GroupForm, net.bluemind.ui.form.Form);

/** @override */
net.bluemind.contact.group.edit.ui.GroupForm.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  this.getChild('name').setValue(model.name);
  this.getChild('categories').setValue(model.categories);
  this.getChild('note').setValue(model.note);
  this.getChild('members').setValue(model.members);

  if (model.errors) {
    var notif = this.getChild('notifications');
    goog.array.forEach(model.errors, function(e) {
      notif.addError( e.property, e.msg);
    }, this);
    this.getChild('notifications').show_();
  }

  this.getChild('name').focus();

};

/** @override */
net.bluemind.contact.group.edit.ui.GroupForm.prototype.createDom = function() {
  goog.base(this, 'createDom');
  goog.dom.classlist.add(this.getElement(), goog.getCssName('vcard'));
  goog.dom.classlist.add(this.getElement(), goog.getCssName('group'));

};

/**
 * Refresh model with data from the form
 *
 * @private
 */
net.bluemind.contact.group.edit.ui.GroupForm.prototype.getModel = function() {
  var model = goog.base(this, 'getModel') || {};
  model.name = this.getChild('name').getValue();
  model.categories = this.getChild('categories').getValue();
  model.note = this.getChild('note').getValue();
  model.members = this.getChild('members').getValue();
  return model;
};

net.bluemind.contact.group.edit.ui.GroupForm.prototype.setTags = function(tags) {
 this.getChild('categories').setTags(tags); 
}