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
 * @fileoverview Calendar toolbar view.
 */

goog.provide("net.bluemind.calendar.toolbar.ToolbarView");

goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.date.Date");
goog.require("goog.dom.classlist");
goog.require("goog.i18n.DateTimeFormat");
goog.require("goog.i18n.DateTimeFormat.Format");
goog.require("goog.ui.Button");
goog.require("goog.ui.Component");
goog.require("goog.ui.Control");
goog.require("goog.ui.LinkButtonRenderer");
goog.require("goog.ui.Menu");
goog.require("goog.ui.MenuButton");
goog.require("goog.ui.MenuItem");
goog.require("goog.ui.ToolbarSeparator");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.style.app.ButtonRenderer");
goog.require("goog.ui.style.app.MenuButtonRenderer");
goog.require("net.bluemind.calendar.toolbar.IcsExportDialog");
goog.require("net.bluemind.calendar.toolbar.IcsImportDialog");
goog.require("net.bluemind.calendar.toolbar.PrintDialog");
/**
 * View class for Calendar months view.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.toolbar.ToolbarView = function(ctx, opt_domHelper) {
  this.ctx = ctx;
  goog.base(this, opt_domHelper);
  var renderer = goog.ui.style.app.ButtonRenderer.getInstance();

  var child = new goog.ui.Control();
  child.setId('dateRange');
  child.addClassName(goog.getCssName('dateRange'));
  child.addClassName(goog.getCssName("goog-inline-block"));
  this.addChild(child);

  /** @meaning calendar.toolbar.today */
  var MSG_TODAY = goog.getMsg('Today');
  var child = new goog.ui.Button(MSG_TODAY, renderer);
  child.setId('today');
  this.addChild(child);

  child = new goog.ui.ToolbarSeparator();
  child.setId('navSeparator');
  this.addChild(child);


  /** @meaning calendar.toolbar.period.previous */
  var MSG_PREV = goog.getMsg('Previous period');
  child = new goog.ui.Button('\u25C4', renderer);
  child.setTooltip(MSG_PREV);
  child.addClassName(goog.getCssName('goog-button-base-first'));
  child.setId('previous');
  this.addChild(child);

  /** @meaning calendar.toolbar.period.next */
  var MSG_NEXT = goog.getMsg('Next period');
  child = new goog.ui.Button('\u25BA', renderer);
  child.setTooltip(MSG_NEXT);
  child.addClassName(goog.getCssName('goog-button-base-last'));
  child.setId('next');
  this.addChild(child);

  // FIXME : RANGE
  child = new goog.ui.Button('', goog.ui.LinkButtonRenderer.getInstance());
  child.setId('pending');
  child.setVisible(false);
  this.addChild(child);
  
  /** @meaning calendar.toolbar.day */
  var MSG_DAY = goog.getMsg('Day');
  child = new goog.ui.Button(MSG_DAY, renderer);
  child.setTooltip(MSG_DAY);
  child.addClassName(goog.getCssName('goog-button-base-first'));
  child.setId('day');
  this.addChild(child);

  /** @meaning calendar.toolbar.week */
  var MSG_WEEK = goog.getMsg('Week');
  child = new goog.ui.Button(MSG_WEEK, renderer);
  child.setTooltip(MSG_WEEK);
  child.addClassName(goog.getCssName('goog-button-base-middle'));
  child.setId('week');
  this.addChild(child);

  /** @meaning calendar.toolbar.month */
  var MSG_MONTH = goog.getMsg('Month');
  child = new goog.ui.Button(MSG_MONTH, renderer);
  child.setTooltip(MSG_MONTH);
  child.addClassName(goog.getCssName('goog-button-base-middle'));
  child.setId('month');
  this.addChild(child);

  /** @meaning calendar.toolbar.list */
  var MSG_LIST = goog.getMsg('List');
  child = new goog.ui.Button(MSG_LIST, renderer);
  child.setTooltip(MSG_LIST);
  child.addClassName(goog.getCssName('goog-button-base-last'));
  child.setId('list');
  this.addChild(child);

  child = new goog.ui.ToolbarSeparator();
  child.setId('separator');
  this.addChild(child);

  var menu = new goog.ui.Menu();
  var dom = this.getDomHelper();

  /** @meaning calendar.toolbar.PDFprint */
  var MSG_PDF_PRINT = goog.getMsg('Print as PDF');
  var content = dom.createDom('div', null, dom
      .createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-print') ]), '\u00A0', MSG_PDF_PRINT);
  child = new goog.ui.MenuItem(content);
  child.setId('pdf-print');
  menu.addChild(child, true);

  /** @meaning calendar.toolbar.refresh */
  var MSG_REFRESH = goog.getMsg('Refresh');
  content = dom.createDom('div', null, dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-refresh') ]),
      '\u00A0', MSG_REFRESH);
  child = new goog.ui.MenuItem(content);
  child.setId('refresh');
  menu.addChild(child, true);

  /** @meaning calendar.toolbar.ics.export */
  var MSG_ICS_EXPORT = goog.getMsg('Export as ICS');
  content = dom.createDom('div', null, dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-upload') ]),
      '\u00A0', MSG_ICS_EXPORT);
  child = new goog.ui.MenuItem(content);
  child.setId('ics-export');
  menu.addChild(child, true);

  /** @meaning calendar.toolbar.ics.import */
  var MSG_ICS_IMPORT = goog.getMsg('Import ICS');
  content = dom.createDom('div', null,
      dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-download') ]), '\u00A0', MSG_ICS_IMPORT);
  child = new goog.ui.MenuItem(content);
  child.setId('ics-import');
  menu.addChild(child, true);

  child = new goog.ui.MenuButton(goog.dom.createDom('div', [ goog.getCssName('goog-button-icon'),
      goog.getCssName('fa'), goog.getCssName('fa-cogs') ]), menu, goog.ui.style.app.MenuButtonRenderer.getInstance());
  child.setId('others');
  this.addChild(child);

  child = new goog.ui.ToolbarSeparator();
  child.setId('endSeparator');
  this.addChild(child);


  var dialog = new net.bluemind.calendar.toolbar.PrintDialog(this.ctx);
  dialog.setId('pdf-print-dialog')
  dialog.setVisible(false);
  this.addChild(dialog, true);
  dialog = new net.bluemind.calendar.toolbar.IcsExportDialog(this.ctx);
  dialog.setId('ics-export-dialog')
  dialog.setVisible(false);
  this.addChild(dialog, true);
  dialog = new net.bluemind.calendar.toolbar.IcsImportDialog(this.ctx);
  dialog.setId('ics-import-dialog')
  dialog.setVisible(false);
  this.addChild(dialog, true);
};
goog.inherits(net.bluemind.calendar.toolbar.ToolbarView, goog.ui.Component);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.toolbar.ToolbarView.prototype.ctx;

/** @override */
net.bluemind.calendar.toolbar.ToolbarView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var dom = this.getDomHelper();
  var el = this.getElement();
  goog.dom.classlist.add(el, goog.getCssName('calendar-toolbar'));
  var west = dom.createDom('div', goog.getCssName('west'));
  dom.append(el, west);
  var east = dom.createDom('div', goog.getCssName('east'));
  dom.append(el, east);
  this.getChild('today').render(west);
  this.getChild('navSeparator').render(west);
  this.getChild('previous').render(west);
  this.getChild('next').render(west);
  this.getChild('dateRange').render(west);
  this.getChild('pending').render(east);
  this.getChild('day').render(east);
  this.getChild('week').render(east);
  this.getChild('month').render(east);
  this.getChild('list').render(east);
  this.getChild('separator').render(east);
  this.getChild('others').render(east);
  this.getChild('endSeparator').render(east);
};

