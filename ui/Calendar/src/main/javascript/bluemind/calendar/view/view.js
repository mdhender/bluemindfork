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
 * @fileoverview Calendar view.
 */

goog.provide('bluemind.calendar.View');

goog.require('bluemind.calendar.template.i18n');
goog.require('bluemind.calendar.ui.widget.MiniCalendar');
goog.require('bluemind.calendar.ui.widget.ViewSelector');
goog.require('bluemind.calendar.utils.AutoComplete');
goog.require('bluemind.calendar.view.Agenda');
goog.require('bluemind.calendar.view.Days');
goog.require('bluemind.calendar.view.Forms');
goog.require('bluemind.calendar.view.Month');
goog.require('bluemind.calendar.view.Pending');
goog.require('bluemind.net.OnlineHandler');
goog.require('bluemind.ui.Toolbar');
goog.require('bluemind.ui.style.PrimaryActionButtonRenderer');
goog.require('goog.i18n.DateTimeFormat');
goog.require('goog.ui.Button');
goog.require('goog.ui.Component.EventType');
goog.require('goog.ui.CustomButtonRenderer');
goog.require('goog.ui.LabelInput');
goog.require('goog.ui.ToolbarSeparator');
goog.require('goog.ui.style.app.ButtonRenderer');

/**
 * BlueMind Calendar view
 *
 * @constructor
 */
bluemind.calendar.View = function(manager, settings) {
  this.manager_ = manager;
  this.settings_ = settings;
  var create = new goog.ui.Button(
    bluemind.calendar.template.i18n.createEvent(),
    new bluemind.ui.style.PrimaryActionButtonRenderer.getInstance());
  create.render(goog.dom.getElement('actions'));
  goog.events.listen(create, goog.ui.Component.EventType.ACTION,
    this.newEventForm, false, this);

  // Left panel datepicker
  this.minical_ = new bluemind.calendar.ui.widget.MiniCalendar();
  this.minical_.render(goog.dom.getElement('datePicker'));

  // View selector
  this.viewSelector_ = new bluemind.calendar.ui.widget.ViewSelector();
  this.viewSelector_.render(goog.dom.getElement('viewSelectorContainer'));

  // Autocomplete
  var ac = new bluemind.calendar.utils.AutoComplete(
    'calendar/ac', 'calendarAutoComplete', this.manager_.visibleCalendars_);
};

/**
 * Calendar manager
 *
 * @type {bluemind.calendar.Manager}
 * @private
 */
bluemind.calendar.View.prototype.manager_;

/**
 * Calendar manager
 *
 * @type {Element}
 * @private
 */
bluemind.calendar.View.prototype.view_;

/**
 * Calendar manager
 *
 * @type {Function}
 * @private
 */
bluemind.calendar.View.prototype.lastView_;

/**
 * Calendar date picker
 *
 * @type {bluemind.calendar.ui.widget.MiniCalendar}
 * @private
 */
bluemind.calendar.View.prototype.minical_;

/**
 * Calendar view selector
 *
 * @type {bluemind.calendar.ui.widget.ViewSelector}
 * @private
 */
bluemind.calendar.View.prototype.viewSelector_;

/**
 * Event handler.
 * @type {goog.events.EventHandler}
 * @private
 */
bluemind.calendar.View.prototype.handler_;

/**
 * Calendar current view
 *
 * @return {bluemind.calendar.view} calendar view.
 */
bluemind.calendar.View.prototype.getView = function() {
  return this.view_;
};

/**
 *
 * @return {bluemind.calendar.view} calendar view.
 */
bluemind.calendar.View.prototype.getViewSelector = function() {
  return this.viewSelector_;
};

/**
 * Build navigation tookbar
 *
 */
