goog.provide('net.bluemind.ui.settings.cti.MainCtiPartProvider');
goog.require('net.bluemind.ui.settings.cti.template');
goog.require('goog.soy');
goog.require('goog.dom');
goog.require('goog.events.EventHandler');
goog.require('goog.dom.DomHelper');
goog.require('goog.events.InputHandler');
/**
 * @constructor
 */
net.bluemind.ui.settings.cti.MainCtiPartProvider = function(model) {
  this.el_ = goog.soy.renderAsElement(net.bluemind.ui.settings.cti.template.main, null);
  this.domHelper = new goog.dom.DomHelper();
  this.widgetModel = model;
}

net.bluemind.ui.settings.cti.MainCtiPartProvider.telRegexp = new RegExp('^[0-9]*$');

/**
 */
net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype.attach = function(parent) {
  parent.appendChild(this.el_);
  var input = this.domHelper.getElementByClass(goog.getCssName('phone-fwd'), this.el_);
  var ih = new goog.events.InputHandler(input);

  goog.events.listen(ih, goog.events.InputHandler.EventType.INPUT, this.formatTel, false, this);

}

net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype.formatTel = function() {
  var input = this.domHelper.getElementByClass(goog.getCssName('phone-fwd'), this.el_);

  if (net.bluemind.ui.settings.cti.MainCtiPartProvider.telRegexp.test(input.value)) {
    this.previousValue = input.value;
  } else {
    input.value = this.previousValue;

  }
}

net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype.detach = function() {

}

net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype.loadModel = function(gmodel) {
  var model = gmodel["user-settings"];
  var forwardValue = null;
  if (model["im_set_phone_presence"]) {
    forwardValue = model["im_set_phone_presence"];
  }
  this.previousValue = forwardValue;
  this.foward_to = goog.dom.getElement("cti-im-phone-presence-fwd-to");
  this.fwd_ = goog.dom.getElement("cti-im-phone-presence-fwd");
  this.dnd_ = goog.dom.getElement("cti-im-phone-presence-dnd");
  this.no_ = goog.dom.getElement("cti-im-phone-presence-no");
  switch (model["im_set_phone_presence"]) {
  case "dnd":
    this.dnd_.checked = true;
    this.foward_to.value = "";
    break;
  case "false":
  case undefined:
  case null:
  case "":
    this.no_.checked = true;
    this.foward_to.value = ""
    break;
  default:
    this.fwd_.checked = true;
    this.foward_to.value = forwardValue;
    break;
  }
  
  var ro = this.widgetModel['readOnly'] || false;  
    this.foward_to['disabled'] = ro;
    this.fwd_['disabled'] = ro;
    this.dnd_['disabled'] = ro;
    this.no_['disabled'] = ro;
}

net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype.saveModel = function(gmodel) {
  if( this.widgetModel['readOnly'] == true) {
    return;
  }
  var model = gmodel["user-settings"];
  if (this.dnd_.checked) {
    model["im_set_phone_presence"] = "dnd";
  } else if (this.fwd_.checked) {
    model["im_set_phone_presence"] = this.foward_to.value;
  } else {
    model["im_set_phone_presence"] = "false";
  }
  return true;
}

goog.exportSymbol('net.bluemind.ui.settings.cti.MainCtiPartProvider', net.bluemind.ui.settings.cti.MainCtiPartProvider);
net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype["attach"] = net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype.attach;
net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype["detach"] = net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype.detach;
net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype["loadModel"] = net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype.loadModel;
net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype["saveModel"] = net.bluemind.ui.settings.cti.MainCtiPartProvider.prototype.saveModel;
