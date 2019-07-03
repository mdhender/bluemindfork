goog.provide("net.bluemind.contact.vcard.edit.ui.AddressField");

goog.require("goog.ui.Button");
goog.require("goog.ui.Dialog");
goog.require("goog.ui.FlatButtonRenderer");
goog.require("goog.ui.Textarea");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Dialog.ButtonSet");
goog.require("net.bluemind.contact.vcard.edit.ui.CoordinateField");
goog.require("net.bluemind.ui.form.TextField");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} label
 * @param {*} options
 * @param {goog.ui.ControlRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {net.bluemind.contact.vcard.edit.ui.CoordinateField}
 */
net.bluemind.contact.vcard.edit.ui.AddressField = function(label, options, opt_renderer, opt_domHelper) {
  net.bluemind.contact.vcard.edit.ui.CoordinateField.call(this, label, options, null, opt_renderer, opt_domHelper);
  this.addClassName(goog.getCssName("field-address"));
  /** @meaning contact.vcard.editAddress */
  var MSG_EDIT_ADDRESS = goog.getMsg('Edit address');
  var dialog = new goog.ui.Dialog();
  dialog.setTitle(MSG_EDIT_ADDRESS);
  dialog.setId('dialog');

  /** @meaning contact.vcard.street */
  var MSG_STREET = goog.getMsg('Street');
  var child = new net.bluemind.ui.form.TextField(MSG_STREET);
  child.setId('street');
  dialog.addChild(child, true);

  /** @meaning contact.vcard.postalCode */
  var MSG_POSTAL_CODE = goog.getMsg('Postal code');
  child = new net.bluemind.ui.form.TextField(MSG_POSTAL_CODE);
  child.setId('postalcode');
  dialog.addChild(child, true);

  /** @meaning contact.vcard.locality */
  var MSG_LOCALITY = goog.getMsg('Locality');
  child = new net.bluemind.ui.form.TextField(MSG_LOCALITY);
  child.setId('locality');
  dialog.addChild(child, true);

  /** @meaning contact.vcard.poBox */
  var MSG_POST_OFFICE_BOX = goog.getMsg('Post office box');
  child = new net.bluemind.ui.form.TextField(MSG_POST_OFFICE_BOX);
  child.setId('pobox');
  dialog.addChild(child, true);

  /** @meaning contact.vcard.region */
  var MSG_REGION = goog.getMsg('Region');
  child = new net.bluemind.ui.form.TextField(MSG_REGION);
  child.setId('region');
  dialog.addChild(child, true);

  /** @meaning contact.vcard.country */
  var MSG_COUNTRY = goog.getMsg('Country');
  child = new net.bluemind.ui.form.TextField(MSG_COUNTRY);
  child.setId('country');
  dialog.addChild(child, true);
  this.addChild(dialog);
  dialog.render();
}
goog.inherits(net.bluemind.contact.vcard.edit.ui.AddressField, net.bluemind.contact.vcard.edit.ui.CoordinateField);

/** @override */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getChild('dialog'), goog.ui.Dialog.EventType.SELECT, this.handleDialogChanged_);
};

/** @override */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.createFormField = function() {
  var container = goog.base(this, 'createFormField');
  var button = new goog.ui.Button("...", goog.ui.FlatButtonRenderer.getInstance());
  button.setId('button');
  container.addChild(button);
  button.render(container.getElementByClass(goog.getCssName('field-base-field')));
  this.getHandler().listen(button, goog.ui.Component.EventType.ACTION, this.handleButtonClicked_);
};

/** @override */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.createInput = function() {
  var input = new goog.ui.Textarea();
  input.setMinHeight(24);
  input.setMaxHeight(60);
  return input;
};

/**
 * Reset value
 * 
 * @protected
 */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.resetValue = function() {
  var dialog = this.removeChild('dialog', true);
  goog.base(this, 'resetValue');
  this.addChildAt(dialog, 0);
  dialog.render();
};

/**
 * Refresh model with data from the form
 * 
 * @private
 */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.handleButtonClicked_ = function(e) {
  var dialog = this.getChild('dialog');
  var container = e.target.getParent();
  var address = this.parseAddress(container.getChild('field').getValue());
  dialog.getChild('street').setValue(address.street);
  dialog.getChild('postalcode').setValue(address.postalcode);
  dialog.getChild('locality').setValue(address.locality);
  dialog.getChild('pobox').setValue(address.pobox);
  dialog.getChild('region').setValue(address.region);
  dialog.getChild('country').setValue(address.country);
  dialog.setModel(container.getId());
  dialog.setVisible(true);
};

/**
 * 
 * @param {goog.event.Event} event
 */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.handleDialogChanged_ = function(event) {

  if (event.key == goog.ui.Dialog.DefaultButtonKeys.OK) {
    var dialog = this.getChild('dialog');
    if (dialog.isVisible()) {
      var field = this.getChild(dialog.getModel()).getChild('field');
      var address = {};
      address.street = dialog.getChild('street').getValue();
      address.postalcode = dialog.getChild('postalcode').getValue();
      address.locality = dialog.getChild('locality').getValue();
      address.pobox = dialog.getChild('pobox').getValue();
      address.region = dialog.getChild('region').getValue();
      address.country = dialog.getChild('country').getValue();
      field.setValue(this.addressToString(address));
      field.setModel(address);
    }
    event.stopPropagation();
  }
};

/**
 * 
 * @param {goog.event.Event} event
 */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.handleInputChanged = function(container) {
  goog.base(this, 'handleInputChanged', container);
  var field = container.getChild('field');
  var model = this.parseAddress(field.getValue());
  field.setModel(model);
};

/** @override */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.setFieldValue = function(component, value) {
  component.getChild('field').setModel(value.value);
  component.getChild('field').setValue(this.addressToString(value.value));
};

/** @override */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.getFieldValue = function(component) {
  return component.getChild('field').getModel();
};

/**
 * 
 */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.parseAddress = function(value) {
  var address = value.split("\n");
  var result = {};

  if (address.length >= 3)
    result.country = address.pop().split(' ');

  if (result.country && result.country.length > 1)
    result.region = result.country.shift();

  if (result.country && result.country.length > 0)
    result.country = result.country.join(' ');

  if (address.length > 1)
    result.locality = address.pop().split(" ");

  if (result.locality && result.locality.length > 1)
    result.postalcode = result.locality.shift();

  if (result.locality && result.locality.length > 0)
    result.locality = result.locality.join(' ');

  if (address.length > 1)
    result.pobox = address.pop();

  result.street = address.join("\n");

  return result;
};

/**
 * 
 * @param {Object} value
 * @return {string}
 */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.addressToString = function(value) {
  var temp = [], lines = [];
  lines.push(value.street || '');
  if (value.pobox)
    lines.push(value.pobox);
  if (value.postalcode)
    temp.push(value.postalcode);
  if (value.locality)
    temp.push(value.locality);
  if (temp.length > 0)
    lines.push(temp.join(' '));
  temp = [];
  if (value.region)
    temp.push(value.region);
  if (value.country)
    temp.push(value.country);
  if (temp.length > 0)
    lines.push(temp.join(' '));
  return lines.join("\n");
};

/** @override */
net.bluemind.contact.vcard.edit.ui.AddressField.prototype.handleLabelChanged = function(container) {
};
