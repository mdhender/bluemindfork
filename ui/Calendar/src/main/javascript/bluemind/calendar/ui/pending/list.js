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
 * @fileoverview Waiting events list componnent.
 */

goog.provide('bluemind.calendar.ui.pending.List');

goog.require('bluemind.calendar.Controller');
goog.require('bluemind.calendar.event.template');
goog.require('bluemind.calendar.pending.template');
goog.require('bluemind.calendar.template.i18n');
goog.require('goog.date');
goog.require('goog.dom');
goog.require('goog.i18n.DateTimeFormat');
goog.require('goog.i18n.DateTimeFormat.Format');
goog.require('goog.soy');
goog.require('goog.ui.AnimatedZippy');
goog.require('goog.ui.Select');
goog.require('goog.ui.ToolbarSeparator');
goog.require('goog.ui.style.app.ButtonRenderer');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.pending.List = function(opt_domHelper) {
  goog.base(this, opt_domHelper);

  this.df_ = new goog.i18n.DateTimeFormat(
    goog.i18n.DateTimeFormat.Format.FULL_DATE);

  this.checkboxes_ = new Array();
  this.events = new Array();

  this.dateTimeHelper_ = bluemind.i18n.DateTimeHelper.getInstance();
};
goog.inherits(bluemind.calendar.ui.pending.List, goog.ui.Component);

/**
 * Date formatter
 *
 * @type {goog.i18n.DateTimeFormat}

 * @private
 */
bluemind.calendar.ui.pending.List.prototype.df_;

/**
 * date time helper
 * @type {bluemind.i18n.DateTimeHelper}
 * @private
 */
bluemind.calendar.ui.pending.List.prototype.dateTimeHelper_;

/**
 * Toolbar
 *
 * @type {bluemind.calendar.ui.pending.List.Toolbar}
 * @private
 */
bluemind.calendar.ui.pending.List.prototype.tb_;

/**
 * Checkboxes
 *
 * @type {Array}
 * @private
 */
bluemind.calendar.ui.pending.List.prototype.checkboxes_;

/**
 * Select all/none
 *
 * @type {goog.ui.Checkbox}
 * @private
 */
bluemind.calendar.ui.pending.List.prototype.rootCheckbox_;

/**
 * Selected events.
 *
 * @type {Array}
 * @private
 */
bluemind.calendar.ui.pending.List.prototype.selectedEvents_;

/**
 * @return {goog.ui.Checkbox} root checkbox.
 */
bluemind.calendar.ui.pending.List.prototype.getRootCheckbox = function() {
  return this.rootCheckbox_;
};

/**
 * @return {Array} selected events.
 */
bluemind.calendar.ui.pending.List.prototype.getSelectedEvents = function() {
  return this.selectedEvents_;
};

/**
 * @param {Array} sel selected events.
 */
bluemind.calendar.ui.pending.List.prototype.setSelectedEvents = function(sel) {
  this.selectedEvents_ = sel;
};

/**
 * @return {Array} checkboxes.
 */
bluemind.calendar.ui.pending.List.prototype.getCheckboxes = function() {
  return this.checkboxes_;
};

/**
 * @param {Array} checkboxes Checkboxes.
 */
bluemind.calendar.ui.pending.List.prototype.setCheckboxes =
  function(checkboxes) {
  this.checkboxes_ = checkboxes;
};

/**
 * @return {bluemind.calendar.ui.pending.List.Toolbar} toolbar.
 */
bluemind.calendar.ui.pending.List.prototype.getToolbar = function() {
  return this.tb_;
};

/**
 * Events
 *
 * @type {Array}
 */
bluemind.calendar.ui.pending.List.prototype.events;

/**
 * @return {Array} events.
 */
bluemind.calendar.ui.pending.List.prototype.getEvents = function() {
  return this.events;
};

/** @inheritDoc */
bluemind.calendar.ui.pending.List.prototype.createDom = function() {

  this.tb_ = new bluemind.calendar.ui.pending.List.Toolbar(this);

  var element = goog.soy.renderAsElement(
    bluemind.calendar.pending.template.list);
  this.setElementInternal(/** @type {Element} */ (element));
  goog.dom.getElement('viewContainer').innerHTML = '';
  goog.dom.appendChild(goog.dom.getElement('viewContainer'), element);

  // Toggle select all/none
  this.rootCheckbox_ = new goog.ui.Checkbox();
  this.rootCheckbox_.decorate(goog.dom.getElement('evt-root'));
  goog.events.listen(this.rootCheckbox_, goog.ui.Component.EventType.CHANGE,
    function() {
      var checked = this.rootCheckbox_.getChecked();
      for (var i = 0; i < this.checkboxes_.length; i++) {
        var cb = this.checkboxes_[i];
        cb.setChecked(checked);
        cb.dispatchEvent(goog.ui.Component.EventType.CHANGE);
      }
    },
    false, this);

  this.selectedEvents_ = new Array();
};

/**
 * Build an event
 * @param {bluemind.calendar.model.Event} evt Event.
 */
