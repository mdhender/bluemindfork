/*
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

/** @fileoverview Event form widget */

goog.provide("net.bluemind.calendar.vevent.ui.CounterForm");

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
goog.require("net.bluemind.calendar.vevent.templates");
goog.require("net.bluemind.calendar.vevent.ui.Freebusy");
goog.require("net.bluemind.calendar.vevent.ui.TimePicker");
goog.require("net.bluemind.calendar.vevent.ui.TimePicker.EventType");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.date.DateTime");
goog.require("bluemind.calendar.template");// FIXME - unresolved required
// symbol
goog.require("bluemind.net.OnlineHandler");
goog.require("bluemind.ui.style.DangerousActionButtonRenderer");
goog.require("bluemind.ui.style.PrimaryActionButtonRenderer");
goog.require("net.bluemind.calendar.vevent.VEventAdaptor");
goog.require('net.bluemind.calendar.vevent.defaultValues');


/**
 * BlueMind Calendar form
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.vevent.ui.CounterForm = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.ctx = ctx;

  this.formatter = this.ctx.helper('dateformat').formatter;
  this.parser = this.ctx.helper('dateformat').parser;
  this.adaptor = new net.bluemind.calendar.vevent.VEventAdaptor(ctx);
  this.errors_ = new goog.structs.Map();
  this.errors_.set('dates', new Array());
  
  this.warnings_ = new goog.structs.Map();
  this.warnings_.set('dates', new Array());
  this.alarm_ = new goog.structs.Map();

  var child = new goog.ui.Toolbar();
  child.setId('toolbar');
  this.addChild(child);

  /** @meaning calendar.back */
  var MSG_BACK = goog.getMsg('Back to calendar');
  child = new goog.ui.Button(goog.dom.createDom('div', [ goog.getCssName('goog-button-icon'), goog.getCssName('fa'),
      goog.getCssName('fa-lg'), goog.getCssName('fa-chevron-left') ]), goog.ui.style.app.ButtonRenderer.getInstance());
  child.setTooltip(MSG_BACK);
  child.setId('back');

  this.getChild('toolbar').addChild(child, true);

  this.getChild('toolbar').addChild(new goog.ui.ToolbarSeparator(), true);

  /** @meaning general.send */
  var MSG_SAVE = goog.getMsg('Send');
  child = new goog.ui.Button(MSG_SAVE, bluemind.ui.style.PrimaryActionButtonRenderer.getInstance());
  child.setId('send');
  this.getChild('toolbar').addChild(child, true);

  this.getChild('toolbar').addChild(new goog.ui.ToolbarSeparator(), true);
  var menu = new goog.ui.Menu();
  
  /** @meaning calendar.event.leave.dialog */
  var MSG_LEAVE_TITLE = goog.getMsg('Do you really want to leave ?');
    /** @meaning calendar.event.leave.dialog.button.leave */
  var MSG_LEAVE = goog.getMsg('Leave');
    /** @meaning calendar.event.leave.dialog.button.stay */
  var MSG_STAY = goog.getMsg('Stay');
  child = new goog.ui.Dialog();
  child.setDraggable(false);
  child.setTitle(MSG_LEAVE_TITLE);
  var buttons = new goog.ui.Dialog.ButtonSet().addButton({
    key: goog.ui.Dialog.DefaultButtonKeys.YES,
    caption: MSG_LEAVE
  }, true).addButton(goog.ui.Dialog.ButtonSet.DefaultButtons.CANCEL, false, true);
  child.setButtonSet(buttons);
  child.setId('leave-dialog');
  this.addChild(child);

  child = new net.bluemind.calendar.vevent.ui.Form.Notification();
  child.setId('notifications')
  this.addChild(child);

  child = this.createDatePicker_();
  child.setId('dstart');
  this.addChild(child);
  child = new net.bluemind.calendar.vevent.ui.TimePicker(this.formatter.time, this.parser.time);
  child.setId('tstart');

  this.addChild(child);
  child = this.createDatePicker_();
  child.setId('dend');
  this.addChild(child);
  child = new net.bluemind.calendar.vevent.ui.TimePicker(this.formatter.time, this.parser.time);
  child.setId('tend');
  this.addChild(child);

  this.defaultTime = {};
  var date = new net.bluemind.date.DateTime();
  date.add(new goog.date.Interval(0, 0, 0, 2));
  date.setMinutes(0);
  date.setSeconds(0);
  date.setMilliseconds(0);
  this.defaultTime.start = {
    hours: date.getHours(),
    minutes: date.getMinutes()
  };
  date.add(new goog.date.Interval(goog.date.Interval.HOURS, 1));
  this.defaultTime.end = {
    hours: date.getHours(),
    minutes: date.getMinutes()
  }

  child = new net.bluemind.calendar.vevent.ui.Freebusy(ctx);
  child.setId('freebusy');
  this.addChild(child);
};
goog.inherits(net.bluemind.calendar.vevent.ui.CounterForm, goog.ui.Component);

