/* BEGIN LICENSE
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
goog.provide('net.bluemind.calendar.vevent.ui.Card');

goog.require("goog.array");
goog.require("goog.date");
goog.require("goog.dom");
goog.require("goog.iter");
goog.require("goog.object");
goog.require("goog.string");
goog.require("goog.style");
goog.require("goog.date.Date");
goog.require("goog.date.Interval");
goog.require("goog.dom.classlist");
goog.require("goog.dom.forms");
goog.require("goog.editor.Field.EventType");
goog.require("goog.events.EventType");
goog.require("goog.events.InputHandler");
goog.require("goog.events.InputHandler.EventType");
goog.require("goog.i18n.DateTimeFormat");
goog.require("goog.i18n.DateTimeSymbols");// FIXME - unresolved required symbol
goog.require("goog.i18n.DateTimeSymbols_en");
goog.require("goog.positioning.AnchoredViewportPosition");
goog.require("goog.positioning.Corner");
goog.require("goog.structs.Map");
goog.require("goog.ui.Button");
goog.require("goog.ui.Component");
goog.require("goog.ui.Dialog");
goog.require("goog.ui.FlatButtonRenderer");
goog.require("goog.ui.InputDatePicker");
goog.require("goog.ui.LabelInput");
goog.require("goog.ui.MenuItem");
goog.require("goog.ui.Popup");
goog.require("goog.ui.Select");
goog.require("goog.ui.TabBar");
goog.require("goog.ui.Toolbar");
goog.require("goog.ui.ToolbarSeparator");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.DatePicker.Events");
goog.require("goog.ui.Dialog.ButtonSet");
goog.require("goog.ui.Dialog.EventType");
goog.require("goog.ui.ac.AutoComplete.EventType");
goog.require("goog.ui.style.app.ButtonRenderer");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.calendar.vevent.ac.AttendeeAutocomplete");
goog.require("net.bluemind.calendar.vevent.templates");
goog.require("net.bluemind.calendar.vevent.ui.Freebusy");
goog.require("net.bluemind.calendar.vevent.ui.TimePicker");
goog.require("net.bluemind.calendar.vevent.ui.TimePicker.EventType");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.date.DateRange");
goog.require("net.bluemind.date.DateTime");
goog.require("bluemind.net.OnlineHandler");
goog.require("bluemind.ui.Editor");
goog.require("bluemind.ui.TagBox");
goog.require("bluemind.ui.style.DangerousActionButtonRenderer");
goog.require("bluemind.ui.style.PrimaryActionButtonRenderer");

/**
 * BlueMind Calendar card
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.vevent.ui.Card = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);

  this.ctx = ctx;

  this.formatter = this.ctx.helper('dateformat').formatter;
  this.parser = this.ctx.helper('dateformat').parser;

  var child = new goog.ui.Toolbar();
  var renderer = goog.ui.style.app.ButtonRenderer.getInstance();
  child.setId('toolbar');
  this.addChild(child);

  /** @meaning calendar.back */
  var MSG_BACK = goog.getMsg('Back to calendar');
  child = new goog.ui.Button(goog.dom.createDom('div', [ goog.getCssName('goog-button-icon'), goog.getCssName('fa'),
      goog.getCssName('fa-lg'), goog.getCssName('fa-chevron-left') ]), goog.ui.style.app.ButtonRenderer.getInstance());
  child.setTooltip(MSG_BACK);
  child.setId('back');
  this.getChild('toolbar').addChild(child, true);

  // FIXME
  child = new net.bluemind.calendar.vevent.ui.Freebusy(ctx);
  child.setId('freebusy');
  this.addChild(child);

};
goog.inherits(net.bluemind.calendar.vevent.ui.Card, goog.ui.Component);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.vevent.ui.Card.prototype.ctx;

/**
 * @type {net.bluemind.i18n.DateTimeHelper.Formatter}
 */
net.bluemind.calendar.vevent.ui.Card.prototype.formatter;

/**
 * @type {net.bluemind.i18n.DateTimeHelper.Parser}
 */
net.bluemind.calendar.vevent.ui.Card.prototype.parser;

/**
 * Writables calendars
 * 
 * @type {Array.<Object>}
 * @private
 */
net.bluemind.calendar.vevent.ui.Card.prototype.calendars;

/**
 * Tags
 * 
 * @type {Array.<Object>}
 * @private
 */
net.bluemind.calendar.vevent.ui.Card.prototype.tags;

/** @override */
net.bluemind.calendar.vevent.ui.Card.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var weekdays = goog.array.clone(goog.i18n.DateTimeSymbols_en.WEEKDAYS);
  var narrow = goog.array.clone(goog.i18n.DateTimeSymbols.NARROWWEEKDAYS);
  var fdow = (goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK + 1) % 7;
  var el = this.getElement();
  var dom = this.getDomHelper();
  var model = this.getModel();
  var range = new net.bluemind.date.DateRange(model.dtstart, model.dtend);
  var date = null;
  if (!model.states.allday) {
    date = this.formatter.datetime.format(model.dtstart);
  } else {
    date = this.formatter.date.format(range.getStartDate());
  }
  if (range.count() <= 1) {
    if (!model.states.allday) {
      date += ' - ' + this.formatter.time.format(model.dtend);
    }
  } else if (!model.states.allday) {
    date += ' - ' + this.formatter.datetime.format(model.dtend);
  } else {
    date += ' - ' + this.formatter.date.format(range.getLastDate());
  }

  if (model.rrule && model.rrule.until) {
    model.rrule.until = this.formatter.datetime.format(model.rrule.until);
  }

  if (model.rrule && model.rrule.byday && model.rrule.byday.length > 0) {
    var weekdays = goog.array.clone(goog.i18n.DateTimeSymbols_en.STANDALONEWEEKDAYS);
    var i18n = goog.array.clone(goog.i18n.DateTimeSymbols.STANDALONEWEEKDAYS);

    var byday = [];
    for (var i = 0; i < model.rrule.byday.length; i++) {
      var day = model.rrule.byday[i].day;
      var offset = model.rrule.byday[i].offset;
      var index = goog.array.indexOf(goog.i18n.DateTimeSymbols_en.STANDALONEWEEKDAYS, day);
      if (offset == 0) {
        byday.push(i18n[index]);
      } else if (!goog.isDefAndNotNull(model.rrule.bymonth)) {
        byday.push({
          day : i18n[index],
          offset : offset
        });
      } else {
        byday.push({
          day : i18n[index],
          month : goog.i18n.DateTimeSymbols.STANDALONEMONTHS[model.rrule.bymonth],
          offset : offset
        });
      }
    }
    model.rrule.byday = byday;
  }

  el.innerHTML = net.bluemind.calendar.vevent.templates.card({
    event : this.getModel(),
    formatedDtstart : date,
    calendar : this.calendar,
    longweekdays : goog.array.rotate(weekdays, -fdow),
    narrowweekdays : goog.array.rotate(narrow, -fdow)
  });

  this.getChild('toolbar').renderBefore(el.firstChild);

};

/** @override */
net.bluemind.calendar.vevent.ui.Card.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var dom = this.getDomHelper();
  var handler = this.getHandler();
  handler.listen(this.getChild('toolbar').getChild('back'), goog.ui.Component.EventType.ACTION, function(e) {
    this.dispatchEvent(net.bluemind.calendar.vevent.EventType.BACK);
  });
}
