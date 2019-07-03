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

goog.provide('net.bluemind.commons.ui.TaskProgressDialog');
goog.require('net.bluemind.core.task.api.TaskClient');
goog.require('net.bluemind.commons.ui.TaskProgressDialogTemplate');
/**
 * 
 * @param {net.bluemind.mvp.ApplicationContext} context
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
net.bluemind.commons.ui.TaskProgressDialog = function(ctx, opt_class, opt_useIframeMask, opt_domHelper) {
  goog.base(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.ctx = ctx;
  this.setDraggable(false);
  this.setButtonSet(null);
  this.ref_ = null;
  this.timer_ = new goog.Timer(1000);
  goog.events.listen(this.timer_, goog.Timer.TICK, this.watch, false, this);
};
goog.inherits(net.bluemind.commons.ui.TaskProgressDialog, goog.ui.Dialog);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.commons.ui.TaskProgressDialog.prototype.ctx;

/**
 * @type {text}
 * @private
 */
net.bluemind.commons.ui.TaskProgressDialog.prototype.ref_;

/**
 * @type {goog.Timer}
 * @private
 */
net.bluemind.commons.ui.TaskProgressDialog.prototype.timer_;

/** @inheritDoc */
net.bluemind.commons.ui.TaskProgressDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
};

/**
 * set popup visible.
 * 
 * @param {boolean} visible is popup visible.
 */
net.bluemind.commons.ui.TaskProgressDialog.prototype.setVisible = function(visible) {
  this.setContent(net.bluemind.commons.ui.TaskProgressDialogTemplate.progress());
  this.setButtonSet(null);
  goog.base(this, 'setVisible', visible);
};

/**
 * set popup visible.
 * 
 * @param {boolean} visible is popup visible.
 */
net.bluemind.commons.ui.TaskProgressDialog.prototype.setStatus = function(msg) {
  this.setContent(msg);
  var dialogButtons = new goog.ui.Dialog.ButtonSet();
  dialogButtons.addButton({
    key : 'ok',
    caption : 'ok'
  }, true, false);
  this.setButtonSet(dialogButtons);
};

net.bluemind.commons.ui.TaskProgressDialog.prototype.setProgress = function(percent) {
  var bar = goog.dom.getElementByClass('bar', this.getElement());
  goog.style.setWidth(bar, percent + '%');
}
/**
 * 
 */
net.bluemind.commons.ui.TaskProgressDialog.prototype.taskmon = function(ref, onFinish) {
  this.ref_ = ref;
  this.onFinish = onFinish || goog.bind(this.defaultOnFinish_, this);
  this.timer_.start();
};

net.bluemind.commons.ui.TaskProgressDialog.prototype.defaultOnFinish_ = function(e) {
  /** @meaning calendar.ics.ko */
  var MSG_ICS_KO = goog.getMsg('Fail to import ICS.');

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
    } else {
      msg = MSG_ICS_KO;
    }
    this.setStatus(msg);
  }

}
/**
 * 
 */
net.bluemind.commons.ui.TaskProgressDialog.prototype.watch = function() {
  var taskClient = new net.bluemind.core.task.api.TaskClient(this.ctx.rpc, '', this.ref_);

  taskClient.status().then(function(e) {
    if (e['state'] == 'Success' || e['state'] == 'InError') {
      this.timer_.stop();
      this.onFinish(e);
      net.bluemind.sync.SyncEngine.getInstance().start();
    } else {
      var total = e['steps'];
      var progress = e['progress'];

      this.setProgress((progress * 100) / total);
    }
  }, null, this);
};