bluemind.calendar.View.prototype.navToolbar = function() {
  var tb = bluemind.ui.Toolbar.getInstance();
  tb.removeChildren();

  this.getHandler().removeAll();

  var today = new goog.ui.Button(bluemind.calendar.template.i18n.today(),
    goog.ui.style.app.ButtonRenderer.getInstance());
  tb.getWest().addChild(today, true);
  this.getHandler().listen(today, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.today();
  });

  tb.getWest().addChild(new goog.ui.ToolbarSeparator(), true);

  var prev = new goog.ui.Button('\u25C4',
    goog.ui.style.app.ButtonRenderer.getInstance());
  prev.setTooltip(bluemind.calendar.template.i18n.previousPeriod());
  prev.addClassName(goog.getCssName('goog-button-base-first'));
  tb.getWest().addChild(prev, true);
  this.getHandler().listen(prev, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.prev();
  });

  var next = new goog.ui.Button('\u25BA',
    goog.ui.style.app.ButtonRenderer.getInstance());
  next.setTooltip(bluemind.calendar.template.i18n.nextPeriod());
  next.addClassName(goog.getCssName('goog-button-base-last'));
  tb.getWest().addChild(next, true);
  this.getHandler().listen(next, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.next();
  });

  var dateRange = new goog.dom.createElement('span');
  goog.dom.setProperties(dateRange, {'id': 'dateRange'});
  goog.dom.classes.add(dateRange, goog.getCssName('dateRange'));
  goog.dom.appendChild(tb.getWest().getContentElement(), dateRange);

  var day = new goog.ui.Button(bluemind.calendar.template.i18n.day(),
    goog.ui.style.app.ButtonRenderer.getInstance());
  day.addClassName(goog.getCssName('goog-button-base-first'));
  tb.getEast().addChild(day, true);
  this.getHandler().listen(day, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.day();
  });

  var week = new goog.ui.Button(bluemind.calendar.template.i18n.week(),
    goog.ui.style.app.ButtonRenderer.getInstance());
  week.addClassName(goog.getCssName('goog-button-base-middle'));
  tb.getEast().addChild(week, true);
  this.getHandler().listen(week, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.week();
  });

  var month = new goog.ui.Button(bluemind.calendar.template.i18n.month(),
    goog.ui.style.app.ButtonRenderer.getInstance());
  month.addClassName(goog.getCssName('goog-button-base-middle'));
  tb.getEast().addChild(month, true);
  this.getHandler().listen(month, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.month();
  });

  var agenda = new goog.ui.Button(bluemind.calendar.template.i18n.list(),
    goog.ui.style.app.ButtonRenderer.getInstance());
  agenda.addClassName(goog.getCssName('goog-button-base-last'));
  tb.getEast().addChild(agenda, true);
  this.getHandler().listen(agenda, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.agenda();
  });

  tb.getEast().addChild(new goog.ui.ToolbarSeparator(), true);
  var othersButton = new goog.ui.Button(goog.dom.createDom('div',
    goog.getCssName('gears-calendar-icon') +
    ' ' + goog.getCssName('goog-inline-block')),
     goog.ui.style.app.ButtonRenderer.getInstance());
  tb.getEast().addChild(othersButton, true);

  var others = new goog.ui.PopupMenu();
  others.setToggleMode(true);

  var print = new goog.ui.MenuItem(
    bluemind.calendar.template.i18n.printAsPdf());
  this.getHandler().listen(print, goog.ui.Component.EventType.ACTION,
    function(e) {
      bluemind.calendar.Controller.getInstance().printDialog();
    });

  var refresh = new goog.ui.MenuItem(
    bluemind.calendar.template.i18n.refresh());
  this.getHandler().listen(refresh, goog.ui.Component.EventType.ACTION,
    function(e) {
      this.refresh();
    });

  var icsExport = new goog.ui.MenuItem(
    bluemind.calendar.template.i18n.icsExport());
  this.getHandler().listen(icsExport, goog.ui.Component.EventType.ACTION,
    function(e) {
      bluemind.calendar.Controller.getInstance().icsExportDialog();
    });

  var icsImport = new goog.ui.MenuItem(
    bluemind.calendar.template.i18n.icsImport());
  this.getHandler().listen(icsImport, goog.ui.Component.EventType.ACTION,
    function(e) {
      bluemind.calendar.Controller.getInstance().icsImportDialog();
    });

  others.addItem(print);
  others.addItem(refresh);
  others.addItem(icsExport);
  others.addItem(icsImport);
  others.render(document.body);
  others.attach(othersButton.getElement(),
      goog.positioning.Corner.BOTTOM_LEFT,
      goog.positioning.Corner.TOP_LEFT);
  others.setEnabled(bluemind.net.OnlineHandler.getInstance().isOnline());
  var title = bluemind.net.OnlineHandler.getInstance().isOnline() ?
    '' : bluemind.calendar.template.i18n.onlineOnly();
  others.getElement().title = title;
  others.getHandler().listen(bluemind.net.OnlineHandler.getInstance(),
    ['offline', 'online'], function() {
    var on = bluemind.net.OnlineHandler.getInstance().isOnline();
    this.setEnabled(on);
    var title = bluemind.net.OnlineHandler.getInstance().isOnline() ?
      '' : bluemind.calendar.template.i18n.onlineOnly();
    this.getElement().title = title;

  });
  switch (this.view_.getName()) {
    case 'days':
      if (bluemind.manager.getNbDays() == 1) {
        day.setEnabled(false);
      } else if (bluemind.manager.getNbDays() == 7) {
        week.setEnabled(false);
      }
      break;
    case 'month':
      month.setEnabled(false);
      break;
    case 'agenda':
      agenda.setEnabled(false);
      break;
  }
};