bluemind.calendar.ui.pending.List.prototype.add = function(evt) {

  var tr = new goog.dom.createDom('tr',
    {'class': 'calendar-' + evt.getCalendar()});
  goog.dom.classes.add(tr, goog.getCssName('calendar-item'));

  var tdSel = new goog.dom.createDom('td');
  var cbox = new goog.ui.Checkbox();
  cbox.setId('evt-' + evt.getId());
  cbox.render(tdSel);
  this.checkboxes_.push(cbox);

  goog.events.listen(cbox, goog.ui.Component.EventType.CHANGE, function() {
    if (cbox.getChecked()) {
      goog.array.insert(this.selectedEvents_, evt);
      goog.dom.classes.add(tr, goog.getCssName('selected'));
      this.tb_.setPartButtonsEnabled(true);
    } else {
      goog.array.remove(this.selectedEvents_, evt);
      goog.dom.classes.remove(tr, goog.getCssName('selected'));
      if (this.selectedEvents_.length < 1) {
        this.tb_.setPartButtonsEnabled(false);
      }
    }
  }, false, this);
  var tdCalendar = new goog.dom.createDom('td');
  goog.dom.classes.add(tdCalendar, goog.getCssName('calendar'));

  var klass = bluemind.manager.getCalendar(evt.getCalendar()).getClass();
  var calendar = new goog.dom.createDom('div');
  goog.dom.classes.add(calendar, bluemind.fixCssName(klass, false));
  goog.dom.appendChild(tdCalendar, calendar);

  var tdDate = new goog.dom.createDom('td',
    {'id': 'day' + evt.getDate().getDayOfYear(),
     'rowspan': 1},
    this.df_.format(evt.getDate()));
  goog.dom.classes.add(tdDate, goog.getCssName('date'));
  if (goog.date.isSameDay(evt.getDate())) {
    goog.dom.classes.add(tdDate, goog.getCssName('today'));
  }

  var time =
    this.dateTimeHelper_.formatTime(evt.getDate()) +
      ' - ' + this.dateTimeHelper_.formatTime(evt.getEnd());
  if (evt.isAllday()) {
    time = bluemind.calendar.template.i18n.allday();
  }
  var tdTime = new goog.dom.createDom('td', {}, time);
  goog.dom.classes.add(tdTime, goog.getCssName('time'));

  var tdDetail = new goog.dom.createDom('td',
    {'id': evt.getId() + '_' + evt.getDate().getDayOfYear()});
  goog.dom.classes.add(tdDetail, goog.getCssName('evtDetail'));

  var repeat = new goog.structs.Map();
  repeat.set('kind', evt.getRepeatKind());
  if (evt.getRepeatKind() != 'none') {
    repeat.set('freq', evt.getRepeatFreq());
    if (evt.getRepeatEnd()) {
      repeat.set('end', this.df_.format(evt.getRepeatEnd()));
    } else {
      repeat.set('end', null);
    }
    if (evt.getRepeatKind() == 'weekly') {
      var days = evt.getRepeatDays();
      var localizedDayList = new Array();
      for (var i = 0; i < days.length; i++) {
        if (days[i] == 1) {
          goog.array.insert(localizedDayList,
            goog.i18n.DateTimeSymbols.WEEKDAYS[i]);
        }
      }
      repeat.set('days', localizedDayList.join(', '));
    }
  }

  var data = {
    id: evt.getId(),
    calendar: evt.getCalendar(),
    title: evt.getTitle(),
    owner: evt.getOwnerDisplayName(),
    location: evt.getLocation(),
    description: evt.getDescription(),
    attendees: evt.getAttendees(),
    repeat: repeat.toObject(),
    isMeeting: evt.getAttendees().length > 1
  };

  var eventDetail = goog.soy.renderAsElement(
    bluemind.calendar.event.template.detail, data);

  goog.dom.appendChild(tdDetail, eventDetail);

  goog.dom.appendChild(goog.dom.getElement('events-list'), tr);
  goog.dom.appendChild(tr, tdSel);
  goog.dom.appendChild(tr, tdDate);
  goog.dom.appendChild(tr, tdCalendar);
  goog.dom.appendChild(tr, tdTime);
  goog.dom.appendChild(tr, tdDetail);

  var zippy = new goog.ui.AnimatedZippy(
    'event-detail-header-' + evt.getId()+'-'+evt.getCalendar(),
    'event-detail-content-' + evt.getId()+'-'+evt.getCalendar(),
    false);
};

/**
 * Empty event list dom.
 */
bluemind.calendar.ui.pending.List.prototype.createNoEventDom = function() {

  var back = new bluemind.calendar.ui.control.BackToCalendar();
  var tb = bluemind.ui.Toolbar.getInstance();
  tb.removeChildren();
  tb.getWest().addChild(back, true);

  var element = goog.soy.renderAsElement(
    bluemind.calendar.pending.template.emptyList);
  this.setElementInternal(/** @type {Element} */ (element));
  goog.dom.getElement('viewContainer').innerHTML = '';
  goog.dom.appendChild(goog.dom.getElement('viewContainer'), element);
};

