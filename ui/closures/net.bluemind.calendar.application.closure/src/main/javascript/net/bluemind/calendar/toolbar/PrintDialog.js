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

goog.provide('net.bluemind.calendar.toolbar.PrintDialog');

goog.require('net.bluemind.calendar.toolbar.templates');
goog.require('goog.ui.Dialog');
goog.require('net.bluemind.calendar.api.PrintClient');
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.date.DateTime");

/**
 * @param {string} opt_class CSS class name for the dialog element, also used as
 * a class name prefix for related elements; defaults to modal-dialog.
 * @param {boolean} opt_useIframeMask Work around windowed controls z-index
 * issue by using an iframe instead of a div for bg element.
 * @param {goog.dom.DomHelper} opt_domHelper Optional DOM helper; see {@link
 * goog.ui.Component} for semantics.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.toolbar.PrintDialog = function(ctx, opt_class, opt_useIframeMask, opt_domHelper) {
  this.ctx = ctx;
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(net.bluemind.calendar.toolbar.PrintDialog, goog.ui.Dialog);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.calendar.toolbar.PrintDialog.prototype.ctx;

/** @inheritDoc */
net.bluemind.calendar.toolbar.PrintDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(net.bluemind.calendar.toolbar.templates.printDialog);
  this.decorateInternal(elem);
};

/** @inheritDoc */
net.bluemind.calendar.toolbar.PrintDialog.prototype.render = function() {
  goog.base(this, 'render');
};

/** @inheritDoc */
net.bluemind.calendar.toolbar.PrintDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(this.getElementByClass(goog.getCssName('save-pdf-btn-export')), goog.events.EventType.CLICK,
      function(e) {
        e.stopPropagation();
        this.print_();
      }, false, this);

  this.getHandler().listen(this.getElementByClass(goog.getCssName('cb-print-details')), goog.events.EventType.CLICK,
      function(e) {
        e.stopPropagation();
        this.setPreview();
      }, false, this);

  this.getHandler().listen(this.getElementByClass(goog.getCssName('sel-print-orientation')),
      goog.events.EventType.CHANGE, function(e) {
        e.stopPropagation();
        this.setPreview();
      }, false, this);

  this.getHandler().listen(this.getElementByClass(goog.getCssName('cb-print-bw')), goog.events.EventType.CLICK,
      function(e) {
        e.stopPropagation();
        this.setPreview();
      }, false, this);
};

/**
 * Print!
 * 
 * @private
 */
net.bluemind.calendar.toolbar.PrintDialog.prototype.print_ = function() {
  var opts = this.getPrintOptions_('PDF');

  var query = new goog.Uri.QueryData();
  query.set('dateBegin', opts['dateBegin']['iso8601']);
  query.set('dateEnd', opts['dateEnd']['iso8601']);
  query.set('format', opts["format"]);
  query.set('showDetail', opts["showDetail"]);
  query.set('color', opts["color"]);
  query.set('view', opts['view']);
  query.set('layout', opts['layout']);
  goog.array.forEach(opts['calendars'], function(c) {
    query.add('calendarUids', c['uid']);
    query.add('calendarColors', c['color']);
  })

  window.location.href = 'calendar/print?' + query.toString();

};

/**
 * Set preview
 */
net.bluemind.calendar.toolbar.PrintDialog.prototype.setPreview = function() {
  var query = this.getPrintOptions_('PNG');
  var e = goog.dom.getElement('bm-ui-form-print-detail');
  goog.style.setStyle(e, 'display', query['view'] == 'AGENDA' ? '' : 'none');

  this.ctx.client('print').print(query).then(function(res) {
    var img = this.getElementByClass(goog.getCssName('print-preview-image'));
    var cssLayout = 'print-preview-image-portrait';
    if (query['layout'] != 'PORTRAIT') {
      cssLayout = 'print-preview-image-landscape';
    }
    img.className = "print-preview-image " + cssLayout;
    img.src = 'data:image/png;base64,' + res['data'];
  }, null, this);

};

/**
 * reset options form
 */
net.bluemind.calendar.toolbar.PrintDialog.prototype.reset = function() {
  var e = goog.dom.getElement('bm-ui-form-print-detail');
  goog.style.setStyle(e, 'display', '');

  var cbDetails = this.getElementByClass(goog.getCssName('cb-print-details'));
  goog.dom.forms.setValue(cbDetails, '');

  var selOrientation = this.getElementByClass(goog.getCssName('sel-print-orientation'));
  goog.dom.forms.setValue(selOrientation, 'auto');

  var cbBW = this.getElementByClass(goog.getCssName('cb-print-bw'));
  goog.dom.forms.setValue(cbBW, '');

};

/**
 * get print options
 * 
 * @param {text} format print format.
 * @return {Object} net.bluemind.calendar.api.PrintOptions
 * @private
 */
net.bluemind.calendar.toolbar.PrintDialog.prototype.getPrintOptions_ = function(format) {
  var cbDetails = this.getElementByClass(goog.getCssName('cb-print-details'));
  var details = goog.dom.forms.getValue(cbDetails) == 'on';

  var cbBW = this.getElementByClass(goog.getCssName('cb-print-bw'));
  var bw = goog.dom.forms.getValue(cbBW) == 'on';

  var selOrientation = this.getElementByClass(goog.getCssName('sel-print-orientation'));
  var orientation = goog.dom.forms.getValue(selOrientation);

  var printOptions = {};
  var from = new net.bluemind.date.Date(this.ctx.session.get('range').getStartDate());
  var to = new net.bluemind.date.Date(this.ctx.session.get('range').getEndDate());

  printOptions['dateBegin'] = this.ctx.helper('date').toBMDateTime(from);
  printOptions['dateEnd'] = this.ctx.helper('date').toBMDateTime(to);
  printOptions['format'] = format;
  printOptions['showDetail'] = details;
  printOptions['color'] = !bw;

  var selectedTag = this.ctx.session.get('selected-tag') || [];
  if (!goog.array.isEmpty(selectedTag)) {
    printOptions['tagsFilter'] = selectedTag;
  }

  if (this.ctx.session.get('view') == 'list') {
    printOptions['view'] = 'AGENDA';
  } else if (this.ctx.session.get('view') == 'week') {
    printOptions['view'] = 'WEEK';
  } else if (this.ctx.session.get('view') == 'day') {
    printOptions['view'] = 'DAY';
  } else if (this.ctx.session.get('view') == 'month') {
    printOptions['view'] = 'MONTH';
  } else {
    printOptions['view'] = 'WEEK';
  }

  if (orientation.toLowerCase() == 'auto') {
    if (this.ctx.session.get('view') == 'day') {
      printOptions['layout'] = 'PORTRAIT';

    } else {
      printOptions['layout'] = 'LANDSCAPE';
    }
  } else {
    printOptions['layout'] = orientation.toUpperCase();
  }

  printOptions['calendars'] = goog.array.map(goog.array.filter(this.ctx.session.get('calendars'), function(c) {
      return c['metadata']['visible'];
  }), function(c) {
    return {
      'uid' : c['uid'],
      'color' : c['metadata']['color']
    };
  });

  return printOptions;
};
