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

/**
 * @fileoverview Event creation bubble graphic componnent.
 */

goog.provide('net.bluemind.calendar.day.ui.UpdatePopup');

goog.require('net.bluemind.calendar.day.ui.Popup');
goog.require('goog.date.Interval');
goog.require('goog.i18n.DateTimeFormat');
goog.require('goog.soy');
goog.require("goog.ui.Menu");
goog.require("goog.ui.MenuButton");
goog.require("goog.ui.MenuItem");
goog.require('goog.ui.PopupMenu');

/**
 * @param {net.bluemind.i18n.DateTimeHelper.Formatter} format Formatter
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.ui.UpdatePopup = function(format, opt_domHelper) {
  goog.base(this, format, opt_domHelper);
  this.lastingHandler = new goog.events.EventHandler(this);
  this.registerDisposable(this.lastingHandler);
  /** @meaning calendar.updatePopup.delete.title */
  var MSG_DELETE_TITLE = goog.getMsg('Delete event');
  /** @meaning calendar.updatePopup.delete.content */
  var MSG_DELETE_CONTENT = goog.getMsg('Would you like to delete this event?');
  var child = new goog.ui.Dialog();
  child.setDraggable(false);
  child.setTitle(MSG_DELETE_TITLE);
  child.setContent(MSG_DELETE_CONTENT);
  child.setButtonSet(goog.ui.Dialog.ButtonSet.YES_NO);
  child.setId('delete-popup');
  this.addChild(child);

  child = new net.bluemind.calendar.day.ui.ReplyInvitation();
  child.setId("reply-invite");
  this.addChild(child);

};
goog.inherits(net.bluemind.calendar.day.ui.UpdatePopup, net.bluemind.calendar.day.ui.Popup);

/** @override */
net.bluemind.calendar.day.ui.UpdatePopup.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.lastingHandler.listen(this.getChild('delete-popup'), goog.ui.Dialog.EventType.SELECT, this.handleDelete_);
};

/** @override */
net.bluemind.calendar.day.ui.UpdatePopup.prototype.buildContent = function() {
  var model = this.getModel();

  var calendar = goog.array.find(this.calendars, function(calendar) {
    return calendar.uid == model.calendar;
  });

  return goog.soy.renderAsElement(net.bluemind.calendar.day.templates.update, {
    event : model,
    calendar : calendar
  });
};

/** @override */
net.bluemind.calendar.day.ui.UpdatePopup.prototype.setModelListeners = function() {

  this.getHandler().listen(goog.dom.getElement('eb-btn-delete'), goog.events.EventType.CLICK, this.deleteEventDialog_);

  this.getHandler().listen(goog.dom.getElement('ecb-update-title'), goog.events.EventType.FOCUS, function() {
    goog.style.setStyle(goog.dom.getElement('eb-btn-update'), 'visibility', 'visible');
  });

  this.getHandler().listen(goog.dom.getElement('eb-btn-update'), goog.events.EventType.CLICK, this.updateEvent_);

  this.getHandler().listen(goog.dom.getElement('eb-btn-event-update-screen'), goog.events.EventType.CLICK,
      this.updateEventScreen_);

  // strange place to do that..
  if (this.getModel().attendee && goog.dom.getElement('partstat-container')) {
    this.getHandler().listen(this.getChild("reply-invite"), net.bluemind.calendar.vevent.EventType.SAVE, this.hide);
    this.getHandler().listen(this.getChild("reply-invite"), net.bluemind.calendar.vevent.EventType.PART, this.hide);
    this.getChild("reply-invite").setModel(this.getModel());
    if (this.getChild("reply-invite").isInDocument()) {
      this.getChild("reply-invite").exitDocument();
    }
    this.getChild("reply-invite").decorate(goog.dom.getElement("partstat-container"));
  }
};

/**
 * delete event
 * 
 * @private
 */
net.bluemind.calendar.day.ui.UpdatePopup.prototype.deleteEventDialog_ = function(e) {
  this.hide();
  this.getChild('delete-popup').setVisible(true);
};

/**
 * delete event
 * 
 * @private
 */
net.bluemind.calendar.day.ui.UpdatePopup.prototype.handleDelete_ = function(e) {
  if (e.key == goog.ui.Dialog.DefaultButtonKeys.YES) {
    if (this.getModel().states.attendee) {
      this.getModel().participation = 'Declined';
    }
    var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.REMOVE, this.getModel());
    this.dispatchEvent(e)
  }
};

/**
 * Update event screen
 * 
 * @param {goog.events.BrowserEvent} e Browser event.
 * @private
 */
net.bluemind.calendar.day.ui.UpdatePopup.prototype.updateEventScreen_ = function(e) {
  this.getModel().summary = goog.dom.forms.getValue(goog.dom.getElement('ecb-update-title'));
  this.hide();
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.DETAILS, this.getModel());
  this.dispatchEvent(e)
};

/**
 * update event
 * 
 * @private
 */
net.bluemind.calendar.day.ui.UpdatePopup.prototype.updateEvent_ = function() {
  this.getModel().summary = goog.dom.forms.getValue(goog.dom.getElement('ecb-update-title'));
  if (this.getModel().summary != '') {
    this.hide();
    var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.SAVE, this.getModel());
    this.dispatchEvent(e)
  } else {
    goog.dom.classes.add(goog.dom.getElement('ecb-update-title'), goog.getCssName('error'));
  }
};
