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
goog.provide("net.bluemind.calendar.vtodo.consult.VTodoConsultView");

goog.require("goog.dom");
goog.require("goog.ui.Button");
goog.require("goog.ui.Component");
goog.require("goog.ui.Control");
goog.require("goog.ui.Toolbar");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.style.app.ButtonRenderer");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.todolist.ui.vtodo.VTodoCard");

/**
 * @constructor
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.vtodo.consult.VTodoConsultView = function(ctx) {
  goog.ui.Component.call(this);
  this.ctx_ = ctx;

  var child = new goog.ui.Toolbar();
  var renderer = goog.ui.style.app.ButtonRenderer.getInstance();
  child.setId('toolbar');
  this.addChild(child, true);

  /** @meaning calendar.back */
  var MSG_BACK = goog.getMsg('Back to calendar');
  child = new goog.ui.Button(goog.dom.createDom('div', [ goog.getCssName('goog-button-icon'), goog.getCssName('fa'),
      goog.getCssName('fa-lg'), goog.getCssName('fa-chevron-left') ]), goog.ui.style.app.ButtonRenderer.getInstance());
  child.setTooltip(MSG_BACK);
  child.setId('back');
  this.getChild('toolbar').addChild(child, true);

  child = new goog.ui.Control();
  child.setId('notice');
  child.addClassName(goog.getCssName('form-notification'));
  this.addChild(child, true);

  var child = new net.bluemind.todolist.ui.vtodo.VTodoCard();
  child.setId('card');
  this.addChild(child, true);
}
goog.inherits(net.bluemind.calendar.vtodo.consult.VTodoConsultView, goog.ui.Component);

/** @override */
net.bluemind.calendar.vtodo.consult.VTodoConsultView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
};

/** @override */
net.bluemind.calendar.vtodo.consult.VTodoConsultView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var dom = this.getDomHelper();
  var handler = this.getHandler();
  handler.listen(this.getChild('toolbar').getChild('back'), goog.ui.Component.EventType.ACTION, function(e) {
    this.dispatchEvent(net.bluemind.calendar.vevent.EventType.BACK);
  });
}

/** @override */
net.bluemind.calendar.vtodo.consult.VTodoConsultView.prototype.getModel = function() {
  return this.getChild('card').getModel();
};

/** @override */
net.bluemind.calendar.vtodo.consult.VTodoConsultView.prototype.setModel = function(model) {
  model.containerName = model.container;
  this.showSynchronizationNotice_(model);
  this.getChild('card').setModel(model);
};

/**
 * Show synchronization state notice
 * 
 * @param {Object} model
 * @private
 */
net.bluemind.calendar.vtodo.consult.VTodoConsultView.prototype.showSynchronizationNotice_ = function(model) {
  var notice = this.getChild('notice');
  if (model.states.synced) {
    notice.setVisible(false);
    return;
  }
  if (model.states.error && model.error.message) {
    /** @meaning general.error.synchronization */
    var MSG_SYNC_ERROR = goog.getMsg("Synchronization failed : '{$message}'", {
      'message' : model.error.message
    });
    notice.setContent(MSG_SYNC_ERROR);
    notice.enableClassName(goog.getCssName('notice'), false);
    notice.enableClassName(goog.getCssName('error'), true);
  } else if (model.states.error) {
    /** @meaning general.error.synchronization.unkown */
    var MSG_UNKNOWN_SYNC_ERROR = goog.getMsg("Synchronization failed, a new attempt will be made later. Please contact support if this error persists.");
    notice.setContent(MSG_SYNC_ERROR);
    notice.enableClassName(goog.getCssName('notice'), false);
    notice.enableClassName(goog.getCssName('error'), true);
  } else {
    /** @meaning general.notice.notSynchronized */
    var MSG_NOT_SYNCHRONIZED = goog.getMsg("Not all modifications are synchronized with the server yet.");
    notice.setContent(MSG_NOT_SYNCHRONIZED);
    notice.enableClassName(goog.getCssName('notice'), true);
    notice.enableClassName(goog.getCssName('error'), false);
  }
  notice.setVisible(true);
};
