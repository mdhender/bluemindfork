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

goog.provide("net.bluemind.contact.group.edit.ui.MemberField");
goog.provide("net.bluemind.contact.group.edit.ui.MemberField.EventType");
goog.provide("net.bluemind.contact.group.edit.ui.MemberField.MemberRenderer");

goog.require("goog.events");
goog.require("goog.string");
goog.require("goog.style");
goog.require("goog.ui.Button");
goog.require("goog.ui.Container");
goog.require("goog.ui.Control");
goog.require("goog.ui.Dialog");
goog.require("goog.ui.FlatButtonRenderer");
goog.require("goog.ui.LabelInput");
goog.require("goog.ui.Component.State");
goog.require("goog.ui.Dialog.DefaultButtonKeys");
goog.require("goog.ui.Dialog.EventType");
goog.require("goog.ui.ac.AutoComplete");
goog.require("goog.ui.ac.InputHandler");
goog.require("goog.ui.ac.Renderer");
goog.require("goog.ui.ac.AutoComplete.EventType");
goog.require("goog.ui.ac.Renderer.CustomRenderer");
goog.require("net.bluemind.contact.group.edit.templates");// FIXME - unresolved
// required symbol
goog.require("net.bluemind.contact.group.ui.MemberDetails");
goog.require("net.bluemind.mvp.UID");
goog.require("net.bluemind.ui.form.FormField");
goog.require("net.bluemind.ui.form.TextField");
goog.require("bluemind.ui.style.TrashButtonRenderer");

/**
 * A input control, rendered as a native browser input by default.
 *
 * @param {goog.ui.ControlContent} label
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @constructor
 * @extends {net.bluemind.ui.form.FormField}
 */
net.bluemind.contact.group.edit.ui.MemberField = function(label, opt_domHelper) {
  goog.base(this, label, opt_domHelper);
  var dialog = new goog.ui.Dialog();
  /** @meaning contact.contact.createNew */
  var MSG_CREATE_CONTACT = goog.getMsg('Create a new contact');
  dialog.setTitle(MSG_CREATE_CONTACT);
  dialog.setId('dialog');

  /** @meaning contact.vcard.fullname */
  var MSG_FULLNAME = goog.getMsg('Full name');
  var child = new net.bluemind.ui.form.TextField(MSG_FULLNAME);
  child.setId('fullname');
  dialog.addChild(child, true);

  /** @meaning contact.vcard.email */
  var MSG_EMAIL = goog.getMsg('E-mail');
  child = new net.bluemind.ui.form.TextField(MSG_EMAIL);
  child.setId('email');
  dialog.addChild(child, true);

  this.addChild(dialog);
  dialog.render();

  this.addClassName(goog.getCssName('field-members'))

};
goog.inherits(net.bluemind.contact.group.edit.ui.MemberField, net.bluemind.ui.form.FormField);

/**
 * Autocomplete mechanism
 *
 * @type {goog.ui.ac.AutoComplete}
 * @private
 */
net.bluemind.contact.group.edit.ui.MemberField.prototype.autocomplete_;

/**
 * Sets the data source providing the autocomplete suggestions.
 *
 * See constructor documentation for the interface.
 *
 * @param {!Object} matcher The matcher.
 */
net.bluemind.contact.group.edit.ui.MemberField.prototype.setMatcher = function(matcher) {
  var renderer = new goog.ui.ac.Renderer(null, new net.bluemind.contact.group.edit.ui.MemberField.MemberRenderer());
  var inputHandler = new goog.ui.ac.InputHandler(null, null, false, 250);
  inputHandler.setUpdateDuringTyping(false);
  this.autocomplete_ = new goog.ui.ac.AutoComplete(matcher, renderer, inputHandler);
  this.autocomplete_.setMaxMatches(10);
  this.registerDisposable(this.autocomplete_);
  inputHandler.attachAutoComplete(this.autocomplete_);
  if (this.getChild('ac')) {
    this.autocomplete_.attachInputs(this.getChild('ac').getElement());
  }

};

/** @override */
net.bluemind.contact.group.edit.ui.MemberField.prototype.createFormField = function() {
  var field = new goog.ui.LabelInput(this.label);
  field.setId('ac');
  this.addChild(field, true);
  if (this.autocomplete_) {
    this.autocomplete_.attachInputs(field.getElement());
  }

  var list = new goog.ui.Container();
  list.setFocusable(false);
  list.setId('list');
  this.addChild(list, true);
};

/** @override */
net.bluemind.contact.group.edit.ui.MemberField.prototype.createMemberField = function(model, check) {
  var list = this.getChild('list');

  var line = new goog.ui.Control();
  
  if (check){
    var e = new goog.events.Event(net.bluemind.contact.group.edit.ui.MemberField.EventType.AC_ADD);
    e.model = model;
    this.dispatchEvent(e);
  }

  var member = new net.bluemind.contact.group.ui.MemberDetails(model);
  member.setId('member');
  line.addChild(member, true);

  var button = new goog.ui.Button(null, bluemind.ui.style.TrashButtonRenderer.getInstance());
  button.setId('remove');
  /** @meaning contact.member.remove */
  var MSG_REMOVE_MEMBER = goog.getMsg('Remove member');
  button.setTooltip(MSG_REMOVE_MEMBER);
  line.addChild(button, true);

  this.getHandler().listen(line.getChild('remove'), goog.ui.Component.EventType.ACTION, function(e) {
    list.removeChild(line, true);
    line.dispose();
  });

  list.addChild(line, true);



};

