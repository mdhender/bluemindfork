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
 * @fileoverview import ics dialog component.
 */

goog.provide('bluemind.calendar.ui.event.IcsImportDialog');

goog.require('bluemind.calendar.event.template');
goog.require('bluemind.calendar.notification.template');
goog.require('bluemind.calendar.template.i18n');
goog.require('bluemind.sync.SyncEngine');
goog.require('goog.net.IframeIo');


/**
 * @param {string} opt_class CSS class name for the dialog element, also used
 *    as a class name prefix for related elements; defaults to modal-dialog.
 * @param {boolean} opt_useIframeMask Work around windowed controls z-index
 *     issue by using an iframe instead of a div for bg element.
 * @param {goog.dom.DomHelper} opt_domHelper Optional DOM helper; see {@link
 *    goog.ui.Component} for semantics.

 * @constructor
 * @extends {goog.ui.Dialog}
 */
bluemind.calendar.ui.event.IcsImportDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.base(this, opt_class, opt_useIframeMask, opt_domHelper);

  this.setDraggable(false);
  this.setTitle(bluemind.calendar.template.i18n.icsImport());
  var dialogButtons = new goog.ui.Dialog.ButtonSet();
  dialogButtons.addButton(
    {key: 'ok', caption: bluemind.calendar.template.i18n.mport()},
    true, false);
  dialogButtons.addButton(
    {key: 'cancel', caption: bluemind.calendar.template.i18n.cancel()},
    false, false);
  this.setButtonSet(dialogButtons);
};
goog.inherits(bluemind.calendar.ui.event.IcsImportDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.IcsImportDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, this.import_);
};

/**
 * set popup visible.
 * @param {boolean} visible is popup visible.
 */
bluemind.calendar.ui.event.IcsImportDialog.prototype.setVisible =
  function(visible) {
  var calendars = [];
  goog.array.forEach(bluemind.writableCalendars, function(id) {
    var calendar = bluemind.manager.getCalendar(id);
    if (calendar) {
      calendars.push({label: calendar.getLabel(), id: id});
    }
  });
  var my = {label: bluemind.me['displayName'], id: bluemind.me['calendar']};
  this.setContent(bluemind.calendar.event.template.icsImportDialog(
    {calendars: calendars, my: my}));
  goog.base(this, 'setVisible', visible);
};

/**
 * export
 * @param {Object} e browser event.
 * @private
 */
bluemind.calendar.ui.event.IcsImportDialog.prototype.import_ = function(e) {
  if (e.key == 'ok') {
    var d = bluemind.calendar.Controller.getInstance().getIcsImportInProgressDialog();
    d.setVisible(true);
    var form = this.getElement().getElementsByTagName('form').item(0);
    var io = new goog.net.IframeIo();
    this.getHandler().listenOnce(io, goog.net.EventType.SUCCESS, function(e) {
      bluemind.sync.SyncEngine.getInstance().stop();
      var io = e.target;
      var resp = JSON.parse(io.getResponseText());
      var taskRefId = resp['taskRefId'];
      if (taskRefId) {
        d.taskmon(taskRefId);
      }
    });
    io.sendFromForm(form);
  }
};
