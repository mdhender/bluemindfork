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

goog.provide("net.bluemind.calendar.list.PendingCountersView");
goog.provide("net.bluemind.calendar.list.PendingCountersView.EventType");

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
goog.require("bluemind.ui.style.ImportantActionButtonRenderer");
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
net.bluemind.calendar.list.PendingCountersView = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.ctx = ctx;

  var child = new goog.ui.Component();
  child.setId('counter-list');
  this.addChild(child, true);
  var element = goog.soy.renderAsElement(net.bluemind.calendar.list.templates.counterlist);

  this.getDomHelper().appendChild(this.getChild('counter-list').getElement(), element);
};

goog.inherits(net.bluemind.calendar.list.PendingCountersView, goog.ui.Component);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.list.PendingCountersView.prototype.ctx;

/**
 * @type {goog.dom.ViewportSizeMonitor}
 */
net.bluemind.calendar.list.PendingCountersView.prototype.sizeMonitor_

/** @meaning calendar.list.allDay */
net.bluemind.calendar.list.PendingCountersView.MSG_ALLDAY = goog.getMsg('All day');

/** @meaning calendar.counter.accept */
net.bluemind.calendar.list.PendingCountersView.MSG_ACCEPT = goog.getMsg('Accept');

/** @meaning calendar.counter.refuse */
net.bluemind.calendar.list.PendingCountersView.MSG_REFUSE = goog.getMsg('Reject');

/** @override */
net.bluemind.calendar.list.PendingCountersView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  goog.dom.classlist.add(this.getElement(), goog.getCssName('list-view'));
};

/** @override */
net.bluemind.calendar.list.PendingCountersView.prototype.enterDocument = function() {
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
net.bluemind.calendar.list.PendingCountersView.prototype.resize_ = function(opt_evt) {
  var grid = this.getElement();
  var size = this.sizeMonitor_.getSize();
  var height = size.height - grid.offsetTop - 3;
  grid.style.height = height + 'px';
};
/**
 * Refresh view
 */
net.bluemind.calendar.list.PendingCountersView.prototype.refresh = function() {
  this.getChild('counter-list').removeChildren(true);
  this.draw_();
};

/**
 * Draw event list
 */
net.bluemind.calendar.list.PendingCountersView.prototype.draw_ = function() {
  if (this.isInDocument()) {
    var events = this.getModel() || [];
    this.checkboxes_ = [];
    var table = this.getDomHelper().getElementByClass(goog.getCssName('counter-pendinglist'));

    var rows = new goog.ui.Component();
    rows.createDom = function() {
      this.element_ = this.getDomHelper().createDom('tbody');
    }
    rows.setId('rows');
    this.getChild('counter-list').addChild(rows);
    rows.render(table);
    var toArr = [];
    for (var i = 0; i < events.length; i++) {
      var event = events[i];
      if (!this.calendarSynced(event)) {
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
          var counters = new Map(); 
          goog.array.forEach(event.counters, function(counter) {
            var rec = counter.counter.recurrenceId;
            if (!rec) {
              rec = 'main';
            } else {
              var dateAsString = this.dateFormat.format(rec);
              if (rec.date.getHours()){
                var timeAsString = this.timeFormat.format(rec);
              } else {
                timeAsString = "";
              }
              rec = dateAsString+timeAsString;
            }
            if (counters.has(rec)){
              var values = counters.get(rec);
            } else {
              values = [];
            }
            values.push(counter);
            counters.set(rec, values);
          }, this);
          if (counters.has('main')){
            var isReccurent = event.main.rrule;
            var separate = event.counters.length == 1;
            this.drawEvent(eventCalendar, event, counters.get('main'), true, isReccurent, rows, separate);
          }
          var that = this;
          var index = 0;
          var len = counters.has('main') ? counters.size -1 : counters.size;
          counters.forEach(function(value, key, map) {
            if (key != 'main'){
              var separate = index == len-1;
              that.drawEvent(eventCalendar, event, counters.get(key), false, false, rows, separate);
            }
            index++;
          });
        }
      }
    }

  }
};

