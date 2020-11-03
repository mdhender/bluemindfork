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
 * @fileoverview Minicalendar componnent.
 */

goog.provide("net.bluemind.calendar.list.PendingEventsView");
goog.provide("net.bluemind.calendar.list.PendingEventsView.EventType");

goog.require("goog.Uri");
goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.events");
goog.require("goog.soy");
goog.require("goog.dom.ViewportSizeMonitor");
goog.require("goog.dom.classlist");
goog.require("goog.events.Event");
goog.require("goog.events.EventType");
goog.require("goog.ui.Button");
goog.require("goog.ui.Checkbox");
goog.require("goog.ui.Component");
goog.require("goog.ui.Control");
goog.require("goog.ui.Button");
goog.require("goog.ui.Toolbar");
goog.require("goog.ui.ToolbarSeparator");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.style.app.ButtonRenderer");
goog.require("net.bluemind.calendar.day.templates");
goog.require("net.bluemind.calendar.list.templates");// FIXME - unresolved
// required symbol
goog.require("net.bluemind.calendar.day.ui.ReplyInvitation");

/**
 * View class for Calendar days view.
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.list.PendingEventsView = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.ctx = ctx;

  var child = new goog.ui.Component();
  child.setId('list');
  this.addChild(child, true);
  var element = goog.soy.renderAsElement(net.bluemind.calendar.list.templates.list);

  this.getDomHelper().appendChild(this.getChild('list').getElement(), element);
};

goog.inherits(net.bluemind.calendar.list.PendingEventsView, goog.ui.Component);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.list.PendingEventsView.prototype.ctx;

/**
 * @type {goog.dom.ViewportSizeMonitor}
 */
net.bluemind.calendar.list.PendingEventsView.prototype.sizeMonitor_

/** @meaning calendar.list.allDay */
net.bluemind.calendar.list.PendingEventsView.MSG_ALLDAY = goog.getMsg('All day');

/** @override */
net.bluemind.calendar.list.PendingEventsView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  goog.dom.classlist.add(this.getElement(), goog.getCssName('list-view'));
};

/** @override */
net.bluemind.calendar.list.PendingEventsView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.sizeMonitor_ = new goog.dom.ViewportSizeMonitor();
  this.getHandler().listen(this.sizeMonitor_, goog.events.EventType.RESIZE, this.resize_);
};

/**
 * Resize grid
 * 
 * @param {goog.events.Event=} opt_evt
 * @private
 */
net.bluemind.calendar.list.PendingEventsView.prototype.resize_ = function(opt_evt) {
  var grid = this.getElement();
  var size = this.sizeMonitor_.getSize();
  var height = size.height - grid.offsetTop - 3;
  grid.style.height = height + 'px';
};
/**
 * Refresh view
 */
net.bluemind.calendar.list.PendingEventsView.prototype.refresh = function() {
  // this.getHandler().removeAll();
  this.getChild('list').removeChildren(true);
  this.draw_();
};

/**
 * Draw event list
 */
net.bluemind.calendar.list.PendingEventsView.prototype.draw_ = function() {
  if (this.isInDocument()) {
    var events = this.getModel() || [];
    this.checkboxes_ = [];
    var table = this.getDomHelper().getElementByClass(goog.getCssName('pendinglist'));

    var rows = new goog.ui.Component();
    rows.createDom = function() {
      this.element_ = this.getDomHelper().createDom('tbody');
    }
    rows.setId('rows');
    this.getChild('list').addChild(rows);
    rows.render(table);
    var toArr = [];
    for (var i = 0; i < events.length; i++) {
      var event = events[i];
      if (!this.calendarSynced(event, rows)) {
        goog.array.insert(toArr, event.calendar);
      }
    }

    var missingCalendars = toArr;
    if (missingCalendars.length > 0) {
      this.loadMissingCalendars(missingCalendars);
      return;
    }

    var groupedByCalendar = {};

    for (var i = 0; i < events.length; i++) {
      var event = events[i];
      var cal = event.calendar;
      var evts;
      if (cal in groupedByCalendar) {
        evts = groupedByCalendar[cal];
      } else {
        evts = [];
      }
      evts.push(event);
      groupedByCalendar[cal] = evts;
    }

    for ( var cal in groupedByCalendar) {
      if (Object.prototype.hasOwnProperty.call(groupedByCalendar, cal)) {
        var events = groupedByCalendar[cal];
        var eventCalendar = goog.array.find(this.calendars, function(calendar) {
          return calendar.uid == cal;
        });
        this.drawCalendarHeader(eventCalendar, rows);
        for (var i = 0; i < events.length; i++) {
          var event = events[i];
          this.drawEvent(eventCalendar, event, rows);
        }
      }
    }

  }
};

