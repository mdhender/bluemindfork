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

goog.provide('net.bluemind.calendar.day.ui.CreationPopup');

goog.require('net.bluemind.calendar.day.ui.Popup');
goog.require('net.bluemind.calendar.day.templates');
goog.require('net.bluemind.calendar.vevent.VEventEvent');
goog.require('net.bluemind.calendar.vevent.EventType');
goog.require('goog.dom.forms');
goog.require('goog.soy');

/**
 * @param {net.bluemind.i18n.DateTimeHelper.Formatter} format Formatter
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.ui.CreationPopup = function(format, opt_domHelper) {
  goog.base(this, format, opt_domHelper);
};
goog.inherits(net.bluemind.calendar.day.ui.CreationPopup, net.bluemind.calendar.day.ui.Popup);

/** @override */
net.bluemind.calendar.day.ui.CreationPopup.prototype.buildContent = function() {
  var model = this.getModel();

  return goog.soy.renderAsElement(net.bluemind.calendar.day.templates.create, {
    summary : model.summary,
    calendar : model.calendar,
    calendars : this.calendars_
  });
};

/**
 * Calendars
 * 
 * @type {Array}
 */
net.bluemind.calendar.day.ui.CreationPopup.prototype.calendars_;

/**
 * Set current view calendars
 * 
 * @param {Array} calendars
 */
net.bluemind.calendar.day.ui.CreationPopup.prototype.setCalendars = function(calendars) {
  var filtered = goog.array.filter(calendars, function(c) {
    if (c['settings'] && c['settings']['type'] == 'externalIcs') {
      return false;
    }
    return c.states.writable;
  });
  goog.array.sort(filtered, function(l, r) {
    return goog.string.caseInsensitiveCompare(l.name, r.name);
  });
  this.calendars_ = filtered;
};

/** @override */
net.bluemind.calendar.day.ui.CreationPopup.prototype.setVisible = function(visible) {
  goog.base(this, 'setVisible', visible);
  if (visible) {
    this.checkIfInThePast_();
    goog.dom.getElement('ecb-title').focus();
  }
};

/** @override */
net.bluemind.calendar.day.ui.CreationPopup.prototype.setListeners = function() {
  // I think that we should use enterDocument
  // see: http://code.google.com/p/closure-library/wiki/IntroToComponents
  var defaultCalendar = goog.array.find(this.calendars_, function(c) {
    return c.states.main;
  })
  if (!defaultCalendar) {
    if (this.calendars_.length > 0) {
      defaultCalendar = this.calendars_[0];
    } else {
      return;
    }
  }
  goog.dom.getElement('ecb-calendar').value = defaultCalendar.uid;
  this.getHandler().listen(goog.dom.getElement('ecb-calendar'), goog.events.EventType.CHANGE, this.setCalendar);

  this.getHandler().listen(goog.dom.getElement('ecb-form'), goog.events.EventType.SUBMIT, this.createEvent_);

  this.getHandler().listen(goog.dom.getElement('eb-btn-event-new-screen'), goog.events.EventType.CLICK,
      this.createEventScreen_);

  goog.base(this, 'setListeners');
};

/**
 * fix calendar
 */
net.bluemind.calendar.day.ui.CreationPopup.prototype.setCalendar = function() {
  var value = goog.dom.forms.getValue(goog.dom.getElement('ecb-calendar'));
  this.getModel().calendar = value || this.getModel().calendar;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.CHANGE, this.getModel());
  this.dispatchEvent(e);
};

/**
 * Notify if event occurs in the past
 * 
 * @private
 */
net.bluemind.calendar.day.ui.CreationPopup.prototype.checkIfInThePast_ = function() {
  var notification = goog.dom.getElement('ecb-form-notification');
  if (this.getModel().states.past) {
    goog.style.setStyle(notification, 'display', 'block');
  } else {
    goog.style.setStyle(notification, 'display', 'none');
  }
};

/**
 * Create event screen
 * 
 * @param {goog.events.BrowserEvent} e Browser event.
 * @private
 */
net.bluemind.calendar.day.ui.CreationPopup.prototype.createEventScreen_ = function(e) {
  e.stopPropagation();
  this.getModel().summary = goog.dom.forms.getValue(goog.dom.getElement('ecb-title'));
  this.hide();
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.DETAILS, this.getModel());
  this.dispatchEvent(e)
};

/**
 * create an event
 * 
 * @private
 */
net.bluemind.calendar.day.ui.CreationPopup.prototype.createEvent_ = function() {
  this.getModel().summary = goog.dom.forms.getValue(goog.dom.getElement('ecb-title'));
  if (this.getModel().summary != '') {
    var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.SAVE, this.getModel());
    this.dispatchEvent(e)
    this.hide();
  } else {
    goog.dom.classes.add(goog.dom.getElement('ecb-title'), goog.getCssName('error'));
  }
};
