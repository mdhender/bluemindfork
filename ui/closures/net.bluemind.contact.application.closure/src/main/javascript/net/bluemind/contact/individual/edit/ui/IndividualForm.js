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
goog.provide("net.bluemind.contact.individual.edit.ui.IndividualForm");

goog.require("goog.dom.classlist");
goog.require("goog.ui.Menu");
goog.require("goog.ui.MenuButton");
goog.require("goog.ui.MenuItem");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.style.app.MenuButtonRenderer");
goog.require("net.bluemind.addressbook.api.i18n.Address.Caption");
goog.require("net.bluemind.addressbook.api.i18n.Email.Caption");
goog.require("net.bluemind.addressbook.api.i18n.IMPP.Caption");
goog.require("net.bluemind.addressbook.api.i18n.Tel.Caption");
goog.require("net.bluemind.addressbook.api.i18n.URL.Caption");
goog.require("net.bluemind.contact.individual.edit.ui.FullNameField");
goog.require("net.bluemind.contact.vcard.edit.ui.AddressField");
goog.require("net.bluemind.contact.vcard.edit.ui.CoordinateField");
goog.require("net.bluemind.ui.form.DateField");
goog.require("net.bluemind.ui.form.Form");
goog.require("net.bluemind.ui.form.PhotoField");
goog.require("net.bluemind.ui.form.RichTextField");
goog.require("net.bluemind.ui.form.TextField");
goog.require("net.bluemind.ui.form.LongTextField");
goog.require("net.bluemind.api.BlueMindClient");
goog.require("net.bluemind.contact.vcard.edit.ui.templates");
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
net.bluemind.contact.individual.edit.ui.IndividualForm = function(ctx, systemUser) {
  goog.base(this, formatter, parser);

  this.ctx = ctx;
  if (systemUser == true) {
    this.systemUser = true;
  } else {
    this.systemUser = false;
  }
  var formatter = ctx.helper('dateformat').formatter;
  var parser = ctx.helper('dateformat').parser;

  var child = new goog.ui.Component();
  child.setId('header');
  this.addChild(child, true);
  goog.dom.classlist.add(this.getChild('header').getElement(), goog.getCssName('header'));

  child = new net.bluemind.ui.form.PhotoField();
  child.setId('photo');
  this.getChild('header').addChild(child, true);

  child = new net.bluemind.contact.individual.edit.ui.FullNameField();
  child.setId('fullname');
  this.getChild('header').addChild(child, true);
  this.getChild('header').getElement()
  /** @meaning contact.vcard.nickname */
  var MSG_NICKNAME = goog.getMsg('Nickname');
  child = new net.bluemind.ui.form.TextField(MSG_NICKNAME);
  child.setId('nickname');
  this.addChild(child, true);
  /** @meaning contact.vcard.company */
  var MSG_COMPANY = goog.getMsg('Company');
  child = new net.bluemind.ui.form.TextField(MSG_COMPANY);
  child.setId('company');
  this.getChild('header').addChild(child, true);
  /** @meaning contact.vcard.jobtitle */
  var MSG_JOB_TITLE = goog.getMsg('Job title')
  child = new net.bluemind.ui.form.TextField(MSG_JOB_TITLE);
  child.setId('job-title');
  this.getChild('header').addChild(child, true);
  /** @meaning contact.vcard.role */
  var MSG_ROLE = goog.getMsg('Role')
  child = new net.bluemind.ui.form.TextField(MSG_ROLE);
  child.setId('role');
  this.getChild('header').addChild(child, true);
  /** @meaning contact.vcard.division */
  var MSG_DIVISION = goog.getMsg('Division');
  child = new net.bluemind.ui.form.TextField(MSG_DIVISION);
  child.setId('division');
  this.addChild(child, true);
  child.setVisible(false);
  /** @meaning contact.vcard.department */
  var MSG_DEPARTMENT = goog.getMsg('Department');
  child = new net.bluemind.ui.form.TextField(MSG_DEPARTMENT);
  child.setId('department');
  this.getChild('header').addChild(child, true);

  child = new goog.ui.Component();
  child.setId('container');
  this.getChild('header').addChild(child, true);
  goog.dom.classlist.add(child.getElement(), goog.getCssName('container'));


  child = new net.bluemind.contact.vcard.edit.ui.CoordinateField(net.bluemind.addressbook.api.i18n.Email.MSG_WORK,
      net.bluemind.addressbook.api.i18n.Email.Caption, 'EMAIL');
  child.setId('emails');
  this.addChild(child, true);
  child = new net.bluemind.contact.vcard.edit.ui.CoordinateField(net.bluemind.addressbook.api.i18n.Tel.MSG_WORKVOICE, net.bluemind.addressbook.api.i18n.Tel.Caption, 'PHONE')
  child.setId('tels');
  this.addChild(child, true);
  /** @meaning contact.vcard.address */
  var MSG_ADDRESS = goog.getMsg('Address');
  child = new net.bluemind.contact.vcard.edit.ui.AddressField(MSG_ADDRESS,
      net.bluemind.addressbook.api.i18n.Address.Caption)
  child.setId('addresses');
  this.addChild(child, true);
  /** @meaning contact.vcard.website */
  var MSG_URL = goog.getMsg('Website');
  child = new net.bluemind.contact.vcard.edit.ui.CoordinateField(MSG_URL, net.bluemind.addressbook.api.i18n.URL.Caption, 'WEBSITE')
  child.setId('urls');
  this.addChild(child, true);
  child.setVisible(false);
  /** @meaning contact.vcard.im */
  var MSG_IMPP = goog.getMsg('Instant messaging');
  child = new net.bluemind.contact.vcard.edit.ui.CoordinateField(MSG_IMPP,
      net.bluemind.addressbook.api.i18n.IMPP.Caption)
  child.setId('impps');
  this.addChild(child, true);
  child.setVisible(false);
  /** @meaning contact.vcard.birthday */
  var MSG_BIRTHDAY = goog.getMsg('Birthday');
  child = new net.bluemind.ui.form.DateField(MSG_BIRTHDAY, formatter.date, parser.date);
  child.setId('birthday');
  this.addChild(child, true);
  /** @meaning contact.vcard.anniversay */
  var MSG_ANNIVERSARY = goog.getMsg('Anniversary')
  child = new net.bluemind.ui.form.DateField(MSG_ANNIVERSARY, formatter.date, parser.date);
  child.setId('anniversary');
  this.addChild(child, true);
  child.setVisible(false);
  /** @meaning contact.vcard.manager */
  var MSG_MANAGER = goog.getMsg('Manager')
  child = new net.bluemind.ui.form.TextField(MSG_MANAGER);
  child.setId('manager');
  this.addChild(child, true);
  child.setVisible(false);
  /** @meaning contact.vcard.assistant */
  var MSG_ASSISTANT = goog.getMsg('Assistant');
  child = new net.bluemind.ui.form.TextField(MSG_ASSISTANT);
  child.setId('assistant');
  this.addChild(child, true);
  child.setVisible(false);
  /** @meaning contact.vcard.spouse */
  var MSG_SPOUSE = goog.getMsg('Spouse')
  child = new net.bluemind.ui.form.TextField(MSG_SPOUSE);
  child.setId('spouse');
  this.addChild(child, true);
  child.setVisible(false);
  /** @meaning contact.vcard.security.key */
  var MSG_PUB_KEY_CERT = goog.getMsg('PubKeyCert')
  child = new net.bluemind.ui.form.LongTextField(MSG_PUB_KEY_CERT);
  child.setId('pubkeycert');
  this.addChild(child, true);
  child.setVisible(false);

  /** @meaning contact.vcard.categories */
  var MSG_CATEGORIES = goog.getMsg('Categories');
  child = new net.bluemind.ui.form.TagField(MSG_CATEGORIES);
  child.setId('categories');
  this.addChild(child, true);

  this.addMoreFieldsButton_();
  
  /** @meaning contact.vcard.note */
  var MSG_NOTE = goog.getMsg('Note');
  child = new net.bluemind.ui.form.RichTextField(MSG_NOTE);
  child.setId('note');
  this.addChild(child, true);

  this.hiddenFields();

  this.getHandler().listen(this.getChild('header').getChild('photo'), 'add-photo', this.handlePhoto);

  // BM-6460 
  this.getHandler().listen(this.getElement(), goog.events.EventType.SUBMIT, this.handleSubmit_);


};
goog.inherits(net.bluemind.contact.individual.edit.ui.IndividualForm, net.bluemind.ui.form.Form);