/** @override */
net.bluemind.calendar.toolbar.ToolbarView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getChild('others'), goog.ui.Component.EventType.ACTION, this.mm_);

};

/** @override */
net.bluemind.calendar.toolbar.ToolbarView.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  var range = model.range;
  var view = model.view;
  if (view == 'list') {
    var start = this.ctx.helper('dateformat').formatDate(range.getStartDate());
    this.getChild('dateRange').setContent(start);
  } else if (view == 'month') {
    var middle = new net.bluemind.date.Date();
    middle.setTime((range.getStartDate().getTime() + range.getEndDate().getTime()) / 2);
    var df = new goog.i18n.DateTimeFormat('MMMM yyyy');
    this.getChild('dateRange').setContent(df.format(middle));
  } else if (model['view'] == 'day' || range.count() == 1) {
    var df = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.MEDIUM_DATE);
    this.getChild('dateRange').setContent(df.format(range.getStartDate()));
  } else {
    var df = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.MEDIUM_DATE);
    var start = df.format(range.getStartDate());
    var end = df.format(range.getLastDate());
    this.getChild('dateRange').setContent(start + ' - ' + end);
  }

  this.setActive_(view);

}

/**
 * Set pending invitations count
 * @param {number} count
 */
net.bluemind.calendar.toolbar.ToolbarView.prototype.setPendingCount = function(count) {
  if (count > 1) {
    /** @meaning calendar.toolbar.pending.plural */
    var MSG_PENDING = goog.getMsg('You have {$pending} invitations', {pending: count});
    this.getChild('pending').setContent(MSG_PENDING);
    this.getChild('pending').setTooltip(MSG_PENDING);
    this.getChild('pending').setVisible(true);
  } else if (count > 0){
    /** @meaning calendar.toolbar.pending */
    var MSG_PENDING = goog.getMsg('You have an invitation');
    this.getChild('pending').setContent(MSG_PENDING);
    this.getChild('pending').setTooltip(MSG_PENDING);
    this.getChild('pending').setVisible(true); 
  } else {
    this.getChild('pending').setVisible(false);
  }
};

/**
 * Set the active button
 * 
 * @param {string} id
 * @private
 */
net.bluemind.calendar.toolbar.ToolbarView.prototype.setActive_ = function(id) {
  var buttons = [ 'list', 'month', 'week', 'day' ];
  goog.array.forEach(buttons, function(button) {
    this.getChild(button).setEnabled(button != id);
  }, this);
  var timeNav = goog.array.contains(buttons, id);
  this.getChild('dateRange').setVisible(timeNav);
  goog.array.forEach(['next', 'previous', 'today'], function(button) {
    this.getChild(button).setEnabled(timeNav);
  }, this);
};

/**
 * Handle action event in toolbar menu
 * 
 * @param {goog.events.Event} e
 * @private
 */
net.bluemind.calendar.toolbar.ToolbarView.prototype.mm_ = function(e) {
  var dialog, entryId = e.target.getId();

  if (entryId == 'pdf-print') {
    dialog = this.getChild('pdf-print-dialog');
    dialog.reset();
    dialog.setPreview();
    dialog.setVisible(true);
  } else if (entryId == 'ics-export') {
    dialog = this.getChild('ics-export-dialog');
    dialog.setVisible(true);
  } else if (entryId == 'ics-import') {
    dialog = this.getChild('ics-import-dialog');
    dialog.setVisible(true);
  } else if (entryId == 'refresh') {
	  this.ctx.helper('url').reload();	   
  }

}
