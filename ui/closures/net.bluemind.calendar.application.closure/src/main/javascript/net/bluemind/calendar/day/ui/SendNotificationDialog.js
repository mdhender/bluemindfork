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

goog.provide("net.bluemind.calendar.day.ui.SendNotificationDialog");

goog.require("goog.ui.Dialog");
goog.require("goog.ui.Dialog.ButtonSet");
goog.require("goog.ui.Dialog.DefaultButtonKeys");
goog.require("goog.ui.Dialog.EventType");

/**
 * @constructor
 * 
 * @param {String} opt_class
 * @param {Boolean} opt_useIframeMask
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Dialog}
 */
net.bluemind.calendar.day.ui.SendNotificationDialog = function(opt_domHelper) {
  goog.base(this, undefined, undefined, opt_domHelper);
  /** @meaning calendar.sendNotification.title */
  var MSG_TITLE = goog.getMsg('Send notification ?');
  this.setTitle(MSG_TITLE);
  /** @meaning calendar.dialog.more*/
  var MSG_MORE = goog.getMsg('More options')
    /** @meaning general.send*/
    var MSG_SEND = goog.getMsg('Send')
  var buttons = new goog.ui.Dialog.ButtonSet()
    .addButton({
      key: goog.ui.Dialog.DefaultButtonKeys.YES,
      caption: MSG_SEND
    }, true)
    .addButton({
      key: 'details',
      caption: MSG_MORE
    })
    .addButton(goog.ui.Dialog.ButtonSet.DefaultButtons.CANCEL, false, true)

  this.setButtonSet(buttons);
}
goog.inherits(net.bluemind.calendar.day.ui.SendNotificationDialog, goog.ui.Dialog);

/**
 * @private {string|!goog.events.EventId}
 */
net.bluemind.calendar.day.ui.SendNotificationDialog.prototype.origin_;

/** @override */
net.bluemind.calendar.day.ui.SendNotificationDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, this.handle_);
};

/** @override */
net.bluemind.calendar.day.ui.SendNotificationDialog.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  /** @meaning calendar.sendNotificationCreation.toAttendees */
  var MSG_CONTENT_CREATE_ATTENDEES = goog.getMsg('Would you like to send invitations to attendees ?');
  /** @meaning calendar.sendNotification.toAttendees */
  var MSG_CONTENT_ATTENDEES = goog.getMsg('Would you like to notify attendees of your changes ?');
  if (model.states.master) {
    if (model.states.updating) {
      this.setContent(MSG_CONTENT_ATTENDEES);
    } else {
      this.setContent(MSG_CONTENT_CREATE_ATTENDEES);
    }
  }
};

/**
 * @param {string|!goog.events.EventId} origin
 */
net.bluemind.calendar.day.ui.SendNotificationDialog.prototype.setOrigin = function(origin) {
  this.origin_ = origin;
}

/**
 * @private
 * @param {goog.ui.Dialog.Event} e
 */
net.bluemind.calendar.day.ui.SendNotificationDialog.prototype.handle_ = function(e) {
  e.stopPropagation();
  if (e.key == goog.ui.Dialog.DefaultButtonKeys.YES) {
    this.getModel().sendNotification = true;
    var e = new net.bluemind.calendar.vevent.VEventEvent(this.origin_, this.getModel());
    this.dispatchEvent(e);
  } else if (e.key == 'details') {
    var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.DETAILS, this.getModel());
    this.dispatchEvent(e)
  } else if (e.key == 'cancel') {
    this.setVisible(false);
    var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.REFRESH, this.getModel());
    this.dispatchEvent(e);
  }
};