/**
 * @type {Array}
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.eventAttendees_;

/**
 * @type {Object}
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.eventDtStart_;

/**
 * @type {Object}
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.eventDtEnd_;

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.ctx;

/**
 * @private
 * @type {net.bluemind.calendar.vevent.ac.AttendeeAutocomplete}
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.ac_;

/**
 * @type {net.bluemind.i18n.DateTimeHelper.Formatter}
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.formatter;

/**
 * @type {net.bluemind.i18n.DateTimeHelper.Parser}
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.parser;

/**
 * Error manager
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.errors_;

/**
 * Warning manager
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.warnings_;

/**
 * Writables calendars
 * 
 * @type {Array.<Object>}
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.calendars;

/**
 * Initial part status
 *
 * @type {String}
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.selectedPartStatus;

/**
 * Generate a date field
 * 
 * @return {goog.ui.InputDatePicker} picker.
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.createDatePicker_ = function() {
  var picker = new goog.ui.InputDatePicker(this.formatter.date, this.parser.date);
  picker.getDatePicker().setAllowNone(false);
  picker.getDatePicker().setShowToday(false);
  return picker;
};

/** @override */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  this.setModelValues_();
};

/** @override */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var el = this.getElement();
  var dom = this.getDomHelper();
  var model = this.getModel().counter;
  var weekdays = goog.array.clone(goog.i18n.DateTimeSymbols_en.WEEKDAYS);
  var narrow = goog.array.clone(goog.i18n.DateTimeSymbols.NARROWWEEKDAYS);
  var fdow = (goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK + 1) % 7;
  var dfd = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.MEDIUM_DATE);
  var dft = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.MEDIUM_TIME);
  if (this.eventDtStart_.date.getHours()){
    var evtDate = dfd.format(this.eventDtStart_) + ' ' + dft.format(this.eventDtStart_) + ' - ' + dfd.format(this.eventDtEnd_) + ' ' + dft.format(this.eventDtEnd_);
  } else {
    evtDate = dfd.format(this.eventDtStart_) + ' - ' + dfd.format(this.eventDtEnd_);
  }
  el.innerHTML = net.bluemind.calendar.vevent.templates.counterform({
    eventDate : evtDate,
    event : model,
    longweekdays : goog.array.rotate(weekdays, -fdow),
    narrowweekdays : goog.array.rotate(narrow, -fdow)
  });
  this.getChild('toolbar').renderBefore(el.firstChild);
  this.getChild('dstart').decorate(this.getElementByClass(goog.getCssName('_dstart')));
  if (model){
    this.getChild('dstart').setDate(model.dtstart);
  }
  this.getChild('tstart').render(this.getElementByClass(goog.getCssName('_tstart')));
  this.getChild('dend').decorate(this.getElementByClass(goog.getCssName('_dend')));
  if (model &&  model.states && model.states.allday) {
    var e = model.dtend.clone();
    e.add(new goog.date.Interval(0, 0, -1));
    this.getChild('dend').setDate(e);
  } else {
    if (model){
      this.getChild('dend').setDate(model.dtend);
    }
  }
  
  this.getChild('tend').render(this.getElementByClass(goog.getCssName('_tend')));
};