/**
 * Build day view
 *
 */
bluemind.calendar.View.prototype.day = function() {
  bluemind.hourMarkerTimer.stop();
  this.hideSidebar(false);
  if (this.view_) this.view_.dispose();
  this.view_ = new bluemind.calendar.view.Days();
  this.view_.displayDay(1);
  this.navToolbar();
  this.updateToolbar();
  this.lastView_ = this.day;
};

/**
 * Build week view
 *
 */
bluemind.calendar.View.prototype.week = function() {
  //bluemind.hourMarkerTimer.stop();
  this.hideSidebar(false);
  if (this.view_) this.view_.dispose();
  this.view_ = new bluemind.calendar.view.Days(this.manager_);
  this.view_.displayWeek();
  this.navToolbar();
  this.updateToolbar();
  this.lastView_ = this.week;
};

/**
 * Build month view
 *
 */
bluemind.calendar.View.prototype.month = function() {
  bluemind.hourMarkerTimer.stop();
  this.hideSidebar(false);
  if (this.view_) this.view_.dispose();
  this.view_ = new bluemind.calendar.view.Month();
  this.view_.display();
  this.navToolbar();
  this.updateToolbar();
  this.lastView_ = this.month;
};

/**
 * Build agenda view
 *
 */
bluemind.calendar.View.prototype.agenda = function() {
  bluemind.hourMarkerTimer.stop();
  this.hideSidebar(false);
  if (this.view_) this.view_.dispose();
  this.view_ = new bluemind.calendar.view.Agenda();
  this.view_.display();
  this.navToolbar();
  this.updateToolbar();
  this.lastView_ = this.agenda;
};

/**
 * Build pending events list
 *
 */
bluemind.calendar.View.prototype.pending = function() {
  bluemind.hourMarkerTimer.stop();
  this.hideSidebar(false);
  if (this.view_) this.view_.dispose();
  this.view_ = new bluemind.calendar.view.Pending();
  this.view_.display();
};

/**
 * Refresh
 */
bluemind.calendar.View.prototype.refresh = function() {
  bluemind.sync.SyncEngine.getInstance().execute();
};

/**
 * Event form
 *
 */
bluemind.calendar.View.prototype.newEventForm = function() {
  bluemind.hourMarkerTimer.stop();
  this.hideSidebar(true);
  if (this.view_) this.view_.dispose();
  this.view_ = new bluemind.calendar.view.Forms();
  this.view_.create();
};


