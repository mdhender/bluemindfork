/**
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

/**
 * @fileoverview Event creation bubble graphic componnent.
 */

goog.provide("net.bluemind.calendar.day.ui.ConsultPopup");

goog.require("goog.dom");
goog.require("goog.soy");
goog.require("goog.events.EventType");
goog.require("net.bluemind.calendar.day.templates");
goog.require("net.bluemind.calendar.day.ui.Popup");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.calendar.vevent.VEventEvent");

/**
 * @param {net.bluemind.i18n.DateTimeHelper.Formatter} format Formatter
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.ui.ConsultPopup = function(format, opt_domHelper) {
  goog.base(this, format, opt_domHelper);
  var menu = new goog.ui.Menu();
  
  /** @meaning calendar.action.duplicate */
  var MSG_DUPLICATE = goog.getMsg('Duplicate');
  child = new goog.ui.MenuItem(MSG_DUPLICATE);
  child.setId('duplicate');
  menu.addChild(child, true);

  /** @meaning calendar.action.duplicateOccurrence */
  var MSG_DUPLICATE_OCC = goog.getMsg('Duplicate occurrence');
  child = new goog.ui.MenuItem(MSG_DUPLICATE_OCC);
  child.setId('duplicate-occurrence');
  menu.addChild(child, true);

  child = new goog.ui.ToolbarMenuButton(goog.dom.createDom('div', [ goog.getCssName('goog-button-icon'),
  goog.getCssName('fa'), goog.getCssName('fa-ellipsis-v') ]), menu, goog.ui.style.app.MenuButtonRenderer.getInstance());
  child.setId('others');
  this.addChild(child);
};
goog.inherits(net.bluemind.calendar.day.ui.ConsultPopup, net.bluemind.calendar.day.ui.Popup);

/** @override */
net.bluemind.calendar.day.ui.ConsultPopup.prototype.setListeners = function() {
  goog.base(this, 'setListeners');

  var detail = goog.dom.getElement('eb-btn-event-consult-screen');
  this.getHandler().listen(detail, goog.events.EventType.CLICK, this.showDetails_, false, this);
  this.getHandler().listen(this.getChild('others'), goog.ui.Component.EventType.ACTION, this.duplicate_);
  this.getHandler().listen(this.getChild('others').getMenu(), goog.ui.Component.EventType.SHOW, function(e) {
    this.addAutoHidePartner(e.target.getElement());
  });
  this.getHandler().listen(this.getChild('others').getMenu(), goog.ui.Component.EventType.HIDE, function(e) {
    this.removeAutoHidePartner(e.target.getElement());
  });
  
};
/** @override */
net.bluemind.calendar.day.ui.ConsultPopup.prototype.eraseElement_ = function() {
  this.getChild('others').exitDocument();
  goog.base(this, 'eraseElement_');
}

/** @override */
net.bluemind.calendar.day.ui.ConsultPopup.prototype.drawElement_ = function() {
  this.getChild('others').exitDocument();
  goog.base(this, 'drawElement_');
  this.getChild('others').render(goog.dom.getElement('eb-btn-event-consult-screen').parentElement);
  var model = this.getModel();
  this.getChild('others').getMenu().getChild('duplicate-occurrence').setVisible(model.states.repeat || model.states.exception)
}

net.bluemind.calendar.day.ui.ConsultPopup.prototype.showDetails_ = function(e) {
  e.stopPropagation();
  this.hide();
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.DETAILS, this.getModel());
  this.dispatchEvent(e);
}

/** @override */
net.bluemind.calendar.day.ui.ConsultPopup.prototype.buildContent = function() {
  var calendar = goog.array.find(this.calendars, function(calendar) {
    return calendar.uid == this.getModel().calendar;
  }, this);
  return goog.soy.renderAsElement(net.bluemind.calendar.day.templates.consult, {
    event : this.getModel(),
    calendar : calendar
  });
};


/**
 * diplicate event
 * 
 * @private
 */
net.bluemind.calendar.day.ui.ConsultPopup.prototype.duplicate_ = function(e) {
  var action = e.target.getId();
  var model = this.getModel();
  switch(action) {
    case "duplicate": 
      model.states.main = true;
      break;
  }

  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.DUPLICATE, model);
  this.dispatchEvent(e);
};