net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.deletePhoto = false;

net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.handleSubmit_= function(e) {
  e.preventDefault();
  if (this.view != null) {
    this.view.dispatchEvent('save');
  }
}

net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.handlePhoto = function() {

  var photoValue = null;
  if (this.getModel().hasPhoto) {
    photoValue = this.getChild('header').getChild('photo').getValue();
  }
  goog.global['net']['bluemind']['ui']['uploadimage'](photoValue, goog.bind(function(s) {
    this.getChild('header').getChild('photo').setValue('tmpfileupload?uuid=' + s);
    var cmd = new relief.rpc.Command(null, null, "retrievePhoto" + s + goog.now(), "tmpfileupload?uuid=" + s, "GET");
    var client = new net.bluemind.api.BlueMindClient(this.ctx.rpc, '');
    // retrieve image
    client.execute(cmd).then(function(data) {
      this.photoData_ = data;
      this.deletePhoto = false;
    }, null, this);
  }, this), function() {
  }, goog.bind(function() {
    this.getChild('header').getChild('photo').setValue('images/nopicture.png');
    this.deletePhoto = true;
    this.getModel().hasPhoto = false;
  }, this), function() {
  });
}

/** @override */
net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);

  this.photoData_ = null;
  this.getChild('header').getChild('fullname').setValue(model.fullname);
  this.getChild('nickname').setValue(model.nickname);
  this.getChild('header').getChild('company').setValue(model.company);
  this.getChild('division').setValue(model.division);
  this.getChild('header').getChild('department').setValue(model.department);
  this.getChild('header').getChild('job-title').setValue(model.title);
  this.getChild('header').getChild('role').setValue(model.role);
  this.getChild('emails').setValue(model.emails);
  this.getChild('tels').setValue(model.tels);
  this.getChild('addresses').setValue(model.addresses);
  this.getChild('urls').setValue(model.urls);
  if (model.pemCertificate){
    this.getChild('pubkeycert').setValue(model.pemCertificate);
  }
  
  var parser = this.ctx.helper('dateformat').parser;
  
  if (model.birthday){
    var bday = new Date();
    parser.date.parse(model.birthday, bday);
    this.getChild('birthday').setValue(bday);
  } else {
    this.getChild('birthday').setValue(null);
  }
  if (model.anniversary){
    var anniv = new Date();
    parser.date.parse(model.anniversary, anniv);
    this.getChild('anniversary').setValue(anniv);
  } else {
    this.getChild('anniversary').setValue(null);
  }
  this.getChild('manager').setValue(model.manager);
  this.getChild('assistant').setValue(model.assistant);
  this.getChild('spouse').setValue(model.spouse);
  this.getChild('categories').setValue(model.categories);
  this.getChild('note').setValue(model.note);
  this.getChild('impps').setValue(model.impps);
  this.getChild('header').getChild('photo').setValue(model.photo);
  this.getChild('categories').setValue(model.categories);
  this.getChild('header').getChild('container').getElement().innerHTML = model.container.name;

  if (model.errors) {
    var notif = this.getChild('notifications');
    goog.array.forEach(model.errors, function(e) {
      notif.addError( e.property, e.msg);
    }, this);
    this.getChild('notifications').show_();
  }
  this.hiddenFields();

  this.getChild('header').getChild('fullname').focus();

};