/** @override */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var dom = this.getDomHelper();
  var handler = this.getHandler();
  this.getChild('notifications').render(this.getElement());

  this.getChild('freebusy').decorate(this.getElementByClass(goog.getCssName('freebusy-root-panel')));
  this.setModelValues_();
  handler.listen(this.getChild('toolbar').getChild('back'), goog.ui.Component.EventType.ACTION, function(e) {
    this.dispatchEvent(net.bluemind.calendar.vevent.EventType.BACK);
  });

  handler.listen(this.getChild('leave-dialog'), goog.ui.Dialog.EventType.SELECT, function(e) {
    if (e.key == goog.ui.Dialog.DefaultButtonKeys.YES) {
      this.dispatchEvent(net.bluemind.calendar.vevent.EventType.BACK);
    }
  });

  handler.listen(this.getChild('toolbar').getChild('send'), goog.ui.Component.EventType.ACTION, function(e) {
    var type = net.bluemind.calendar.vevent.EventType.SEND;
    var evt = new net.bluemind.calendar.vevent.VEventEvent(type, this.getModel());
    this.dispatchEvent(evt);    
  });

  // DTSTART
  handler.listen(this.getChild('dstart'), goog.ui.DatePicker.Events.CHANGE, this.onDStartChange_).listen(
      this.getChild('dstart').getElement(), goog.events.EventType.CHANGE, this.onDStartChange_).listen(
      this.getChild('tstart'), net.bluemind.calendar.vevent.ui.TimePicker.EventType.UPDATE, this.onTStartChange_);

  // DTEND
  handler.listen(this.getChild('dend'), goog.ui.DatePicker.Events.CHANGE, this.onDEndChange_).listen(
      this.getChild('dend').getElement(), goog.events.EventType.CHANGE, this.onDEndChange_).listen(
      this.getChild('tend'), net.bluemind.calendar.vevent.ui.TimePicker.EventType.UPDATE, this.onTEndChange_);

  // ALLDAY
  handler.listen(dom.getElement('bm-ui-form-allday'), goog.events.EventType.CHANGE, function(){ 
    this.onAllDayChange_();
    var model = this.getModel().counter;
    this.getChild('freebusy').checkAvailability();
    this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, false);
  });

  // PARTICIPATION
  handler.listen(dom.getElement('bm-ui-form-participation'), goog.events.EventType.CHANGE, this.onParticipationChange_);
};

