goog.provide("net.bluemind.contact.individual.edit.ui.FullNameField");

goog.require("goog.events.InputHandler");
goog.require("goog.events.InputHandler.EventType");
goog.require("goog.ui.Button");
goog.require("goog.ui.Dialog");
goog.require("goog.ui.FlatButtonRenderer");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Dialog.DefaultButtonKeys");
goog.require("goog.ui.Dialog.EventType");
goog.require("net.bluemind.ui.form.TextField");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} label
 * @param {goog.ui.ControlRenderer} opt_renderer
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {net.bluemind.ui.form.TextField}
 */
net.bluemind.contact.individual.edit.ui.FullNameField = function(opt_renderer, opt_domHelper) {
  /** @meaning contact.vcard.fullname */
  var MSG_FULL_NAME = goog.getMsg('Full name');
  net.bluemind.ui.form.TextField.call(this, MSG_FULL_NAME, opt_renderer, opt_domHelper);
  this.addClassName(goog.getCssName('big'));
  this.addClassName(goog.getCssName('no-label'));
  this.addClassName(goog.getCssName('field-fullname'));

  /** @meaning contact.vcard.editFullname */
  var MSG_EDIT_FULLNAME = goog.getMsg('Edit full name details');
  var dialog = new goog.ui.Dialog();
  dialog.setTitle(MSG_EDIT_FULLNAME);
  dialog.setId('dialog');

  /** @meaning contact.vcard.title */
  var MSG_PREFIXE = goog.getMsg('Title');
  var child = new net.bluemind.ui.form.TextField(MSG_PREFIXE);
  child.setId('prefixes');
  dialog.addChild(child, true);

  /** @meaning contact.vcard.fistname */
  var MSG_FIRSTNAME = goog.getMsg('Firstname');
  child = new net.bluemind.ui.form.TextField(MSG_FIRSTNAME);
  child.setId('firstnames');
  dialog.addChild(child, true);

  /** @meaning contact.vcard.otherName */
  var MSG_ADDITIONAL_NAME = goog.getMsg('Other name');
  child = new net.bluemind.ui.form.TextField(MSG_ADDITIONAL_NAME);
  child.setId('additionalNames');
  dialog.addChild(child, true);

  /** @meaning contact.vcard.lastname */
  var MSG_FIRSTNAME = goog.getMsg('Lastname');
  child = new net.bluemind.ui.form.TextField(MSG_FIRSTNAME);
  child.setId('lastnames');
  dialog.addChild(child, true);
  this.addChild(dialog);

  /** @meaning contact.vcard.suffixe */
  var MSG_SUFFIXE = goog.getMsg('Suffixe');
  child = new net.bluemind.ui.form.TextField(MSG_SUFFIXE);
  child.setId('suffixes');
  dialog.addChild(child, true);
  this.addChild(dialog);
  dialog.render();

}
goog.inherits(net.bluemind.contact.individual.edit.ui.FullNameField, net.bluemind.ui.form.TextField);

/** @override */
net.bluemind.contact.individual.edit.ui.FullNameField.prototype.createFormField = function() {
  goog.base(this, 'createFormField');
  var button = new goog.ui.Button("...", goog.ui.FlatButtonRenderer.getInstance());
  button.setId('button');
  this.addChild(button);
  button.render(this.getElementByClass(goog.getCssName('field-base-field')));
};

/** @override */
net.bluemind.contact.individual.edit.ui.FullNameField.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getChild('button'), goog.ui.Component.EventType.ACTION, this.handleButtonClicked_);
  this.getHandler().listen(this.getChild('dialog'), goog.ui.Dialog.EventType.SELECT, this.handleDialogChanged_);
  var input = new goog.events.InputHandler(this.getChild('field').getElement());
  this.registerDisposable(input);
  this.getHandler().listen(input, goog.events.InputHandler.EventType.INPUT, this.handleInputChanged_);
};

/**
 * Refresh model with data from the form
 * 
 * @private
 */
net.bluemind.contact.individual.edit.ui.FullNameField.prototype.handleButtonClicked_ = function() {

  var model = this.getModel() || {};
  var dialog = this.getChild('dialog');
  dialog.getChild('lastnames').setValue(model.lastnames || '');
  dialog.getChild('prefixes').setValue(model.prefixes || '');
  dialog.getChild('firstnames').setValue(model.firstnames || '');
  dialog.getChild('additionalNames').setValue(model.additionalNames || '');
  dialog.getChild('suffixes').setValue(model.suffixes || '');
  this.getChild('dialog').setVisible(true);
};

/**
 * 
 * @param {goog.event.Event} event
 */
net.bluemind.contact.individual.edit.ui.FullNameField.prototype.handleInputChanged_ = function(event) {
  var value = this.getChild('field').getValue();
  var fullname = value.split(' ');
  var model = this.getModel() || {};
  model.value = value;
  model.lastnames = fullname.pop() || '';
  model.prefixes = (fullname.length > 1) ? fullname.shift() : '';
  model.firstnames = fullname.shift() || '';
  model.additionalNames = fullname.join(' ') || '';
};

/**
 * 
 * @param {goog.event.Event} event
 */
net.bluemind.contact.individual.edit.ui.FullNameField.prototype.handleDialogChanged_ = function(event) {
  if (event.key == goog.ui.Dialog.DefaultButtonKeys.OK) {

    var model = this.getModel() || {};
    var dialog = this.getChild('dialog');
    model.lastnames = dialog.getChild('lastnames').getValue();
    model.prefixes = dialog.getChild('prefixes').getValue();
    model.suffixes = dialog.getChild('suffixes').getValue();
    model.firstnames = dialog.getChild('firstnames').getValue();
    model.additionalNames = dialog.getChild('additionalNames').getValue();
    model.value = this.stringModel();
    this.getChild('field').setValue(model.value);
  }
};

/** @override */
net.bluemind.contact.individual.edit.ui.FullNameField.prototype.setValue = function(value) {
  var value = value || {};
  this.setModel(value);
  value.value = this.stringModel();
  goog.base(this, 'setValue', value.value);
};

/**
 * @return {Array}
 */
net.bluemind.contact.individual.edit.ui.FullNameField.prototype.stringModel = function() {
  var fullname = [];
  if (this.getModel().prefixes)
    fullname.push(this.getModel().prefixes);
  if (this.getModel().firstnames)
    fullname.push(this.getModel().firstnames);
  if (this.getModel().additionalNames)
    fullname.push(this.getModel().additionalNames);
  if (this.getModel().lastnames)
    fullname.push(this.getModel().lastnames);
  return fullname.join(' ');
}

/** @override */
net.bluemind.contact.individual.edit.ui.FullNameField.prototype.getValue = function() {
  return this.getModel();
};
