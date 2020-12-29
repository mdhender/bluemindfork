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

goog.provide("net.bluemind.calendar.day.ui.ReplyInvitation");

goog.require("goog.dom");
goog.require("goog.soy");
goog.require("goog.events.EventType");
goog.require("goog.ui.Component");

goog.require("goog.ui.Menu");
goog.require("goog.ui.MenuButton");
goog.require("goog.ui.MenuItem");
goog.require('goog.ui.PopupMenu');
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.calendar.vevent.VEventEvent");

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.ui.ReplyInvitation = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
};
goog.inherits(net.bluemind.calendar.day.ui.ReplyInvitation, goog.ui.Component);

/** @override */
net.bluemind.calendar.day.ui.ReplyInvitation.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  if (this.getChild('counter-selection')){
    this.removeChild('counter-selection');
  }

  if (!this.getModel().attendee) {
    // nothing to do
    return;
  }
  var model = this.getModel();

  var menu = new goog.ui.Menu();
  if (model.rrule){
    /** @meaning calendar.action.proposition.series */
    var MSG_PROPOSE_DATE_SERIES = goog.getMsg('Propose a date for the series');
    child = new goog.ui.MenuItem(MSG_PROPOSE_DATE_SERIES);
    child.setId('propositionSeries');
    menu.addChild(child, true);
  }

  /** @meaning calendar.action.proposition */
  var MSG_PROPOSE_DATE = goog.getMsg('Propose a date');
  var child = new goog.ui.MenuItem(MSG_PROPOSE_DATE);
  child.setId('proposition');
  if (model.acceptCounters){
    menu.addChild(child, true);
    child = new goog.ui.MenuButton(goog.dom.createDom('div', [ goog.getCssName('goog-button-icon'),
    goog.getCssName('fa'), goog.getCssName('fa-ellipsis-v'), goog.getCssNam ]), menu, goog.ui.style.app.MenuButtonRenderer.getInstance());
    child.setId('counter-selection');
    this.addChild(child);
  }

  var invitButtons = goog.soy.renderAsFragment(net.bluemind.calendar.day.templates.participation_, this.getModel());
  var dom = this.getDomHelper();
  dom.appendChild(this.getElement(), invitButtons);
  var rrule = this.getModel().rrule;

  var pmAccepted = new goog.ui.PopupMenu();
  pmAccepted.setToggleMode(true);
  pmAccepted.decorate(this.getElementByClass('partstat-accepted-menu'));
  pmAccepted.attach(this.getElementByClass('eb-btn-accepted-menu'), goog.positioning.Corner.BOTTOM_LEFT,
      goog.positioning.Corner.TOP_LEFT);

  // Yes
  // Accept
  this.getHandler().listen(this.getElementByClass('eb-btn-event-accepted-default-notification'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateInstancePartStatus_('Accepted', true, false, rrule);
      });

  // Accept + note
  this.getHandler().listen(this.getElementByClass('eb-btn-event-accepted-edit-notification'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateInstancePartStatus_('Accepted', null, true, rrule);
      });
  
  // Accept
  this.getHandler().listen(
      this.getElementByClass('eb-btn-event-accepted-default-notification-series', this.getElement()),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateSeriesPartStatus_('Accepted', true, false);
      });

  // Accept + note
  this.getHandler().listen(this.getElementByClass('eb-btn-event-accepted-edit-notification-series'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateSeriesPartStatus_('Accepted', null, true);
      });

  if (model.acceptCounters){
    this.getChild('counter-selection').exitDocument();
    this.getChild('counter-selection').render(this.getElementByClass('counterselect'));
    
    this.getHandler().listen(this.getChild('counter-selection'), goog.ui.Component.EventType.ACTION, function(e){
      var action = e.target.getId();
      if (action == 'proposition'){
        this.proposeACounter_('event');
      } else {
        this.proposeACounter_('series');
      }
    });
  }

  var pmNeedsAction = new goog.ui.PopupMenu();
  pmNeedsAction.setToggleMode(true);
  pmNeedsAction.decorate(this.getElementByClass('partstat-tentative-menu'));
  pmNeedsAction.attach(this.getElementByClass('eb-btn-tentative-menu'), goog.positioning.Corner.BOTTOM_LEFT,
      goog.positioning.Corner.TOP_LEFT);

  // Tentative
  this.getHandler().listen(this.getElementByClass('eb-btn-event-tentative-default-notification'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateInstancePartStatus_('Tentative', true, false, rrule);
      });

  // Tentative + note
  this.getHandler().listen(this.getElementByClass('eb-btn-event-tentative-edit-notification'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateInstancePartStatus_('Tentative', null, true, rrule);
      });

  // Tentative
  this.getHandler().listen(this.getElementByClass('eb-btn-event-tentative-default-notification-series'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateSeriesPartStatus_('Tentative', true, false);
      });

  // Tentative + note
  this.getHandler().listen(this.getElementByClass('eb-btn-event-tentative-edit-notification-series'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateSeriesPartStatus_('Tentative', null, true);
      });


  var pmDeclined = new goog.ui.PopupMenu();
  pmDeclined.setToggleMode(true);
  pmDeclined.decorate(this.getElementByClass('partstat-declined-menu'));
  pmDeclined.attach(this.getElementByClass('eb-btn-declined-menu'), goog.positioning.Corner.BOTTOM_LEFT,
      goog.positioning.Corner.TOP_LEFT);

  // Nein nein nein
  // Decline
  this.getHandler().listen(this.getElementByClass('eb-btn-event-declined-default-notification'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateInstancePartStatus_('Declined', true, false, rrule);
      });

  // Decline + note
  this.getHandler().listen(this.getElementByClass('eb-btn-event-declined-edit-notification'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateInstancePartStatus_('Declined', null, true, rrule);
      });


  // Decline
  this.getHandler().listen(this.getElementByClass('eb-btn-event-declined-default-notification-series'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateSeriesPartStatus_('Declined', true, false);
      });

  // Decline + note
  this.getHandler().listen(this.getElementByClass('eb-btn-event-declined-edit-notification-series'),
      goog.events.EventType.MOUSEDOWN, function(e) {
        this.updateSeriesPartStatus_('Declined', null, true);
      });


};

/**
 * Change attendee participation for an instance
 * 
 * @private
 */
net.bluemind.calendar.day.ui.ReplyInvitation.prototype.updateInstancePartStatus_ = function(partStatus, notification,
    note, rrule) {
  var model = this.getModel();
  model.participation = partStatus;
  model.sendNotification = notification;
  model.addNote = note;
  model.recurringDone = true;
  var evt = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.PART, model);
  this.dispatchEvent(evt);
};

/**
 * Change attendee participation for an events series
 * 
 * @private
 */
net.bluemind.calendar.day.ui.ReplyInvitation.prototype.updateSeriesPartStatus_ = function(partStatus, notification,
    note) {
  var model = this.getModel();
  model.participation = partStatus;
  model.sendNotification = notification;
  model.addNote = note;
  model.recurringDone = true;
  model.states.main = true;
  var evt = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.PART, model);
  this.dispatchEvent(evt);
};

/**
 * Add a counter proposition
 * 
 * @private
 */
net.bluemind.calendar.day.ui.ReplyInvitation.prototype.proposeACounter_ = function(target) {
  this.getModel().selectedPartStatus = 'Tentative';
  this.getModel().target = target;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.COUNTER_DETAILS, this.getModel());
  this.dispatchEvent(e)
};