/**
 * Apply participation update
 *
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.onParticipationChange_ = function() {
  var dom = this.getDomHelper();
  var select = dom.getElement('bm-ui-form-participation');
  var value = goog.dom.forms.getValue(select);
  var model = this.getModel().counter;
  model.attendees[0].partStatus = value;
  model.participation = value;
}

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.setDTStart = function(date) {
  if (date != null){
    this.getChild('dstart').setDate(date);
    if (!this.getModel().counter.states || !this.getModel().counter.states.allday) {
      this.getChild('tstart').setTime(date);
    }
  }
};

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.setDTEnd = function(date) {
  if (date != null){
      this.getChild('dend').setDate(date);
      if (!this.getModel().counter.states || !this.getModel().counter.states.allday) {
      this.getChild('tend').setTime(date);
    }
  }
};
/**
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.setModelValues_ = function() {
  if (this.isInDocument()) {
    var model = this.getModel().counter;
    this.getChild('dstart').setDate(model.dtstart);
    if (model.dtstart.date.getHours()) {
      this.getChild('tstart').setValue(this.formatter.time.format(model.dtstart));
      this.getChild('tend').setValue(this.formatter.time.format(model.dtend));
      this.getChild('dend').setDate(model.dtend);
    } 

    if (this.selectedPartStatus){
      this.setFormValue_('participation', this.selectedPartStatus);
    } else {
      if (model.attendees[0]['partStatus'] && model.attendees[0]['partStatus'] != 'NeedsAction'){
        this.setFormValue_('participation', model.attendees[0]['partStatus']);
      }
    }
    this.onParticipationChange_();

    this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, true);
    this.getChild('freebusy').setModel(model);
    this.getChild('freebusy').initToolbar();
    this.getChild('freebusy').initGrid();

    this.eventAttendees_.push(model.organizer);

    this.getChild('freebusy').addAttendees(this.eventAttendees_);
    this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, true);
    this.getChild('freebusy').setVisible(true);
  
    this.setFormValue_('allday', model.states && model.states.allday);
    this.onAllDayChange_();
  }
};

/**
 * Apply check and modification when the all day status change
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.onAllDayChange_ = function() {
  var dom = this.getDomHelper();

  var checkbox = dom.getElement('bm-ui-form-allday');
  var value = goog.dom.forms.getValue(checkbox);
  var model = this.getModel().counter;

  model.states.allday = !!value;
  this.getChild('tstart').setVisible(!model.states.allday);
  this.getChild('tend').setVisible(!model.states.allday);
  goog.dom.classlist.enable(checkbox.parentNode, goog.getCssName('active'), model.states.allday);
  if (model.states.allday) {
    if (model.dtstart instanceof goog.date.DateTime) {
      this.defaultTime.start = {
        hours: model.dtstart.getHours(),
        minutes: model.dtstart.getMinutes()
      };
      model.dtstart = new net.bluemind.date.Date(model.dtstart);
    }
    if (model.dtend instanceof goog.date.DateTime) {
      this.defaultTime.end = {
        hours: model.dtend.getHours(),
        minutes: model.dtend.getMinutes()
      };
      model.dtend.add(new goog.date.Interval(goog.date.Interval.SECONDS, -1));
      model.dtend = new net.bluemind.date.Date(model.dtend);
      var e = model.dtend.clone();
      model.dtend.add(new goog.date.Interval(goog.date.Interval.DAYS, 1));
      this.getChild('dend').setDate(e);
    }
  } else {
    if (!(model.dtstart instanceof goog.date.DateTime)) {
      model.dtstart = new net.bluemind.date.DateTime(model.dtstart);
      model.dtstart.setHours(this.defaultTime.start.hours);
      model.dtstart.setMinutes(this.defaultTime.start.minutes);
    }
    if (!(model.dtend instanceof goog.date.DateTime)) {
      model.dtend = new net.bluemind.date.DateTime(model.dtend);
      model.dtend.add(new goog.date.Interval(goog.date.Interval.DAYS, -1));
      model.dtend.setHours(this.defaultTime.end.hours);
      model.dtend.setMinutes(this.defaultTime.end.minutes);
      if (goog.date.Date.compare(model.dtstart, model.dtend) >= 0) {
        model.dtend.setHours(model.dtstart.getHours());
        model.dtend.setMinutes(model.dtstart.getMinutes());
        model.dtend.add(new goog.date.Interval(goog.date.Interval.HOURS, 2));
      }
      this.getChild('dend').setDate(model.dtend);
    }
    this.getChild('tstart').setValue(this.formatter.time.format(model.dtstart));
    this.getChild('tend').setValue(this.formatter.time.format(model.dtend));
    
  }
  this.setFormActions_();

  this.checkDate_();
};


/**
 * @param {string} id Element id param {string | boolean | Array.<string>}
 * value element value
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.setFormValue_ = function(id, value) {
  var element = this.getDomHelper().getElement('bm-ui-form-' + id);
  goog.dom.forms.setValue(element, value);
  if (element.type.toLowerCase() == 'checkbox') {
    goog.dom.classlist.enable(element.parentNode, goog.getCssName('active'), !!value);
  }
};

/**
 * @return {goog.events.Event} e Change event.
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.onDStartChange_ = function(e) {
  if (e) e.stopPropagation();
  var model = this.getModel().counter;
  var date = this.getChild('dstart').getDate();

  if (!date) {
    this.getChild('dstart').setDate(model.dtstart);
  } else {
    var old = model.dtstart.clone();
    model.dtstart.setDatePart(date);

    this.onDTStartChange_(old, model.dtstart);
  }

  this.getChild('freebusy').checkAvailability();
  this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, false);
  this.checkDate_();
};

/**
 * @return {goog.events.Event} e Change event.
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.onTStartChange_ = function(e) {
  e.stopPropagation();
  var model = this.getModel().counter;
  if (!model.states.allday) {
    var old = model.dtstart.clone();
    var valid = this.parser.time.strictParse(this.getChild('tstart').getValue(), model.dtstart);
    if (valid <= 3) {
      /** @meaning calendar.form.error.date */
      var MSG_DATE_FORMAT_ERROR = goog.getMsg('Invalid date format');
      this.addError_('dates', this.getChild('tstart').getInputElement(), MSG_DATE_FORMAT_ERROR);
    }
    this.onDTStartChange_(old, model.dtstart);
  }
  this.getChild('freebusy').checkAvailability();
  this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, false);
  this.checkDate_();
};
/**
 * @return {goog.events.Event} e Change event.
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.onDEndChange_ = function(e) {
  e.stopPropagation();
  var model = this.getModel().counter;
  var date = this.getChild('dend').getDate();
  if (!date) {
    /** @meaning calendar.form.error.date */
    var MSG_DATE_FORMAT_ERROR = goog.getMsg('Invalid date format');
    this.addError_('dates', [], MSG_DATE_FORMAT_ERROR);
    date = model.dtend.clone();
    if (model.states.allday) {
      date.add(new goog.date.Interval(goog.date.Interval.DAYS, -1));
    }
    this.getChild('dend').setDate(date)
  } else {
    if (model.states.allday) {
      date.add(new goog.date.Interval(0, 0, 1));
    }
    var old = model.dtend.clone();
    model.dtend.setDatePart(date);
    this.onDTEndChange_(old, model.dtend);
  }
  this.getChild('freebusy').checkAvailability();
  this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, false);
  this.checkDate_();
};

