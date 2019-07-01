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
 * @fileoverview print dialog component.
 */

goog.provide('bluemind.calendar.ui.event.PrintDialog');

goog.require('bluemind.calendar.event.template');
goog.require('goog.ui.Dialog');

/**
 * @param {string} opt_class CSS class name for the dialog element, also used
 *    as a class name prefix for related elements; defaults to modal-dialog.
 * @param {boolean} opt_useIframeMask Work around windowed controls z-index
 *     issue by using an iframe instead of a div for bg element.
 * @param {goog.dom.DomHelper} opt_domHelper Optional DOM helper; see {@link
 *    goog.ui.Component} for semantics.
 *
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.PrintDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(bluemind.calendar.ui.event.PrintDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.PrintDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.printDialog);
  this.decorateInternal(elem);
};

/** @inheritDoc */
bluemind.calendar.ui.event.PrintDialog.prototype.render = function() {
  goog.base(this, 'render');
};

/** @inheritDoc */
bluemind.calendar.ui.event.PrintDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('save-pdf-btn-export'),
    goog.events.EventType.CLICK, function(e) {
      e.stopPropagation();
      this.print_();
    }, false, this);

  this.getHandler().listen(goog.dom.getElement('cb-print-details'),
    goog.events.EventType.CLICK, function(e) {
      e.stopPropagation();
      this.setPreview();
    }, false, this);

  this.getHandler().listen(goog.dom.getElement('sel-print-orientation'),
    goog.events.EventType.CHANGE, function(e) {
      e.stopPropagation();
      this.setPreview();
    }, false, this);

  this.getHandler().listen(goog.dom.getElement('cb-print-bw'),
    goog.events.EventType.CLICK, function(e) {
      e.stopPropagation();
      this.setPreview();
    }, false, this);
};

/**
 * Print!
 * @private
 */
bluemind.calendar.ui.event.PrintDialog.prototype.print_ = function() {
  var query = this.getQueryData_('pdf');
  // FIXME The spoon does not exist
  window.location.href = 'calendar/print?' + query.toString();
  this.setVisible(false);
};

/**
 * Set preview
 */
bluemind.calendar.ui.event.PrintDialog.prototype.setPreview = function() {
  var query = this.getQueryData_('png');
  var img = goog.dom.getElement('print-preview');
  img.src = 'calendar/print?' + query.toString();
};

/**
 * reset options form
 */
bluemind.calendar.ui.event.PrintDialog.prototype.reset = function() {
  var e = goog.dom.getElement('bm-ui-form-print-detail');
  if (bluemind.view.getView().getName() == 'agenda') {
    goog.style.setStyle(e, 'display', '');
  } else {
    goog.style.setStyle(e, 'display', 'none');
  }

  var cbDetails = goog.dom.getElement('cb-print-details');
  goog.dom.forms.setValue(cbDetails, '');

  var selOrientation = goog.dom.getElement('sel-print-orientation');
  goog.dom.forms.setValue(selOrientation, 'auto');

  var cbBW = goog.dom.getElement('cb-print-bw');
  goog.dom.forms.setValue(cbBW, '');

};

/**
 * get query data
 * @param {text} format print format.
 * @return {goog.Uri.QueryData} query data.
 * @private
 */
bluemind.calendar.ui.event.PrintDialog.prototype.getQueryData_ =
  function(format) {
  var cbDetails = goog.dom.getElement('cb-print-details');
  var details = goog.dom.forms.getValue(cbDetails) == 'on';

  var cbBW = goog.dom.getElement('cb-print-bw');
  var bw = goog.dom.forms.getValue(cbBW) == 'on';

  var selOrientation = goog.dom.getElement('sel-print-orientation');
  var orientation = goog.dom.forms.getValue(selOrientation);

  var query = new goog.Uri.QueryData();
  query.set('from', bluemind.view.getView().getStart().getTime());
  query.set('to', bluemind.view.getView().getEnd().getTime());
  query.set('format', format);
  query.set('details', details);
  query.set('color', !bw);

  if (bluemind.view.getView().getName() == 'days') {
    if (bluemind.manager.getNbDays() == 1) {
      query.set('view', 'day');
      query.set('layout', 'portrait');
    } else {
      query.set('view', 'week');
      query.set('layout', 'landscape');
    }
  } else {
    query.set('view', bluemind.view.getView().getName());
    if (bluemind.view.getView().getName() == 'month') {
      query.set('layout', 'landscape');
    } else {
      query.set('layout', 'portrait');
    }
  }

  if (orientation != 'auto') {
    query.set('layout', orientation);
  }

  var cal;
  var calendars = new Array();
  goog.array.forEach(bluemind.manager.getCalendars(), function(c) {
    if (c.isVisible()) {
      cal = {
        'id': c.getId(),
        'css': c.getClass()
      };
      calendars.push(cal);
    }
  });
  query.set('calendars', goog.json.serialize(calendars));
  query.set('ts', goog.now());
  return query;
};

