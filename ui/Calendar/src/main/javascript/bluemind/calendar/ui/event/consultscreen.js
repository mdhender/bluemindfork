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
 * @fileoverview Events consult componnent.
 */

goog.provide('bluemind.calendar.ui.event.ConsultScreen');

goog.require('bluemind.calendar.event.template');
goog.require('bluemind.calendar.template');
goog.require('bluemind.calendar.ui.control.BackToCalendar');
goog.require('bluemind.i18n.DateTimeHelper');
goog.require('bluemind.ui.Toolbar');
goog.require('goog.date');
goog.require('goog.events.EventHandler');
goog.require('goog.i18n.DateTimeFormat');
goog.require('goog.i18n.DateTimeFormat.Format');
goog.require('goog.soy');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.ConsultScreen = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.handler_ = new goog.events.EventHandler(this);
  this.df_ = new goog.i18n.DateTimeFormat(
    goog.i18n.DateTimeFormat.Format.FULL_DATE);

};
goog.inherits(bluemind.calendar.ui.event.ConsultScreen, goog.ui.Component);

/**
 * Date formatter
 *
 * @type {goog.i18n.DateTimeFormat}

 * @private
 */
bluemind.calendar.ui.event.ConsultScreen.prototype.df_;

/** @inheritDoc */
bluemind.calendar.ui.event.ConsultScreen.prototype.createDom = function() {
  var evt = this.getModel();
  var date = this.df_.format(evt.getDate()) + ', ' +
    bluemind.i18n.DateTimeHelper.getInstance().formatTime(evt.getDate());

  if (goog.date.isSameDay(evt.getDate(), evt.getEnd())) {
    date += ' - ' +
      bluemind.i18n.DateTimeHelper.getInstance().formatTime(evt.getEnd());
  } else {
    date += ' - ' + this.df_.format(evt.getEnd()) + ', ' +
    bluemind.i18n.DateTimeHelper.getInstance().formatTime(evt.getEnd());
  }

  var attendees = new Array();
  var a;
  goog.object.forEach(evt.getAttendees(), function(att) {
    a = new goog.structs.Map();
    var displayname = (att['displayName']) ? att['displayName'] : att['email'];
    a.set('displayName', displayname);
    a.set('participation', att['participation']);
    a.set('picture', att['picture']);
    a.set('type', att['type']);
    a.set('role', att['role']);
    a.set('email', att['email']);
    goog.array.insert(attendees, a.toObject());
  });

  attendees.sort(function(a1, a2) {
    return a1['displayName'] > a2['displayName'];
  });

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

  var canWrite = goog.array.contains(bluemind.writableCalendars,
    evt.getCalendar().toString()) || goog.array.contains(bluemind.manageableCalendars,
    evt.getCalendar().toString());
  var reminder = {};
  if (canWrite) {
    var alerts = evt.getAttendeesAlerts();
    var seconds = null;
    if (alerts != null) {
      seconds = alerts[evt.getCalendar()];
    }
    if (seconds == null) {
      reminder.duration = -1.
      reminder.unit = 'seconds';
    } else {
      if (!seconds) {
        reminder.duration = 10;
        reminder.unit = 'minutes';
      } else if (seconds % 60 != 0) {
        reminder.duration = seconds;
        reminder.unit = 'seconds';
      } else if (seconds % 3600 != 0) {
        reminder.duration = seconds / 60;
        reminder.unit = 'minutes';
      } else if (seconds % 86400 != 0) {
        reminder.duration = seconds / 3600;
        reminder.unit = 'hours';
      } else {
        reminder.duration = seconds / 86400;
        reminder.unit = 'days';
      }
    }
  } else {
    reminder = null;
  }

  var tags = [];
  goog.iter.forEach(evt.getTags(), function(tag) {
    tags.push(tag.serialize());
  }, this);

  var data = {
    title: evt.getTitle(),
    owner: evt.getOwnerDisplayName(),
    location: evt.getLocation(),
    allday: evt.isAllday(),
    date: date,
    calendar: bluemind.manager.getCalendar(evt.getCalendar()).getLabel(),
    desc: evt.getDescription(),
    attendees: attendees,
    reminder: reminder,
    repeat: repeat.toObject(),
    tags: tags,
    sid: bluemind.me['sid']
  };

  var element = soy.renderAsFragment(
    bluemind.calendar.event.template.consultScreen, data);

  this.setElementInternal(/** @type {Element} */ (element));
  goog.dom.getElement('viewContainer').innerHTML = '';
  goog.dom.appendChild(goog.dom.getElement('viewContainer'), element);

  var back = new bluemind.calendar.ui.control.BackToCalendar();
  var tb = bluemind.ui.Toolbar.getInstance();
  tb.removeChildren();

  tb.getWest().addChild(back, true);

  if (reminder) {

    tb.getWest().addChild(new goog.ui.ToolbarSeparator(), true);

    var save = new goog.ui.Button(bluemind.calendar.template.i18n.save(),
      bluemind.ui.style.PrimaryActionButtonRenderer.getInstance());
    tb.getWest().addChild(save, true);
    goog.events.listen(save, goog.ui.Component.EventType.ACTION, function(e) {
      this.storeReminder_();
    }, false, this);

    tb.getWest().addChild(new goog.ui.ToolbarSeparator(), true);

    var discard = new goog.ui.Button(bluemind.calendar.template.i18n.discard(),
      goog.ui.style.app.ButtonRenderer.getInstance());
    tb.getWest().addChild(discard, true);
    goog.events.listen(discard, goog.ui.Component.EventType.ACTION,
      function(e) {
      e.stopPropagation();
      bluemind.view.lastView();
    }, false, bluemind.view);

    this.handler_.listen(goog.dom.getElement('bm-ui-consult-add-reminder'),
      goog.events.EventType.CLICK, this.addReminder_);
    this.handler_.listen(goog.dom.getElement('bm-ui-consult-no-reminder'),
      goog.events.EventType.CLICK, this.removeReminder_);

    if (reminder.unit == 'seconds' && reminder.duration == -1) {
      element = goog.dom.getElement('bm-ui-consult-reminder-block');
      goog.style.showElement(element, false);
    } else {
      element = goog.dom.getElement('bm-ui-consult-no-reminder-block');
      goog.style.showElement(element, false);
    }
  }
  bluemind.resize();
};


