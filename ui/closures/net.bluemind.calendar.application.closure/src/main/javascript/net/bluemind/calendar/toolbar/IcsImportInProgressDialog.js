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
 * @fileoverview import ics in progress dialog component.
 */

goog.provide('net.bluemind.calendar.toolbar.IcsImportInProgressDialog');
goog.require('net.bluemind.calendar.toolbar.templates');
goog.require('net.bluemind.core.task.api.TaskClient');
/**
 * @param {net.bluemind.mvp.ApplicationContext} context
 * @param {string} opt_class CSS class name for the dialog element, also used as
 * a class name prefix for related elements; defaults to modal-dialog.
 * @param {boolean} opt_useIframeMask Work around windowed controls z-index
 * issue by using an iframe instead of a div for bg element.
 * @param {goog.dom.DomHelper} opt_domHelper Optional DOM helper; see {@link
 * goog.ui.Component} for semantics.
 * @constructor
 * @extends {goog.ui.Dialog}
 */
net.bluemind.calendar.toolbar.IcsImportInProgressDialog = function(ctx, opt_class, opt_useIframeMask, opt_domHelper) {
  goog.base(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.ctx = ctx;
  this.setDraggable(false);
  /** @meaning calendar.ics.import */
  var MSG_ICS_IMPORT = goog.getMsg('ICS Import');
  this.setTitle(MSG_ICS_IMPORT);
  this.setButtonSet(null);
};
goog.inherits(net.bluemind.calendar.toolbar.IcsImportInProgressDialog, goog.ui.Dialog);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.calendar.toolbar.IcsExportDialog.prototype.ctx;

/**
 * @type {text}
 * @private
 */
net.bluemind.calendar.toolbar.IcsImportInProgressDialog.prototype.ref_;

/**
 * @type {goog.Timer}
 * @private
 */
net.bluemind.calendar.toolbar.IcsImportInProgressDialog.prototype.timer_;

/** @inheritDoc */
net.bluemind.calendar.toolbar.IcsImportInProgressDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
};

/**
 * set popup visible.
 * 
 * @param {boolean} visible is popup visible.
 */
net.bluemind.calendar.toolbar.IcsImportInProgressDialog.prototype.setVisible = function(visible) {
  this.setContent(net.bluemind.calendar.toolbar.templates.icsImportInProgressDialog());
  this.setButtonSet(null);
  this.ref_ = null;
  this.timer_ = new goog.Timer(1000);
  goog.events.listen(this.timer_, goog.Timer.TICK, this.watch, false, this);
  goog.base(this, 'setVisible', visible);
};

/**
 * set popup visible.
 * 
 * @param {boolean} visible is popup visible.
 */
net.bluemind.calendar.toolbar.IcsImportInProgressDialog.prototype.setStatus = function(msg) {
  var elem = goog.dom.getElement('ics-progress-dialog-status');
  elem.innerHTML = msg;
  var dialogButtons = new goog.ui.Dialog.ButtonSet();
  /** @meaning general.close */
  var MSG_ICS_IMPORT = goog.getMsg('Close');
  dialogButtons.addButton({
    key : 'ok',
    caption : MSG_ICS_IMPORT
  }, true, false);
  this.setButtonSet(dialogButtons);
  this.ref_ = null;
  this.timer_ = null;
};

net.bluemind.calendar.toolbar.IcsImportInProgressDialog.prototype.setProgress = function(percent) {
  var elem = goog.dom.getElement('ics-progress-dialog-status');
  var bar = goog.dom.getElementByClass('bar', elem);
  goog.style.setWidth(bar, percent + '%');
}
/**
 * 
 */
net.bluemind.calendar.toolbar.IcsImportInProgressDialog.prototype.taskmon = function(ref) {
  this.ref_ = ref;
  this.timer_.start();
};

/**
 * 
 */
net.bluemind.calendar.toolbar.IcsImportInProgressDialog.prototype.watch = function() {
  var taskClient = new net.bluemind.core.task.api.TaskClient(this.ctx.rpc, '', this.ref_);

  taskClient.status().then(function(e) {
    if (e['state'] == 'Success' || e['state'] == 'InError') {
      /** @meaning calendar.ics.ko */
      var MSG_ICS_KO = goog.getMsg('Fail to import ICS.');
      this.timer_.stop();
      if (e['state'] == 'InError') {
        this.setStatus(MSG_ICS_KO);
      } else {
        var res = JSON.parse(e['result']);
        /** @meaning calendar.ics.success.some */
        var msg, all = res['total'], ok = res['uids'].length;
        if (ok > 1 && ok == all) {
          var MSG_ICS_ALL = goog.getMsg('{$ok} events succesfully imported.', {
            'ok' : ok
          });
          msg = MSG_ICS_ALL;
        } else if (ok == 1 && ok == all) {
          var MSG_ICS_ONLY = goog.getMsg('The event have been succesfully imported.');
          msg = MSG_ICS_ONLY;
        } else if (ok > 1) {
          var MSG_ICS_SOME = goog.getMsg('{$ok} / {$all} events succesfully imported.', {
            'ok' : ok,
            'all' : all
          });
          msg = MSG_ICS_SOME;
        } else if (ok == 1) {
          var MSG_ICS_ONE = goog.getMsg('1 / {$all} event succesfully imported.', {
            'all' : all
          });
          msg = MSG_ICS_ONE;
        } else if (ok == 0) {
          var MSG_ICS_NONE = goog.getMsg('No modified events have been found.');
          msg = MSG_ICS_NONE;
        } else {
          msg = MSG_ICS_KO;
        }
        this.setStatus(msg);
      }
      net.bluemind.sync.SyncEngine.getInstance().start();
    } else {
      var total = e['steps'];
      var progress = e['progress'];

      this.setProgress((progress * 100) / total);
    }
  }, null, this);
};