net.bluemind.calendar.list.PendingCountersView.prototype.loadMissingCalendars = function(missingCalendars) {
  this.ctx.service('folders').getFoldersRemote(null, missingCalendars).then(function(folders) {
    return this.ctx.service('calendarsMgmt').addCalendars(folders);
  }, null, this).then(function() {
    this.ctx.helper('url').reload();
  }, function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
}

net.bluemind.calendar.list.PendingCountersView.prototype.calendarSynced = function(event) {

  var cal = goog.array.find(this.calendars, function(calendar) {
    return calendar.uid == event.calendar;
  });

  if (!cal) {
    console.log("cal ", event.uid, "not synchronized");
    return false;
  }

  return true;
}

net.bluemind.calendar.list.PendingCountersView.prototype.drawCalendarHeader = function(cal, rows) {
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
  goog.dom.classlist.add(tr, goog.getCssName('separator'));

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
 * Draw an event for one line (date) of the counter view
 * 
 * @param {Object} event Event to draw
 * @param {Element} parent
 */
net.bluemind.calendar.list.PendingCountersView.prototype.drawEvent = function(cal, event, counters, isMain, isRecurrent, rows, separate) {
  var dom = this.getDomHelper();
  var parent = rows.getElement();

  if (isMain){
    var currentEvent = event.main;
  } else {
    currentEvent = goog.array.filter(event.occurrences, function(occ){
      return goog.date.Date.compare(occ.dtstart, counters[0].counter.recurrenceId) == 0;
    })[0];
  }

  var tr = dom.createDom('tr');

  var tdCalendar = dom.createDom('td');
  tdCalendar.colSpan = "2";
  var calendar = dom.createDom('div');
  goog.dom.classlist.add(calendar, goog.getCssName('calendar'));
  goog.dom.classlist.add(calendar, goog.getCssName('counter-calendar'));
  calendar.style.backgroundColor = cal.color.background;
  dom.appendChild(tdCalendar, calendar);

  var spanTitle = dom.createDom('span', {}, currentEvent.summary);
  goog.dom.classlist.add(spanTitle, goog.getCssName('counter-summary'));
  dom.appendChild(tdCalendar, spanTitle);

  var fn = goog.partial(this.handleClick_, event);
  this.getHandler().listen(spanTitle, goog.events.EventType.CLICK, fn);

  if (!isRecurrent){
    var tdDate = dom.createDom('td', {}, this.dateFormat.format(currentEvent.dtstart));
  } else {
    var repeat = dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-repeat') ]);
    var da = dom.createDom('span', {}, "  "+this.dateFormat.format(currentEvent.dtstart));
    var tdDate = dom.createDom('td', {}, repeat);
    dom.appendChild(tdDate, da);
  }
  goog.dom.classlist.add(tdDate, goog.getCssName('date'));
  if (!currentEvent.dtend.getHours()) {
    var time = net.bluemind.calendar.list.PendingCountersView.MSG_ALLDAY;
  } else {
    time = this.timeFormat.format(currentEvent.dtstart) + ' - ' + this.timeFormat.format(currentEvent.dtend);
  }
  var tdTime = dom.createDom('td', {}, time);
  goog.dom.classlist.add(tdTime, goog.getCssName('time'));

  /** @meaning calendar.counter.rejectall */
  var MSG_SAVE = goog.getMsg('Reject all');
  var button = new goog.ui.Button(MSG_SAVE, bluemind.ui.style.ImportantActionButtonRenderer.getInstance());
  var tdReject = dom.createDom('td', {}, "");
  button.render(tdReject);
  goog.dom.classlist.add(tdReject, goog.getCssName('buttontd'));
  var fnRejectAll = goog.partial(this.dispatchReject_, counters);
  this.getHandler().listen(tdReject, goog.events.EventType.CLICK, fnRejectAll);

  dom.appendChild(tr, tdCalendar);
  dom.appendChild(tr, tdDate);
  dom.appendChild(tr, tdTime);
  dom.appendChild(tr, tdReject);
  dom.appendChild(parent, tr);

  goog.array.sort(counters, function(r1, r2){
    if (!r1.counter.recurrenceId){
      return -1;
    }
    if (!r2.counter.recurrenceId){
      return 1;
    }
    return goog.date.Date.compare(r1.counter.recurrenceId, r2.counter.recurrenceId);
  });

  for (var i = 0; i < counters.length; i++) {
    var counter = counters[i];
    this.drawCounter(counter, parent, event, currentEvent, i, counters.length, isRecurrent, dom, separate);
  }
  
};

net.bluemind.calendar.list.PendingCountersView.prototype.drawCounter = function(counter, parent, event, currentOccurrence, index, len, isRecurrent, dom, separate){
    var tr = dom.createDom('tr');
    if (separate && index == event.counters.length-1){
      goog.dom.classlist.add(tr, goog.getCssName('separator'));
    }

    var tdOriginator = dom.createDom('td', {}, counter.originator.commonName);
    goog.dom.classlist.add(tdOriginator, goog.getCssName('counter-cn'));

    var tdCol2 = dom.createDom('td', {}, "");
    var refDate = this.dateFormat.format(currentOccurrence.dtstart);
    if (!currentOccurrence.dtstart.getHours()) {
      refBegin = net.bluemind.calendar.list.PendingCountersView.MSG_ALLDAY;
    } else {
      var refBegin = this.timeFormat.format(currentOccurrence.dtstart);
    }

    var counterDate = this.dateFormat.format(counter.counter.dtstart);
    if (!isRecurrent){
      var tdDate = dom.createDom('td', {}, counterDate);
    } else {
      var repeat = dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-repeat') ]);
      var da = dom.createDom('span', {}, "  " + counterDate);
      tdDate = dom.createDom('td', {}, repeat);
      dom.appendChild(tdDate, da);
    }
    if (counterDate != refDate){
      goog.dom.classlist.add(tdDate, goog.getCssName('counter-modified'));
    }
    if (!counter.counter.dtstart.date.getHours()) {
      var counterBegin = net.bluemind.calendar.list.PendingCountersView.MSG_ALLDAY;
      var time = counterBegin;
    } else {
      var counterBegin = this.timeFormat.format(counter.counter.dtstart); 
      var time = counterBegin + ' - ' + this.timeFormat.format(counter.counter.dtend);
    }
    var tdTime = dom.createDom('td', {}, time);
    if (refBegin != counterBegin){
      goog.dom.classlist.add(tdTime, goog.getCssName('counter-modified'));
    }
  
    var actionDiv = dom.createDom('div');
    var accept = dom.createDom('span', [ goog.getCssName('counter-accept'), goog.getCssName('fa'), goog.getCssName('fa-check') ]);
    accept.title = net.bluemind.calendar.list.PendingCountersView.MSG_ACCEPT;
    var fnAccept = goog.partial(this.dispatchAccept_, counter);
    this.getHandler().listen(accept, goog.events.EventType.CLICK, fnAccept);
  
    var refuse = dom.createDom('span', [ goog.getCssName('counter-refuse'), goog.getCssName('fa'), goog.getCssName('fa-times') ]);
    var fnRefuse = goog.partial(this.dispatchReject_, [counter]);
    refuse.title = net.bluemind.calendar.list.PendingCountersView.MSG_REFUSE;
    this.getHandler().listen(refuse, goog.events.EventType.CLICK, fnRefuse);

    dom.appendChild(actionDiv, accept);
    dom.appendChild(actionDiv, refuse);
    var tdActions = dom.createDom('td', {}, actionDiv);
    goog.dom.classlist.add(tdActions, goog.getCssName('buttontd'));
    goog.dom.classlist.add(tdActions, goog.getCssName('actionbuttontd'));
  
    dom.appendChild(parent, tr);
    dom.appendChild(tr, tdOriginator);
    dom.appendChild(tr, tdCol2);
    dom.appendChild(tr, tdDate);
    dom.appendChild(tr, tdTime);
    dom.appendChild(tr, tdActions);
}

net.bluemind.calendar.list.PendingCountersView.prototype.dispatchAccept_ = function(counter, e) {
  var series = goog.array.filter(this.getModel(), function(vevent){
    return vevent.uid == counter.counter.uid;
  })[0];
  if (counter.counter.recurrenceId){
    var adaptor = new net.bluemind.calendar.vevent.VEventSeriesAdaptor(this.ctx);
    var model = adaptor.getOccurrence(counter.counter.recurrenceId, series);
    model.states.main = false;
  } else {
    var model = series.main;
    model.states.main = true;
  }
  model.dtstart = counter.counter.dtstart;
  model.dtend = counter.counter.dtend;
  model.sendNotification = true;
  
  model.recurringDone = true;
  var evtAccept = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.SAVE, model);
  this.dispatchEvent(evtAccept);
}

net.bluemind.calendar.list.PendingCountersView.prototype.dispatchReject_ = function(counters, e) {
  var evtRejectCounters = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.REJECT_COUNTERS, counters);
  this.dispatchEvent(evtRejectCounters);
}

/**
 * @param {Object} event
 * @param {goog.event.Event} e
 */
net.bluemind.calendar.list.PendingCountersView.prototype.handleClick_ = function(event, e) {
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
net.bluemind.calendar.list.PendingCountersView.EventType = {
  BACK : goog.events.getUniqueId('back')

};