/**
 * Show reminder fields
 * @param {goog.events.BrowserEvent} e Event.
 * @private
 */
bluemind.calendar.ui.event.ConsultScreen.prototype.addReminder_ = function(e) {
  goog.dom.forms.setValue(goog.dom.getElement('bm-ui-consult-reminder'), 10);
  goog.dom.forms.setValue(
    goog.dom.getElement('bm-ui-consult-reminder-unit'), '60');
  goog.style.showElement(
    goog.dom.getElement('bm-ui-consult-reminder-block'), true);
  goog.style.showElement(
    goog.dom.getElement('bm-ui-consult-no-reminder-block'), false);
};


/**
 * hide reminder fields
 * @param {goog.events.BrowserEvent} e Event.
 * @private
 */
bluemind.calendar.ui.event.ConsultScreen.prototype.removeReminder_ =
  function(e) {
  goog.dom.forms.setValue(goog.dom.getElement('bm-ui-consult-reminder'), '-1');
  goog.dom.forms.setValue(
    goog.dom.getElement('bm-ui-consult-reminder-unit'), 'seconds');
  goog.style.showElement(
    goog.dom.getElement('bm-ui-consult-reminder-block'), false);
  goog.style.showElement(
    goog.dom.getElement('bm-ui-consult-no-reminder-block'), true);
};

/**
 * Store reminder
 * @private
 */
bluemind.calendar.ui.event.ConsultScreen.prototype.storeReminder_ = function() {
  var data = goog.dom.forms.getFormDataMap(
    goog.dom.getElement('bm-ui-consult-form'));
  var duration = data.get('reminderduration') * data.get('reminderunit');
  var calendar = bluemind.manager.getCalendar(this.getModel().getCalendar());
  if (data.get('reminderunit') && duration > -1) {
    bluemind.calendar.Controller.getInstance().setAlert(
      this.getModel(), calendar.getOwnerId(), duration);
  } else {
    bluemind.calendar.Controller.getInstance().removeAlert(
      this.getModel(), calendar.getOwnerId());
  }
};

