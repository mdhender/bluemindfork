goog.provide('net.bluemind.ui.settings.cti.CalendarCtiPartProvider');
goog.require('net.bluemind.ui.settings.cti.template');
goog.require('goog.soy');
goog.require('goog.dom');

/**
 * 
 *
 * @constructor
 */
net.bluemind.ui.settings.cti.CalendarCtiPartProvider = function(model) {
  this.el_ = goog.soy.renderAsElement(
      net.bluemind.ui.settings.cti.template.calendar, null);
  this.domHelper = new goog.dom.DomHelper();
  this.widgetModel = model;
}

/**
 * 
 * @
 */
net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype.attach = function(parent) {
  parent.appendChild( this.el_);
  var input = this.domHelper.getElementByClass(goog.getCssName('phone-fwd'), this.el_);
  var ih = new goog.events.InputHandler(input);
  goog.events.listen(ih, goog.events.InputHandler.EventType.INPUT, this.formatTel, false, this);
}

net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype.detach = function() {
  
}

net.bluemind.ui.settings.cti.CalendarCtiPartProvider.telRegexp = new RegExp('^[0-9]*$');

net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype.formatTel = function() {
  var input = this.domHelper.getElementByClass(goog.getCssName('phone-fwd'), this.el_);

  if (net.bluemind.ui.settings.cti.CalendarCtiPartProvider.telRegexp.test(input.value)) {
    this.previousValue = input.value;
  } else {
    input.value = this.previousValue;

  }
}

net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype.loadModel = function(gmodel) {
  var model = gmodel["user-settings"];
  var forwardValue = null;
  if(model["cal_set_phone_presence"]) {
    forwardValue = model["cal_set_phone_presence"];
  }
  this.previousValue = forwardValue;

  this.foward_to = goog.dom.getElement("cti-phone-presence-fwd-to");
  this.fwd_ = goog.dom.getElement("cti-phone-presence-fwd");
  this.dnd_ = goog.dom.getElement("cti-phone-presence-dnd");
  this.no_ = goog.dom.getElement("cti-phone-presence-no");
  
  switch (model["cal_set_phone_presence"]) {
    case "dnd":
      this.dnd_.checked=true;
      this.foward_to.value = "";
      break;
    case "no":
    case null:
    case "false":
    case "":
      this.no_.checked=true;
      this.foward_to.value = "";
      break;
    default:
      this.fwd_.checked=true;
      this.foward_to.value = forwardValue;
      break;
    
    }
  
  var ro = this.widgetModel['readOnly'] || false;
  this.foward_to['disabled'] = ro;
  this.fwd_['disabled'] = ro;
  this.dnd_['disabled'] = ro;
  this.no_['disabled'] = ro;
}

net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype.saveModel = function(gmodel) {
  if( this.widgetModel['readOnly'] == true) {
    return;
  }
  var model = gmodel["user-settings"];
  if( this.dnd_.checked) {
    model["cal_set_phone_presence"] = "dnd";
  } else if( this.fwd_.checked) {
    model["cal_set_phone_presence"] = this.foward_to.value;
  } else {
    model["cal_set_phone_presence"] = "false";
  }
}

goog.exportSymbol('net.bluemind.ui.settings.cti.CalendarCtiPartProvider',net.bluemind.ui.settings.cti.CalendarCtiPartProvider);
net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype["attach"]=net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype.attach;
net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype["detach"]=net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype.detach;
net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype["loadModel"]=net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype.loadModel;
net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype["saveModel"]=net.bluemind.ui.settings.cti.CalendarCtiPartProvider.prototype.saveModel;