/** @override */
net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.createDom = function() {
  goog.base(this, 'createDom');
  goog.dom.classlist.add(this.getElement(), goog.getCssName('vcard'));
  goog.dom.classlist.add(this.getElement(), goog.getCssName('individual'));
};

/**
 * Refresh model with data from the form
 * 
 * @private
 */
net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.getModel = function() {
  var model = goog.base(this, 'getModel') || {};
  model.fullname = this.getChild('header').getChild('fullname').getValue();
  model.nickname = this.getChild('nickname').getValue();
  model.company = this.getChild('header').getChild('company').getValue();
  model.division = this.getChild('division').getValue();
  model.department = this.getChild('header').getChild('department').getValue();
  model.title = this.getChild('header').getChild('job-title').getValue();
  model.role = this.getChild('header').getChild('role').getValue();
  model.emails = this.getChild('emails').getValue();
  model.tels = this.getChild('tels').getValue();
  model.addresses = this.getChild('addresses').getValue();
  model.urls = this.getChild('urls').getValue();
  model.impps = this.getChild('impps').getValue();
  model.birthday = this.getChild('birthday').getValue();
  model.anniversary = this.getChild('anniversary').getValue();
  model.manager = this.getChild('manager').getValue();
  model.assistant = this.getChild('assistant').getValue();
  model.spouse = this.getChild('spouse').getValue();
  model.categories = this.getChild('categories').getValue();
  model.note = this.getChild('note').getValue();
  model.categories = this.getChild('categories').getValue();
  model.pemCertificate = this.getChild('pubkeycert').getValue();
  model.uploadPhoto = this.photoData_;
  model.deletePhoto = this.deletePhoto;
  return model;
};