net.bluemind.calendar.list.PendingEventsView.prototype.loadMissingCalendars = function(missingCalendars) {
  this.ctx.service('folders').getFoldersRemote(null, missingCalendars).then(function(folders) {
    return this.ctx.service('calendarsMgmt').addCalendars(folders);
  }, null, this).then(function() {
    this.ctx.helper('url').reload();
  }, function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
}

net.bluemind.calendar.list.PendingEventsView.prototype.calendarSynced = function(event, rows) {

  var cal = goog.array.find(this.calendars, function(calendar) {
    return calendar.uid == event.calendar;
  });

  if (!cal) {
    console.log("cal ", event.uid, "not synchronized");
    return false;
  }

  return true;
}

net.bluemind.calendar.list.PendingEventsView.prototype.drawCalendarHeader = function(cal, rows) {
  var dom = this.getDomHelper();

  var parent = rows.getElement();
  var tr = dom.createDom('tr');

  var headerDiv = dom.createDom('div', {
    'style' : 'display:table'
  }, "");

  var icon = dom
      .createDom(
          'img',
          {
            'id' : 'ico' + cal.uid,
            'style' : 'border:1px solid #ccc; height: 24px;',
            'src' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAHzAAAB8wBK+h4IgAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAKMSURBVEiJtVXBShtRFD33vfvuZKItY9WEQBEtuEgWIoWi6N6VH+BX+BFu3fQD1K0rdxZciSio0FVdzEaCoaAm0TSQtE2N8b0ujGIkMxFCzm7uveedd+69M0NbW1tflVLfMQBYa7+wMebH8vLyt0EI7O7uChtjxPO8oUEIeJ5n2BgjrVarp8Dm5ub48fFxxjlHs7OzpdXV1WIvjlJKWCklSqlkXOHa2tqn/f39z0/PhUIBl5eX4fr6ethTgJnFWhvrIAzDnO/78jKWz+ezzWbzJzO7KJ6IGGZmUUpFCpydnQ01m81x3/dfp7yDg4OPS0tLv2IdEFGsg9vb2w+JREK65Wq12oi19i6Kq7UWZmYTNwMR8UWkq0AikfDjuFprw1prcc5FOkin0ywiXrfc2NgYx3GVUsIAPCKKLJqensbk5OTd1dXVu5fxIAj+zc/P3wOI5BLRY4ucc7FrOjU11by5uelo08TERK0Xj5kNE1Gsg3ahb4zpaJOI+L14RCQMIHYGAKCUShpjOhwQkf8GnrDW2kTdJJ/P+0dHR6OFQmHYGNORu7i4SG1sbAwvLi5WcrncnwiNx0/F616GYZg8OTkZub6+9gFAa92VXSwWZWdnJzg8PLybm5urzszM/FZKvXRgmIgE7U1oNBpqe3s7XS6XPQBg5rgOPKNarXp7e3vvT09P71dWVkpBELSeBZRSBgABwPn5ebJSqQxH3bgX6vW6CcNwdGFhod4OERORB8ADAGutz8xdX6q3wlqbBGDbDlpPLTIAQEQJrXVfAlrrBwCufd5fBiAAkgCQzWYplUrd9yMQBIFGe6ZE1GKl1LOAiCCTyfRzfgeIqNGxRQNAnUul0oNzLvKn0Q/K5bL7D9ddzgDKtkW0AAAAAElFTkSuQmCC'
          }, "");

  var iconDiv = dom.createDom('div', {}, icon);
  var nameDiv = dom.createDom('div', {
    'style' : 'display:table-cell;vertical-align: middle;padding-left:15px;font-size:1.2em'
  }, cal.name);

  var tdCal = dom.createDom('td', {
    'colspan' : 5
  }, headerDiv);

  goog.dom.classlist.add(tdCal, goog.getCssName('calheader'));

  dom.appendChild(headerDiv, iconDiv);
  dom.appendChild(headerDiv, nameDiv);
  dom.appendChild(parent, tr);
  dom.appendChild(tr, tdCal);

  var dir = new net.bluemind.directory.api.DirectoryClient(this.ctx.rpc, '', this.ctx.user['domainUid']);
  dir.getEntryIcon(cal.owner).then(function(photo) {
    if (bluemind.net.OnlineHandler.getInstance().isOnline()) {
      if (photo) {
        photo = "data:image/png;base64," + photo;
        goog.dom.getElement('ico' + cal.uid).src = photo;
      }
    }
  });

}

/**
 * Draw an event for one line (date) of the planning view
 * 
 * @param {Object} event Event to draw
 * @param {Element} parent
 */
net.bluemind.calendar.list.PendingEventsView.prototype.drawEvent = function(cal, event, rows) {

  var dom = this.getDomHelper();

  var parent = rows.getElement();
  var tr = dom.createDom('tr');

  var tdDate = dom.createDom('td', {}, this.dateFormat.format(event.dtstart));

  goog.dom.classlist.add(tdDate, goog.getCssName('date'));

  if (event.states.allday) {
    var time = net.bluemind.calendar.list.PendingEventsView.MSG_ALLDAY;
  } else {
    var time = this.timeFormat.format(event.dtstart) + ' - ' + this.timeFormat.format(event.dtend);
  }

  var tdTime = dom.createDom('td', {}, time);
  goog.dom.classlist.add(tdTime, goog.getCssName('time'));

  var tdCalendar = dom.createDom('td');

  var calendar = dom.createDom('div');
  goog.dom.classlist.add(calendar, goog.getCssName('calendar'));
  calendar.style.backgroundColor = cal.color.background;

  dom.appendChild(tdCalendar, calendar);
  var tdDetail = dom.createDom('td', {
    'id' : event.uid
  });
  goog.dom.classlist.add(tdDetail, goog.getCssName('evtDetail'));

  var detail = event.summary;
  if (event.location) {
    detail += ', ' + event.location;
  }
  var evtDetail = dom.createDom('a', {}, detail);
  goog.dom.classlist.add(evtDetail, goog.getCssName('detail'));

  dom.appendChild(tdDetail, evtDetail);

  if (event.states.meeting) {
    var meeting = dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-users') ]);
    dom.appendChild(tdDetail, meeting);
  }

  if (event.states.private_) {
    var priv = dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-lock') ]);
    dom.appendChild(tdDetail, priv);
  }

  if (event.states.repeat) {
    var repeat = dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-repeat') ]);
    dom.appendChild(tdDetail, repeat);
  }

  goog.array.forEach(event.tags, function(tag) {
    var tag = dom.createDom('div', {
      'title' : tag.label,
      'style' : 'background-color: #' + tag.color
    });
    dom.appendChild(evtDetail, tag);
  });
  var fn = goog.partial(this.handleClick_, event);
  this.getHandler().listen(evtDetail, goog.events.EventType.CLICK, fn);

  dom.appendChild(parent, tr);
  dom.appendChild(tr, tdDate);

  dom.appendChild(tr, tdTime);
  dom.appendChild(tr, tdCalendar);
  dom.appendChild(tr, tdDetail);

  var tdActions = dom.createDom('td');
  dom.appendChild(tr, tdActions);

  var invitButtons = new net.bluemind.calendar.day.ui.ReplyInvitation();
  invitButtons.setModel(event);
  rows.addChild(invitButtons);
  invitButtons.decorate(tdActions);
};

/**
 * @param {Object} event
 * @param {goog.event.Event} e
 */
net.bluemind.calendar.list.PendingEventsView.prototype.handleClick_ = function(event, e) {
  // FIXME : should be in presenter not in view
  var loc = goog.global['location'];
  var uri = new goog.Uri('/vevent/');
  uri.getQueryData().set('uid', event.uid);
  if (event.recurrenceId) {
    uri.getQueryData().set('recurrence-id', event.recurrenceId.toIsoString(true, true))
  }
  uri.getQueryData().set('container', event.calendar);
  loc.hash = uri.toString();
};

/** @enum {string} */
net.bluemind.calendar.list.PendingEventsView.EventType = {
  BACK : goog.events.getUniqueId('back')

};
