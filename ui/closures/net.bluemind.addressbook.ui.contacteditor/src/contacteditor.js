goog.provide('bluemind.contact.ui.ContactEditor');

goog.require('net.bluemind.contact.individual.edit.ui.IndividualForm');
goog.require('net.bluemind.contact.vcard.VCardModelAdapter');
goog.require('net.bluemind.date.DateHelper');
goog.require('net.bluemind.i18n.DateTimeHelper');
goog.require('net.bluemind.mvp.ApplicationContext');
goog.require('relief.rpc.RPCService');
goog.require('relief.cache.Cache');
goog.require('net.bluemind.timezone.TimeZoneHelper');

/** @constructor */
bluemind.contact.ui.ContactEditor = function(model) {
  if (model && model['id']) {
    this['id'] = model['id'];
  }

  this['type'] = 'bluemind.contact.ui.ContactEditor';
  this.handler_ = new goog.events.EventHandler(this);
  this.photoUuid = null;
  var ctx = new net.bluemind.mvp.ApplicationContext();
  var helper = new net.bluemind.i18n.DateTimeHelper();
  ctx.helper('dateformat', helper);
  helper = new net.bluemind.date.DateHelper(helper);
  ctx.helper('date', helper);

  helper = new net.bluemind.timezone.TimeZoneHelper('UTC');
  ctx.helper('timezone', helper);

  ctx.rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
    'X-BM-ApiKey' : goog.global['bmcSessionInfos'].sid,
    'Accept' : 'application/json'
  }));

  this.ctx = ctx;

};

/** @expose */
bluemind.contact.ui.ContactEditor.prototype.loadModel = function(model) {
  var vcardItem = {
    'uid' : model['entryUid'],
    'value' : model['vcard']
  };

  var fmodel = new net.bluemind.contact.vcard.VCardModelAdapter(this.ctx).toModelView(vcardItem, {
    'uid' : model['domainUid']
  });

  if (vcardItem['value']['identification']['photo']) {
    fmodel.photo = '/api/directory/' + model['domainUid'] + '/entry-uid/' + vcardItem['uid'] + '/photo';
    fmodel.hasPhoto = true;
  }
  this.form_ = new net.bluemind.contact.individual.edit.ui.IndividualForm(this.ctx, true);
  this.form_.render(this.containerDiv_);
  this.form_.hideTags();
  this.form_.setModel(fmodel);

};

/** @expose */
bluemind.contact.ui.ContactEditor.prototype.saveModel = function(model) {
  var formModel = this.form_.getModel();
  var vcardItem = new net.bluemind.contact.vcard.VCardModelAdapter(this.ctx).fromModelView(formModel);
  model["vcard"] = vcardItem['value'];
  model["vcard"]['identification']['photo'] = false;

  // do not save tags
  model["vcard"]["explanatory"]["categories"] = [];

  if (formModel.uploadPhoto) {
    model["vcard"]['identification']['photo'] = true;
    model['vcardPhoto'] = formModel.uploadPhoto;
  } else if (formModel.deletePhoto) {
    model['deletePhoto'] = formModel.deletePhoto;
    model["vcard"]['identification']['photo'] = false;
  }
};

/** @expose */
bluemind.contact.ui.ContactEditor.prototype.attach = function(parent) {
  this.containerDiv_ = document.createElement("div");
  parent.appendChild(this.containerDiv_);
};

goog.exportSymbol('bluemind.contact.ui.ContactEditor', bluemind.contact.ui.ContactEditor);