/**
 * Pending event toolbar
 * @param {bluemind.calendar.ui.pending.List} list pending events list.
 * @constructor
 */
bluemind.calendar.ui.pending.List.Toolbar = function(list) {
  this.list_ = list;
  var back = new bluemind.calendar.ui.control.BackToCalendar();
  this.tb_ = bluemind.ui.Toolbar.getInstance();
  this.tb_.removeChildren();
  this.tb_.getWest().addChild(back, true);

  this.tb_.getWest().addChild(new goog.ui.ToolbarSeparator(), true);

  this.accept_ = new goog.ui.Button(bluemind.calendar.template.i18n.accept(),
      new goog.ui.style.app.ButtonRenderer.getInstance());
  this.tb_.getWest().addChild(this.accept_, true);
  goog.events.listen(this.accept_, goog.ui.Component.EventType.ACTION,
    function(e) {
    e.stopPropagation();
      bluemind.calendar.Controller.getInstance().setParticipations(
        this.selectedEvents_, 'ACCEPTED');
  }, false, list);

  this.tb_.getWest().addChild(new goog.ui.ToolbarSeparator(), true);

  this.decline_ = new goog.ui.Button(bluemind.calendar.template.i18n.decline(),
      new goog.ui.style.app.ButtonRenderer.getInstance());
  this.tb_.getWest().addChild(this.decline_, true);
  goog.events.listen(this.decline_, goog.ui.Component.EventType.ACTION,
    function(e) {
    e.stopPropagation();
      bluemind.calendar.Controller.getInstance().setParticipations(
        this.selectedEvents_, 'DECLINED');
  }, false, list);

  this.setPartButtonsEnabled(false);
};

/**
 * @param {bluemind.ui.Toolbar} this is toolbar.
 * @private
 */
bluemind.calendar.ui.pending.List.Toolbar.prototype.tb_;

/**
 * @param {goog.ui.Button} accept button.
 * @private
 */
bluemind.calendar.ui.pending.List.Toolbar.prototype.accept_;

/**
 * @param {goog.ui.Button} decline button.
 * @private
 */
bluemind.calendar.ui.pending.List.Toolbar.prototype.decline_;

/**
 * @param {goog.ui.Button} decline button.
 * @private
 */
bluemind.calendar.ui.pending.List.Toolbar.prototype.calendarFilter_;

/**
 * @param {bluemind.calendar.ui.pending.List} decline button.
 * @private
 */
bluemind.calendar.ui.pending.List.Toolbar.prototype.list_;

/**
 * Enabled participation button.
 * @param {boolean} enabled enabled buttons.
 */
bluemind.calendar.ui.pending.List.Toolbar.prototype.setPartButtonsEnabled =
  function(enabled) {
  this.accept_.setEnabled(enabled);
  this.decline_.setEnabled(enabled);
};

/**
 * Build calendar list
 * @param {Array} data calendar list.
 */
bluemind.calendar.ui.pending.List.Toolbar.prototype.buildCalendarFilter =
  function(data) {

  var keys = data.getKeys();
  if (keys.length > 1) {
    var filterButton = new goog.ui.Button(bluemind.calendar.template.i18n.filter(),
       goog.ui.style.app.ButtonRenderer.getInstance());
    this.tb_.getEast().addChild(filterButton, true);

    var items = new goog.ui.PopupMenu();
    items.setToggleMode(true);

    var item = new goog.ui.MenuItem(bluemind.calendar.template.i18n.all());
    goog.events.listen(item, goog.ui.Component.EventType.ACTION,
      function(e) {
          var events = this.list_.getEvents();
          this.list_.setSelectedEvents(new Array());
          this.list_.setCheckboxes(new Array());
          goog.dom.getElement('events-list').innerHTML = '';
          for (var i = 0; i < events.length; i++) {
            this.list_.add(events[i]);
          }
          this.list_.getRootCheckbox().setChecked(false);
          this.setPartButtonsEnabled(false);
      }, false, this);
    items.addItem(item);

    goog.array.forEach(keys, function(k) {
      var item = new goog.ui.MenuItem(data.get(k));
      goog.events.listen(item, goog.ui.Component.EventType.ACTION,
        function(e) {
            var calendarId = k;
            var events = this.list_.getEvents();
            this.list_.setSelectedEvents(new Array());
            this.list_.setCheckboxes(new Array());
            goog.dom.getElement('events-list').innerHTML = '';
            for (var i = 0; i < events.length; i++) {
              var evt = events[i];
              if (calendarId == 0 || evt.getCalendar() == calendarId) {
                this.list_.add(evt);
              }
            }
            this.list_.getRootCheckbox().setChecked(false);
            this.setPartButtonsEnabled(false);
        }, false, this);
      items.addItem(item);
    }, this);
    items.render(document.body);
    items.attach(filterButton.getElement(),
      goog.positioning.Corner.BOTTOM_LEFT,
      goog.positioning.Corner.TOP_LEFT);
  }
};