/**
 * Apply check and modification when the dateend or timend change
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.onDTEndChange_ = function() {
  var model = this.getModel().counter;
  this.setFormActions_();
  this.getChild('freebusy').checkAvailability();
  this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, false);
  this.checkDate_();
};

/**
 * Apply check and modification when the datebegin, or timebegin change
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.onDTStartChange_ = function(old, dtstart) {
  var e, model = this.getModel().counter;

  this.adaptor.adjustDTend(model, old);
  if (!model.states.allday) {
    this.getChild('tend').setValue(this.formatter.time.format(model.dtend));
    e = model.dtend;
  } else {
    e = model.dtend.clone();
    e.add(new goog.date.Interval(goog.date.Interval.DAYS, -1));
  }
  this.getChild('dend').setDate(e);
  
  this.setFormActions_();
  this.getChild('freebusy').checkAvailability();
  this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, false);
};

/**
 * @return {goog.events.Event} e Change event.
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.onTEndChange_ = function(e) {
  e.stopPropagation();
  var model = this.getModel().counter;
  if (!model.states.allday) {
    var old = model.dtend.clone();
    var valid = this.parser.time.strictParse(this.getChild('tend').getValue(), model.dtend);
    if (valid <= 3) {
      /** @meaning calendar.form.error.date */
      var MSG_DATE_FORMAT_ERROR = goog.getMsg('Invalid date format');
      this.addError_('dates', this.getChild('tstart').getInputElement(), MSG_DATE_FORMAT_ERROR);
    }
    this.onDTEndChange_(old, model.dtend);
  }
  this.getChild('freebusy').checkAvailability();
  this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, false);
};
  
/**
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.updateModel_ = function() {
  this.setFormActions_();
}

/**
 * Check date validity
 * 
 * @return {boolean} Is Date valid.
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.checkDate_ = function() {
  this.resetError_('dates');
  var model = this.getModel().counter;
  var ret = true;
  var fields = new Array();
  if (goog.date.Date.compare(model.dtend, model.dtstart) < 0) {
    /** @meaning calendar.form.error.endBegin */
    var MSG_END_BEGIN_ERROR = goog.getMsg('End date cannot be before begin date');
    this.addError_('dates', this.getChild('dend').getElement(), MSG_END_BEGIN_ERROR);
    ret = false;
  } else {
    this.resetError_('dates');
  }

  return ret;
};