/**
 * Add a button to add more fields to contact form
 * 
 */
net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.addMoreFieldsButton_ = function() {
  var menu = new goog.ui.Menu();

  /** @meaning contact.addMoreFields */
  var MSG_ADD_MORE_FIELDS = goog.getMsg('Add more fields');
  var button = new goog.ui.MenuButton(MSG_ADD_MORE_FIELDS, menu, goog.ui.LinkButtonRenderer.getInstance());
  button.setId('add-more-fields');
  this.addChild(button, true);
  this.getHandler().listen(menu, goog.ui.Component.EventType.ACTION, this.addField_);
};

/**
 * 
 */
net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.hiddenFields = function() {
  var hidden = [ 'nickname', 'division', 'urls', 'impps', 'anniversary', 'manager', 'assistant', 'spouse', 'pubkeycert' ]
  var menu = this.getChild('add-more-fields').getMenu();

  for (var i = 0; i < hidden.length; i++) {
    var child = this.getChild(hidden[i]);
    var visible = goog.isArray(child.getValue()) ? child.getValue().length > 0 : !!child.getValue();
    child.setVisible(visible);
    var item = menu.getChild(hidden[i]);
    if (!item) {
      var item = new goog.ui.MenuItem(child.label);
      item.setId(child.getId());
      menu.addChild(item, true);
    }
    item.setVisible(!visible);
  }
  var count = 0;
  menu.forEachChild(function(child) {
    return (count + (child.isVisible() ? 1 : 0));
  });
  if (count == 0) {
    menu.setVisible(false);
  } else {
    menu.setVisible(true);
  }

  this.getChild('emails').setVisible(this.systemUser == false);

};

net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.hideTags = function() {
  this.getChild('categories').setVisible(false);
}

/**
 * 
 */
net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.addField_ = function(event) {
  var item = event.target;
  var field = this.getChild(item.getId());
  item.setVisible(false);
  field.setVisible(true);
  var count = 0;
  var menu = this.getChild('add-more-fields').getMenu();

  menu.forEachChild(function(child) {
    return (count + (child.isVisible() ? 1 : 0));
  });
  if (count == 0) {
    menu.setVisible(false);
  }
};

net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.setTags = function(tags) {
  this.getChild('categories').setTags(tags);
}

net.bluemind.contact.individual.edit.ui.IndividualForm.prototype.setView = function(view) {
  this.view = view;
}