/** @override */
net.bluemind.contact.group.edit.ui.MemberField.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler()
      .listen(this.autocomplete_, goog.ui.ac.AutoComplete.EventType.UPDATE, this.handleAutoCompleteAction_);
  this.getHandler().listen(this.getChild('dialog'), goog.ui.Dialog.EventType.SELECT, this.handleDialogSave_);

};

/**
 *
 * @param {goog.event.Event} event
 */
net.bluemind.contact.group.edit.ui.MemberField.prototype.handleDialogSave_ = function(event) {
  if (event.key == goog.ui.Dialog.DefaultButtonKeys.OK) {
    var dialog = this.getChild('dialog');
    var model = {};
    model.id = net.bluemind.mvp.UID.generate();
    model.name = dialog.getChild('fullname').getValue();
    model.email = dialog.getChild('email').getValue();
    model.container = dialog.getModel().container;
    var e = new goog.events.Event(net.bluemind.contact.group.edit.ui.MemberField.EventType.CREATE);
    e.model = model;
    this.dispatchEvent(e);
    this.createMemberField(model);
    this.sortList();
  }
};

/**
 *
 */
net.bluemind.contact.group.edit.ui.MemberField.prototype.handleAutoCompleteAction_ = function(e) {
  this.getChild('ac').setValue('');
  var row = e.row;
  if (row.id != null) {
    this.createMemberField(row, true);
    this.sortList();
  } else {
    var dialog = this.getChild('dialog');
    var token = row.name;
    if (token.match(/.*@.*/)) {
      dialog.getChild('email').setValue(token);
      token = token.split('@').shift();
    } else {
      dialog.getChild('email').setValue('');
    }
    token = goog.string.toTitleCase(token.replace('.', ' '));
    dialog.getChild('fullname').setValue(token);
    dialog.setModel(row);
    dialog.setVisible(true);
  }
};

/** @override */
net.bluemind.contact.group.edit.ui.MemberField.prototype.getValue = function() {
  var list = this.getChild('list');
  var value = [];
  list.forEachChild(function(child) {
    value.push(child.getChild('member').getModel());
  }, this);
  return value;
};

/** @override */
net.bluemind.contact.group.edit.ui.MemberField.prototype.setValue = function(value) {
  value = this.sortMembers(value);
  this.resetValue_();
  if (goog.isArray(value)) {
    goog.array.forEach(value, function(member) {
      this.createMemberField(member)
    }, this);
  }
};

/**
 * @private
 */
net.bluemind.contact.group.edit.ui.MemberField.prototype.resetValue_ = function() {
  var list = this.getChild('list');
  this.getChild('ac').setValue('');
  var childs = list.removeChildren(true);
  goog.array.forEach(childs, function(child) {
    child.dispose();
  })
};


/**
 * @private
 */
net.bluemind.contact.group.edit.ui.MemberField.prototype.sortMembers = function(members) {
    members.sort(function(member1, member2) {
        return member1.name.localeCompare(member2.name);
    });
    return members;
};

/**
 * @private
 */
net.bluemind.contact.group.edit.ui.MemberField.prototype.sortList = function() {
    members = this.sortMembers(this.getValue());
  this.resetValue_();
  if (goog.isArray(members)) {
    goog.array.forEach(members, function(member) {
      this.createMemberField(member)
    }, this);
  }
};

/**
 * @constructor
 *
 * @extends {goog.ui.ac.Renderer.CustomRenderer}
 */
net.bluemind.contact.group.edit.ui.MemberField.MemberRenderer = function() {
}
goog.inherits(net.bluemind.contact.group.edit.ui.MemberField.MemberRenderer, goog.ui.ac.Renderer.CustomRenderer);

/** @override */
net.bluemind.contact.group.edit.ui.MemberField.MemberRenderer.prototype.render = null;

/** @override */
net.bluemind.contact.group.edit.ui.MemberField.MemberRenderer.prototype.renderRow = function(row, token, node) {
  if (goog.isDefAndNotNull(row.data.id)) {
    node.innerHTML = net.bluemind.contact.group.edit.templates.ac({
      entry : row.data
    });
  } else {
    node.innerHTML = net.bluemind.contact.group.edit.templates.create(row.data);
    if (row.id != 1) {
      goog.style.setStyle(node, 'border-top', '1px solid #333');
    }
  }

};
/**
 * @enum
 *
 */
net.bluemind.contact.group.edit.ui.MemberField.EventType = {
  CREATE : goog.events.getUniqueId('create'),
  AC_ADD : goog.events.getUniqueId('ac-add')
};
