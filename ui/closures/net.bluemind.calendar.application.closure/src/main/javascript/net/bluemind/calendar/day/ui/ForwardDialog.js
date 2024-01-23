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

goog.provide('net.bluemind.calendar.day.ui.ForwardDialog');

goog.require("goog.ui.Dialog");
goog.require('goog.ui.LabelInput');
goog.require("goog.ui.Textarea");
goog.require('net.bluemind.calendar.day.ui.Popup');
goog.require('net.bluemind.calendar.vevent.ac.AttendeeAutocomplete');

/**
 * @param {net.bluemind.i18n.DateTimeHelper.Formatter} format Formatter
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.ui.ForwardDialog = function(ctx, opt_domHelper) {
  goog.base(this, undefined, undefined, opt_domHelper);
  this.autocomplete_ = new net.bluemind.calendar.vevent.ac.AttendeeAutocomplete(ctx);
  
  this.ctx_ = ctx;
  
  /** @meaning calendar.popup.forward.title */
  var MSG_TITLE = goog.getMsg('Add an attendee');
  this.setTitle(MSG_TITLE);
  this.setContent(net.bluemind.calendar.day.templates.forwardDialog());

  // /** @meaning calendar.popup.foward.label */
  var MSG_FIND_ATTENDEE = goog.getMsg('Find attendee...')
  var child = new goog.ui.LabelInput(MSG_FIND_ATTENDEE);
  child.setId('attendee-autocomplete');
  this.addChild(child);
  
    /** @meaning general.send*/
  var MSG_SEND = goog.getMsg('Send')
  var buttons = new goog.ui.Dialog.ButtonSet()
    .addButton({
      key: goog.ui.Dialog.DefaultButtonKeys.OK,
      caption: MSG_SEND
    }, true)
    .addButton(goog.ui.Dialog.ButtonSet.DefaultButtons.CANCEL, false, true)

  this.setButtonSet(buttons);
  this.setDraggable(false);

};
goog.inherits(net.bluemind.calendar.day.ui.ForwardDialog, goog.ui.Dialog);

/**
 * @private
 * @type {net.bluemind.calendar.vevent.ac.AttendeeAutocomplete}
 */
net.bluemind.calendar.day.ui.ForwardDialog.prototype.autocomplete_;

/**
 * @private
 * @type {Object}
 */
net.bluemind.calendar.day.ui.ForwardDialog.prototype.attendee_;

/** @override */
net.bluemind.calendar.day.ui.ForwardDialog.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.getChild('attendee-autocomplete').decorate(this.getElementByClass(goog.getCssName('forward-attendee')));
  this.getButtonSet().getButton(goog.ui.Dialog.DefaultButtonKeys.OK).disabled = true;
  this.autocomplete_.attachInputs(this.getChild('attendee-autocomplete').getElement());
}
/** @override */
net.bluemind.calendar.day.ui.ForwardDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, this.forward_);
  this.getHandler().listen(this.autocomplete_, goog.ui.ac.AutoComplete.EventType.UPDATE, this.addAttendee_);
}

/** @override */
net.bluemind.calendar.day.ui.ForwardDialog.prototype.exitDocument = function() {
  goog.base(this, 'exitDocument');
  this.getHandler().unlisten(this, goog.ui.Dialog.EventType.SELECT, this.forward_);
  this.getHandler().unlisten(this.autocomplete_, goog.ui.ac.AutoComplete.EventType.UPDATE, this.addAttendee_);
}

/** @override */
net.bluemind.calendar.day.ui.ForwardDialog.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  var attendees = goog.array.clone(model.attendees);
  attendees.push(model.organizer);
  this.autocomplete_.setAttendees(model.attendees);
  if (this.isInDocument()) {
    this.clear_();
  }
}

/**
 * 
 * @param {} e 
 */
net.bluemind.calendar.day.ui.ForwardDialog.prototype.addAttendee_ = function(e) {
  this.getChild('attendee-autocomplete').clear();
  var element = this.getElementByClass(goog.getCssName('forward-recipients-list'));
  this.attendee_ = this.adaptAttendee_(e.row, this.getModel());
  var row = soy.renderAsFragment(net.bluemind.calendar.day.templates.forwardRecipient, {
    attendee : this.attendee_
  });
  this.getDomHelper().appendChild(element, row);
  goog.style.setElementShown(element, true);

  goog.style.setElementShown(this.getChild('attendee-autocomplete').getElement(), false);

  var remove = this.getDomHelper().getElementByClass(goog.getCssName('forward-recipient-remove'), row);
  this.getHandler().listen(remove, goog.events.EventType.CLICK, this.clear_);
  this.getButtonSet().getButton(goog.ui.Dialog.DefaultButtonKeys.OK).disabled = false;

};

/**
 * 
 * @param {} e 
 */
net.bluemind.calendar.day.ui.ForwardDialog.prototype.clear_ = function() {
  this.getChild('attendee-autocomplete').clear();
  this.getButtonSet().getButton(goog.ui.Dialog.DefaultButtonKeys.OK).disabled = true;
  var note = this.getElementByClass(goog.getCssName('forward-note'));
  note.value = "";
  var element = this.getElementByClass(goog.getCssName('forward-recipients-list'));
  goog.style.setElementShown(element, false);
  goog.style.setElementShown(this.getChild('attendee-autocomplete').getElement(), true);
  element.innerHTML = "";
  this.attendee_ = null;
}

/**
 * @private
 * @param {} attendee 
 * @returns 
 */
net.bluemind.calendar.day.ui.ForwardDialog.prototype.adaptAttendee_ = function(attendee) {
  attendee['role'] = 'NonParticipant';
  attendee['partStatus'] = 'NeedsAction';
  attendee['sentBy'] = this.getModel().attendee.mailto;
  return attendee;
}

/**
 * @private
 * @param {goog.ui.Dialog.Event} e
 */
net.bluemind.calendar.day.ui.ForwardDialog.prototype.forward_ = function(e) {
  e.stopPropagation();
  var action = e.key;
  if (action == goog.ui.Dialog.DefaultButtonKeys.OK) {
    var model = this.getModel();
    var note = this.getElementByClass(goog.getCssName('forward-note'));  
    this.attendee_['responseComment'] = note.value;
    model.attendees.push(this.attendee_);
    model.sendNotification = true;
    var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.FORWARD, model);
    this.dispatchEvent(e);
  }

};