/**
 * @param {boolean} availability attendee availability.
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.availabilityWarn = function(availability) {
  if (!availability) {
    /** @meaning calendar.form.warning.availability */
    var MSG_AVAILABILITY_WARN = goog.getMsg('Not all attendees are available.');
    var fields = new Array();
    fields.push(this.getChild('dend').getElement());
    fields.push(this.getChild('dstart').getElement());
    this.addWarn_('dates', fields, MSG_AVAILABILITY_WARN);
  } else {
    this.resetWarn_('dates');
  }
};

/**
 * Add a warning to the form
 * 
 * @param {string} module Module that own this error.
 * @param {string | Element | Array} fields Fields concerned by this error.
 * @param {string} opt_text Optional error text.
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.addWarn_ = function(module, fields, opt_text) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }

  for (var i = 0; i < fields.length; i++) {
    var field = fields[i];
    var elt = null;
    if (goog.typeOf(field) == 'string') {
      elt = goog.dom.getElement(field);
      fields[i] = field;
    } else {
      elt = field;
    }
    if (elt) {
      this.warnings_.get(module).push({
        type : 'field',
        value : elt
      });
      goog.dom.classlist.add(elt, goog.getCssName('warn'));
    }
  }
  if (!opt_text) {
    /** @meaning calendar.form.error.default */
    var MSG_DEFAULT_ERROR = goog.getMsg('Invalid format');
    opt_text = MSG_DEFAULT_ERROR;
  }

  this.getChild('notifications').addWarn(fields, opt_text);
};

/**
 * Add an error to the form
 * 
 * @param {string} module Module that own this error.
 * @param {string | Element | Array} fields Fields concerned by this error.
 * @param {string} opt_text Optional error text.
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.addError_ = function(module, fields, opt_text) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }

  for (var i = 0; i < fields.length; i++) {
    var field = fields[i];
    if (goog.typeOf(field) == 'string') {
      field = goog.dom.getElement(field);
      fields[i] = field;
    }
    this.errors_.get(module).push({
      type : 'field',
      value : field
    });
    goog.dom.classlist.add(field, goog.getCssName('error'));
  }
  if (!opt_text) {
    /** @meaning calendar.form.error.default */
    var MSG_DEFAULT_ERROR = goog.getMsg('Invalid format');
    opt_text = MSG_DEFAULT_ERROR;
  }

  this.getChild('notifications').addError(fields, opt_text);
};

/* Reset errors for a module
 * 
 * @param {string} module Module to clear.
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.resetWarn_ = function(module) {
  var warnings = this.warnings_.get(module);
  for (var i = 0; i < warnings.length; i++) {
    switch (warnings[i].type) {
    case 'field':
      goog.dom.classlist.remove(warnings[i].value, goog.getCssName('warn'));
      this.getChild('notifications').removeWarn(warnings[i].value);
      break;
    }
  }
  goog.array.clear(this.warnings_.get(module));
};

/**
 * Reset errors for a module
 * 
 * @param {string} module Module to clear.
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.resetError_ = function(module) {
  var errors = this.errors_.get(module);
  for (var i = 0; i < errors.length; i++) {
    switch (errors[i].type) {
    case 'field':
      goog.dom.classlist.remove(errors[i].value, goog.getCssName('error'));
      this.getChild('notifications').removeError(errors[i].value);
      break;
    }
  }
  goog.array.clear(this.errors_.get(module));
};

net.bluemind.calendar.vevent.ui.CounterForm.prototype.setFormActions_ = function() {
  /** @meaning general.send */
  var MSG_SAVE = goog.getMsg('Send');
  this.getChild('toolbar').getChild("send").setCaption(MSG_SAVE);
}

/**
 * show notice message
 * 
 * @param {text} msg message to display.
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.notice_ = function(msg) {
  this.getChild('notifications').addNotice(msg);
};

/**
 * show warning message
 * 
 * @param {text} msg message to display.
 * @private
 */
net.bluemind.calendar.vevent.ui.CounterForm.prototype.warn_ = function(msg) {
  this.getChild('notifications').addWarn(goog.dom.getElement('bm-ui-form-title'), msg);
};

