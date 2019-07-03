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

goog.provide("net.bluemind.calendar.day.ui.SendNoteDialog");

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
net.bluemind.calendar.day.ui.SendNoteDialog = function(opt_domHelper) {
  goog.base(this, undefined, undefined, opt_domHelper);

  /** @meaning calendar.popup.note.title */
  var MSG_TITLE = goog.getMsg('Add a note');
  this.setTitle(MSG_TITLE);

  /** @meaning calendar.responseComment */
  var MSG_RESPONSE_TO_ORGANISER = goog.getMsg("Response to organizer");
  var child = new goog.ui.LabelInput(MSG_RESPONSE_TO_ORGANISER);
  child.setId("responseComment");
  this.addChild(child);

  this.setButtonSet(goog.ui.Dialog.ButtonSet.OK_CANCEL);

}
goog.inherits(net.bluemind.calendar.day.ui.SendNoteDialog, goog.ui.Dialog);

/** @override */
net.bluemind.calendar.day.ui.SendNoteDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, this.handle_);
};

/** @override */
net.bluemind.calendar.day.ui.SendNoteDialog.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  if (this.getChild('responseComment').isInDocument()) {
    this.getChild('responseComment').exitDocument();
  }

  /** @meaning calendar.responseComment */
  var MSG_RESPONSE_TO_ORGANISER = goog.getMsg("Response to organizer");
  this.setContent(MSG_RESPONSE_TO_ORGANISER);

  this.getChild('responseComment').render(this.getContentElement());
  goog.style.setStyle(this.getChild('responseComment').getElement(), 'width', '100%');
  if (this.getModel().attendee.responseComment) {
    this.getChild('responseComment').setValue(this.getModel().attendee.responseComment);
  } else {
    this.getChild('responseComment').setValue('');
  }
};
/**
 * @private
 * @param {goog.ui.Dialog.Event} e
 */
net.bluemind.calendar.day.ui.SendNoteDialog.prototype.handle_ = function(e) {
  e.stopPropagation();

  if (e.key == goog.ui.Dialog.DefaultButtonKeys.OK) {
    var model = this.getModel();
    model.sendNotification = true;
    var value = this.getChild('responseComment').getValue();
    model.attendee.responseComment = value;
    model.addNote=null;
    var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.PART, model);
    this.dispatchEvent(e);
  }

};
