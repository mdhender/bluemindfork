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

goog.provide('net.bluemind.calendar.toolbar.IcsImportDialog');

goog.require('net.bluemind.calendar.toolbar.IcsImportInProgressDialog');
goog.require('net.bluemind.calendar.toolbar.templates');
goog.require('net.bluemind.sync.SyncEngine');
goog.require('goog.net.IframeIo');

/**
 * @param {net.bluemind.mvp.ApplicationContext} context
 * 
 * @param {string} opt_class CSS class name for the dialog element, also used as
 *          a class name prefix for related elements; defaults to modal-dialog.
 * @param {boolean} opt_useIframeMask Work around windowed controls z-index
 *          issue by using an iframe instead of a div for bg element.
 * @param {goog.dom.DomHelper} opt_domHelper Optional DOM helper; see {@link
 *          goog.ui.Component} for semantics.
 * 
 * @constructor
 * @extends {goog.ui.Dialog}
 */
net.bluemind.calendar.toolbar.IcsImportDialog = function(ctx, opt_class, opt_useIframeMask, opt_domHelper) {
  goog.base(this, opt_class, opt_useIframeMask, opt_domHelper);
  /** @meaning calendar.toolbar.import */
  var MSG_IMPORT = goog.getMsg("Import");
  /** @meaning calendar.toolbar.ics.import */
  var MSG_IMPORT_ICS = goog.getMsg("Import ics");
  /** @meaning general.cancel */
  var MSG_CANCEL = goog.getMsg("cancel");
  this.ctx = ctx;
  this.setDraggable(false);
  this.setTitle(MSG_IMPORT_ICS);
  var dialogButtons = new goog.ui.Dialog.ButtonSet();
  dialogButtons.addButton({
    key : 'ok',
    caption : MSG_IMPORT_ICS
  }, true, false);
  dialogButtons.addButton({
    key : 'cancel',
    caption : MSG_CANCEL
  }, false, false);
  this.setButtonSet(dialogButtons);
  this.progressDialog_ = new net.bluemind.calendar.toolbar.IcsImportInProgressDialog(this.ctx);

};
goog.inherits(net.bluemind.calendar.toolbar.IcsImportDialog, goog.ui.Dialog);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.calendar.toolbar.IcsImportDialog.prototype.ctx;

/**
 * @private
 */
net.bluemind.calendar.toolbar.IcsImportDialog.prototype.progressDialog_;

/** @inheritDoc */
net.bluemind.calendar.toolbar.IcsImportDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, this.import_);
};

/**
 * set popup visible.
 * 
 * @param {boolean} visible is popup visible.
 */
net.bluemind.calendar.toolbar.IcsImportDialog.prototype.setVisible = function(visible) {
  var calendars = goog.array.filter(this.ctx.session.get('calendars'), function(calendar) {
    return !!calendar['writable']
  });
  calendars = goog.array.map(calendars, function(calendar) {
    return {
      label : calendar['name'],
      id : calendar['uid'],
      isDefault : (calendar['uid'] == this.ctx.session.get('calendar.default'))
    }
  }, this);
  this.setContent(net.bluemind.calendar.toolbar.templates.icsImportDialog({
    calendars : calendars
  }));
  goog.base(this, 'setVisible', visible);
};

/**
 * export
 * 
 * @param {Object} e browser event.
 * @private
 */
net.bluemind.calendar.toolbar.IcsImportDialog.prototype.import_ = function(e) {
  if (e.key == 'ok') {
    this.progressDialog_.setVisible(true);
    var form = this.getElement().getElementsByTagName('form').item(0);
    var calendar = goog.dom.getElement('import-calendar-select').value;

    form.action = "calendar/import?calendar=" + calendar;
    var io = new goog.net.IframeIo();
    this.getHandler().listenOnce(io, goog.net.EventType.SUCCESS, function(e) {
      net.bluemind.sync.SyncEngine.getInstance().stop();
      var io = e.target;
      var taskRefId = io.getResponseText();
      if (taskRefId) {
        if (goog.string.contains(taskRefId, '413 Request Entity Too Large')){
          this.progressDialog_.setStatus('413 Request Entity Too Large');
        } else {
          this.progressDialog_.taskmon(taskRefId);
        }
      }
    }, this);
    io.sendFromForm(form);
  }
};
