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
 * @fileoverview export ics dialog component.
 */

goog.provide("net.bluemind.calendar.toolbar.IcsExportDialog");

goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.soy");
goog.require("goog.dom.classes");
goog.require("goog.dom.forms");
goog.require("goog.events.EventType");
goog.require("goog.ui.Dialog");
goog.require("goog.ui.InputDatePicker");
goog.require("goog.ui.Dialog.EventType");
goog.require("net.bluemind.calendar.toolbar.templates");
goog.require("net.bluemind.date.DateTime");
goog.require("bluemind.i18n.DateTimeHelper");

/**
 * 
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
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.toolbar.IcsExportDialog = function(ctx, opt_class, opt_useIframeMask, opt_domHelper) {
  this.ctx = ctx;
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(net.bluemind.calendar.toolbar.IcsExportDialog, goog.ui.Dialog);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.calendar.toolbar.IcsExportDialog.prototype.ctx;

/** @inheritDoc */
net.bluemind.calendar.toolbar.IcsExportDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(net.bluemind.calendar.toolbar.templates.icsExportDialog, {
    calendars : []
  });
  this.decorateInternal(elem);
};

/**
 * ICS export date begin
 * 
 * @type {goog.ui.InputDatePicker}
 * @private
 */
net.bluemind.calendar.toolbar.IcsExportDialog.prototype.picker_;

/** @inheritDoc */
net.bluemind.calendar.toolbar.IcsExportDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.picker_ = new goog.ui.InputDatePicker(bluemind.i18n.DateTimeHelper.getInstance().getDateFormatter(),
    bluemind.i18n.DateTimeHelper.getInstance().getDateParser());
  this.picker_.decorate(goog.dom.getElement('bm-ui-ics-export-date'));
  this.picker_.getDatePicker().setAllowNone(false);
  this.picker_.getDatePicker().setShowToday(false);

  this.toggleExportDate_();

  this.getHandler().listen(goog.dom.getElement('radio-export-all'), goog.events.EventType.CHANGE, function(e) {
    this.toggleExportDate_();
  }, false, this);

  this.getHandler().listen(goog.dom.getElement('radio-export-date'), goog.events.EventType.CHANGE, function(e) {
    this.toggleExportDate_();
  }, false, this);

  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, function(e) {
    e.stopPropagation();
    if (e.key == 'export') {
      this.export_();
    }
  }, false, this);

  this.getHandler().listen(goog.dom.getElement('ics-export-btn-export'), goog.events.EventType.CLICK, function(e) {
    e.stopPropagation();
    this.export_();
  }, false, this);

};

/** @override */
net.bluemind.calendar.toolbar.IcsExportDialog.prototype.setVisible = function(visible) {
  if (visible) {
    var select = goog.dom.getElement('export-calendar-select');

    var calendars = goog.array.map(this.ctx.session.get('calendars'), function(calendar) {
      return {
        label : calendar['name'],
        id : calendar['uid'],
        isDefault : (calendar['uid'] == this.ctx.session.get('calendar.default'))
      }
    }, this);

    select.innerHTML = net.bluemind.calendar.toolbar.templates.icsCalendarSelect({
      calendars : calendars
    });
  }
  goog.base(this, 'setVisible', visible);
};

/**
 * export
 * 
 * @private
 */
net.bluemind.calendar.toolbar.IcsExportDialog.prototype.export_ = function() {
  var calendar = goog.dom.forms.getValue(goog.dom.getElement('export-calendar-select'));
  var target = 'calendar/export?container=' + calendar;
  var radio = goog.dom.getElement('radio-export-date');
  var value = goog.dom.forms.getValue(radio);
  if (value) {
    var date = new net.bluemind.date.DateTime(this.picker_.getDate());
    target += '&date=' + date.getTime();
  }
  window.location.href = target; // Crappy
  this.setVisible(false);
};

/**
 * toggle export date picker
 * 
 * @private
 */
net.bluemind.calendar.toolbar.IcsExportDialog.prototype.toggleExportDate_ = function() {
  var input = goog.dom.getElement('bm-ui-ics-export-date');
  var radio = goog.dom.getElement('radio-export-date');
  var value = goog.dom.forms.getValue(radio);
  if (value) {
    goog.dom.forms.setDisabled(input, false);
  } else {
    goog.dom.forms.setDisabled(input, true);
  }
};
