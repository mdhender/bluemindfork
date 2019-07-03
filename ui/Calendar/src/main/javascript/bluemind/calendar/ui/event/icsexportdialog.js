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

goog.provide('bluemind.calendar.ui.event.IcsExportDialog');

goog.require('bluemind.calendar.event.template');
goog.require('bluemind.date.DateTime');
goog.require('goog.ui.Dialog');
goog.require('goog.ui.InputDatePicker');

/**
 * @param {string} opt_class CSS class name for the dialog element, also used
 *    as a class name prefix for related elements; defaults to modal-dialog.
 * @param {boolean} opt_useIframeMask Work around windowed controls z-index
 *     issue by using an iframe instead of a div for bg element.
 * @param {goog.dom.DomHelper} opt_domHelper Optional DOM helper; see {@link
 *    goog.ui.Component} for semantics.

 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.IcsExportDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(bluemind.calendar.ui.event.IcsExportDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.IcsExportDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.icsExportDialog);
  this.decorateInternal(elem);
};

/** @inheritDoc */
bluemind.calendar.ui.event.IcsExportDialog.prototype.render = function() {
  goog.base(this, 'render');
  this.picker_ = new goog.ui.InputDatePicker(
      bluemind.i18n.DateTimeHelper.getInstance().getDateFormatter(),
      bluemind.i18n.DateTimeHelper.getInstance().getDateParser());
  this.picker_.decorate(goog.dom.getElement('bm-ui-ics-export-date'));
  goog.dom.classes.set(
    this.picker_.getDatePicker().getElement(),
    goog.getCssName('goog-date-picker-popup'));
  this.picker_.getDatePicker().setAllowNone(false);
  this.picker_.getDatePicker().setShowToday(false);
};

/**
 * ICS export date begin
 * @type {goog.ui.InputDatePicker}
 * @private
 */
bluemind.calendar.ui.event.IcsExportDialog.prototype.picker_;

/** @inheritDoc */
bluemind.calendar.ui.event.IcsExportDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');

  this.toggleExportDate_();

  this.getHandler().listen(goog.dom.getElement('radio-export-all'),
    goog.events.EventType.CHANGE, function(e) {
      this.toggleExportDate_();
    }, false, this);

  this.getHandler().listen(goog.dom.getElement('radio-export-date'),
    goog.events.EventType.CHANGE, function(e) {
      this.toggleExportDate_();
    }, false, this);

  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, function(e) {
    e.stopPropagation();
    if (e.key == 'export') {
      this.export_();
    }
  }, false, this);

  this.getHandler().listen(goog.dom.getElement('ics-export-btn-export'),
    goog.events.EventType.CLICK, function(e) {
      e.stopPropagation();
      this.export_();
    }, false, this);

};

/**
 * export
 * @private
 */
bluemind.calendar.ui.event.IcsExportDialog.prototype.export_ = function() {
  var target = 'calendar/export';
  var radio = goog.dom.getElement('radio-export-date');
  var value = goog.dom.forms.getValue(radio);
  if (value) {
    var date = new bluemind.date.DateTime(this.picker_.getDate());
    target += '?date=' + date.getTime();
  }
  window.location.href = target; // Crappy
  this.setVisible(false);
};

/**
 * toggle export date picker
 * @private
 */
bluemind.calendar.ui.event.IcsExportDialog.prototype.toggleExportDate_ =
  function() {
  var input = goog.dom.getElement('bm-ui-ics-export-date');
  var radio = goog.dom.getElement('radio-export-date');
  var value = goog.dom.forms.getValue(radio);
  if (value) {
    goog.dom.forms.setDisabled(input, false);
  } else {
    goog.dom.forms.setDisabled(input, true);
  }
};