/**
 * Event form
 * @param {bluemind.calendar.model.Event} evt Event to update;.
 */
bluemind.calendar.View.prototype.updateEventForm = function(evt) {
  bluemind.hourMarkerTimer.stop();
  this.hideSidebar(true);
  if (this.view_) this.view_.dispose();
  this.view_ = new bluemind.calendar.view.Forms();
  this.view_.update(evt);
};


/**
 * Today
 *
 */
bluemind.calendar.View.prototype.today = function() {
  this.view_.dispose();
  this.hideSidebar(false);
  this.manager_.setToday(new goog.date.Date());
  this.view_.displayToday();
  this.view_.display();
  this.updateToolbar();
};

/**
 * Prev period
 *
 */
bluemind.calendar.View.prototype.prev = function() {
  this.view_.dispose();
  this.view_.prev();
  this.view_.display();
  this.updateToolbar();
};

/**
 * Next period
 *
 */
bluemind.calendar.View.prototype.next = function() {
  this.view_.dispose();
  this.view_.next();
  this.view_.display();
  this.updateToolbar();
};

/**
 * Update toolbar title
 *
 */
bluemind.calendar.View.prototype.updateToolbar = function() {
 this.minical_.setDate(bluemind.manager.getCurrentDate());
  var container = goog.dom.getElement('dateRange');
  var start = this.view_.getStart();
  if (this.view_.getName() == 'month') {
    var df = new goog.i18n.DateTimeFormat('MMMM yyyy');
    start = bluemind.manager.getCurrentDate();
    goog.dom.setTextContent(container, df.format(start));
  } else if (this.view_.getName() == 'days') {
    var df = new goog.i18n.DateTimeFormat(
      goog.i18n.DateTimeFormat.Format.MEDIUM_DATE);
    if (this.manager_.getNbDays() > 1) {
      var end = this.view_.getEnd().clone();
      end.add(new goog.date.Interval(0, 0, -1));
      goog.dom.setTextContent(
        container, df.format(start) + ' - ' + df.format(end));
    } else {
      var df = new goog.i18n.DateTimeFormat(
        goog.i18n.DateTimeFormat.Format.FULL_DATE);
      goog.dom.setTextContent(container, df.format(start));
    }
  } else if (this.view_.getName() == 'agenda') {
    var df = new goog.i18n.DateTimeFormat(
      goog.i18n.DateTimeFormat.Format.FULL_DATE);
    goog.dom.setTextContent(container, df.format(start));
  }
};


/**
 * go to last calendar view.
 *
 */
bluemind.calendar.View.prototype.lastView = function() {
  this.lastView_();
};

/**
 * Show or hide the sidebar
 * @param {boolean} hide hide sidebar flag.
 */
bluemind.calendar.View.prototype.hideSidebar = function(hide) {
  if (hide) {
    goog.dom.classes.set(goog.dom.getElement('content'),
      goog.getCssName('content-full'));
  } else {
    goog.dom.classes.set(goog.dom.getElement('content'),
      goog.getCssName('content'));
  }
  goog.style.showElement(goog.dom.getElement('leftPanel'), !hide);
};

/**
 * Update hour marker
 */
bluemind.calendar.View.prototype.updateHourMarker = function() {
  var hourMarker = goog.dom.getElement('hour-marker');
  if (hourMarker) {
    var n = new bluemind.date.DateTime();
    goog.style.setStyle(hourMarker, 'top',
      n.getHours() * 42 + n.getMinutes() * (42 / 60) + 'px');
    var rightNow = goog.dom.getElement('today-hour-marker');
    if (rightNow) {
      goog.style.setStyle(rightNow, 'top',
        n.getHours() * 42 + n.getMinutes() * (42 / 60) + 'px');
    }
  }
};

/**
 *
 */
bluemind.calendar.View.prototype.getHandler = function() {
  return this.handler_ ||
         (this.handler_ = new goog.events.EventHandler(this));
};
  
