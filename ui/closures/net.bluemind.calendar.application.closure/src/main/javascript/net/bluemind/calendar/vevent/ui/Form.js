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

goog.provide("net.bluemind.calendar.vevent.ui.Form");
goog.provide("net.bluemind.calendar.vevent.ui.Form.Notification");

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
goog.require("net.bluemind.calendar.vevent.ui.Counters");
goog.require("net.bluemind.calendar.vevent.ui.TimePicker");
goog.require("net.bluemind.calendar.vevent.ui.TimePicker.EventType");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.date.DateTime");
goog.require("bluemind.calendar.template");// FIXME - unresolved required
// symbol
goog.require("bluemind.net.OnlineHandler");
goog.require("bluemind.ui.Editor");
goog.require("net.bluemind.ui.form.TagField");
goog.require("bluemind.ui.style.DangerousActionButtonRenderer");
goog.require("bluemind.ui.style.PrimaryActionButtonRenderer");
goog.require("net.bluemind.calendar.vevent.VEventAdaptor");
goog.require("net.bluemind.history.HistoryDialog");
goog.require('net.bluemind.filehosting.api.FileHostingClient');
goog.require('net.bluemind.calendar.vevent.defaultValues');

/**
 * BlueMind Calendar form
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.vevent.ui.Form = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);

  this.ctx = ctx;

  this.formatter = this.ctx.helper('dateformat').formatter;
  this.parser = this.ctx.helper('dateformat').parser;
  this.adaptor = new net.bluemind.calendar.vevent.VEventAdaptor(ctx);
  this.errors_ = new goog.structs.Map();
  this.errors_.set('details', new Array());
  this.errors_.set('dates', new Array());
  this.errors_.set('repeat', new Array());
  this.errors_.set('title', new Array());
  this.errors_.set('attendees', new Array());

  this.warnings_ = new goog.structs.Map();
  this.warnings_.set('attendees', new Array());
  this.warnings_.set('dates', new Array());
  this.warnings_.set('master', new Array());

  this.alarm_ = new goog.structs.Map();

  var setDefaultTrigger = function(settingName, alarmSetting) {
    var alarmAction = net.bluemind.calendar.vevent.defaultValues.action;
    if (this.ctx.settings.get('default_event_alert_mode')){
      alarmAction = this.ctx.settings.get('default_event_alert_mode');
    }
    if (this.ctx.settings.get(settingName) && !isNaN(parseInt(this.ctx.settings.get(settingName)))) {
        this.alarm_.set(alarmSetting, [
            {
                trigger: this.ctx.settings.get(settingName),
                action: alarmAction
            }
        ]);
    }
  }.bind(this);

  setDefaultTrigger("default_event_alert", "inday");
  setDefaultTrigger("default_allday_event_alert", "allday");
  
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

  /** @meaning general.save */
  var MSG_SAVE = goog.getMsg('Save');
  child = new goog.ui.Button(MSG_SAVE, bluemind.ui.style.PrimaryActionButtonRenderer.getInstance());
  child.setId('send');
  this.getChild('toolbar').addChild(child, true);

  this.getChild('toolbar').addChild(new goog.ui.ToolbarSeparator(), true);

  /** @meaning calendar.save_draft */
  var MSG_SAVE_DRAFT = goog.getMsg('Save draft');
  child = new goog.ui.Button(goog.dom.createDom('div', [ goog.getCssName('goog-button-icon'), goog.getCssName('fa'),
  goog.getCssName('fa-lg'), goog.getCssName('fa-download') ]), goog.ui.style.app.ButtonRenderer.getInstance());
  child.setTooltip(MSG_SAVE_DRAFT);
  child.setId('save');
  child.setVisible(false);

  this.getChild('toolbar').addChild(child, true);

  this.getChild('toolbar').addChild(new goog.ui.ToolbarSeparator(), true);

  var menu = new goog.ui.Menu();
  
  /** @meaning calendar.action.duplicate */
  var MSG_DUPLICATE = goog.getMsg('Duplicate');
  child = new goog.ui.MenuItem(MSG_DUPLICATE);
  child.setId('duplicate');
  menu.addChild(child, true);

  /** @meaning calendar.action.duplicateOccurrence */
  var MSG_DUPLICATE_OCC = goog.getMsg('Duplicate occurrence');
  child = new goog.ui.MenuItem(MSG_DUPLICATE_OCC);
  child.setId('duplicate-occurrence');
  menu.addChild(child, true);

  /** @meaning general.history */
  var MSG_HISTORY = goog.getMsg('History');
  child = new goog.ui.MenuItem(MSG_HISTORY);
  child.setId('history');
  menu.addChild(child, true);

  child = new goog.ui.ToolbarMenuButton(goog.dom.createDom('div', [ goog.getCssName('goog-button-icon'),
  goog.getCssName('fa'), goog.getCssName('fa-ellipsis-v') ]), menu, goog.ui.style.app.MenuButtonRenderer.getInstance());
  child.setId('others');
  this.getChild('toolbar').addChild(child, true);
  

  /** @meaning general.remove */
  var MSG_REMOVE = goog.getMsg('Remove');
  child = new goog.ui.Button(MSG_REMOVE, bluemind.ui.style.DangerousActionButtonRenderer.getInstance());
  child.setId('delete');
  child.setVisible(false);
  this.getChild('toolbar').addChild(child, true);
  goog.style.setStyle(child.getElement(), 'float', 'right');

  /** @meaning calendar.event.delete */
  var MSG_DELETE_TITLE = goog.getMsg('Delete event');
  /** @meaning calendar.event.delete.caption */
  var MSG_DELETE_CONTENT = goog.getMsg('Would you like to delete this event?');
  child = new goog.ui.Dialog();
  child.setDraggable(false);
  child.setTitle(MSG_DELETE_TITLE);
  child.setContent(MSG_DELETE_CONTENT);
  child.setButtonSet(goog.ui.Dialog.ButtonSet.YES_NO);
  child.setId('delete-dialog');
  this.addChild(child);

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

    /** @meaning calendar.event.change_attendees.dialog */
    var MSG_CHANGE_ATT_TITLE = goog.getMsg('You have added or deleted attendees');
    /** @meaning calendar.event.change_attendees.caption */
    var MSG_CHANGE_ATT_CONTENT = goog.getMsg('Do you want to send an update only to changed attendees, or to all attendees ?');
      /** @meaning calendar.event.change_attendees.button.send_all */
    var MSG_SEND_ALL = goog.getMsg('Send to All');
      /** @meaning calendar.event.change_attendees.button.send_changed */
    var MSG_SEND_CHANGED = goog.getMsg('Send to Changed');
    child = new goog.ui.Dialog();
    child.setDraggable(false);
    child.setTitle(MSG_CHANGE_ATT_TITLE);
    child.setContent(MSG_CHANGE_ATT_CONTENT);
    var buttons = new goog.ui.Dialog.ButtonSet().addButton({
      key: goog.ui.Dialog.DefaultButtonKeys.NO,
      caption: MSG_SEND_ALL
    }, true).addButton({
      key: goog.ui.Dialog.DefaultButtonKeys.YES,
      caption: MSG_SEND_CHANGED
    }).addButton(goog.ui.Dialog.ButtonSet.DefaultButtons.CANCEL, false, true);
    child.setButtonSet(buttons);
    child.setId('attendee-dialog');
    this.addChild(child);


  child = new net.bluemind.calendar.vevent.ui.Form.Notification();
  child.setId('notifications')
  this.addChild(child);

  child = new net.bluemind.ui.form.TagField();
  child.setId('tags');
  child.addClassName(goog.getCssName('no-label'));
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

  child = this.createDatePicker_();
  child.setId('until');
  this.addChild(child);

  child = new goog.ui.LabelInput();
  child.setId('count');
  this.addChild(child);

  child = new net.bluemind.calendar.vevent.ui.Freebusy(ctx);
  child.setId('freebusy');
  this.addChild(child);

  child = new net.bluemind.calendar.vevent.ui.Counters(ctx);
  child.setId('counters');
  this.addChild(child);

  child = new goog.ui.TabBar();
  child.setId('details');
  this.addChild(child);
  /** @meaning calendar.event.addAttendee */
  var MSG_ADD_ATTENDEE = goog.getMsg('Add an attendee...')
  child = new goog.ui.LabelInput(MSG_ADD_ATTENDEE);
  child.setId('attendee-autocomplete');
  this.addChild(child);

  var dummy = new goog.ui.Component();
  dummy.setId('reminder')
  this.addChild(dummy);

  var history = new net.bluemind.history.HistoryDialog(ctx);
  history.setId('history-dialog');
  this.addChild(history);

  this.ac_ = new net.bluemind.calendar.vevent.ac.AttendeeAutocomplete(ctx);
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
};
goog.inherits(net.bluemind.calendar.vevent.ui.Form, goog.ui.Component);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.vevent.ui.Form.prototype.ctx;

/**
 * @private
 * @type {net.bluemind.calendar.vevent.ac.AttendeeAutocomplete}
 */
net.bluemind.calendar.vevent.ui.Form.prototype.ac_;

/**
 * @type {net.bluemind.i18n.DateTimeHelper.Formatter}
 */
net.bluemind.calendar.vevent.ui.Form.prototype.formatter;

/**
 * @type {net.bluemind.i18n.DateTimeHelper.Parser}
 */
net.bluemind.calendar.vevent.ui.Form.prototype.parser;

/**
 * Error manager
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.errors_;

/**
 * Warning manager
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.warnings_;

/**
 * Writables calendars
 * 
 * @type {Array.<Object>}
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.calendars;

/**
 * counters
 * 
 * @type {Array.<Object>}
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.counters;


/**
 * Tags
 * 
 * @type {Array.<Object>}
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.tags;

/**
 * Alarms
 * 
 * @type {Array.<Object>}
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.alarm_;

/**
 * Generate a date field
 * 
 * @return {goog.ui.InputDatePicker} picker.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.createDatePicker_ = function() {
  var picker = new goog.ui.InputDatePicker(this.formatter.date, this.parser.date);
  picker.getDatePicker().setAllowNone(false);
  picker.getDatePicker().setShowToday(false);
  return picker;
};

/** @override */
net.bluemind.calendar.vevent.ui.Form.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  this.setModelValues_();
};

/** @override */
net.bluemind.calendar.vevent.ui.Form.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var el = this.getElement();
  var dom = this.getDomHelper();
  var model = this.getModel();
  var weekdays = goog.array.clone(goog.i18n.DateTimeSymbols_en.WEEKDAYS);
  var narrow = goog.array.clone(goog.i18n.DateTimeSymbols.NARROWWEEKDAYS);
  var fdow = (goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK + 1) % 7;
  el.innerHTML = net.bluemind.calendar.vevent.templates.form({
    defaultCalendar : this.getModel().states.defaultCalendar,
    event : this.getModel(),
    calendars : this.calendars,
    longweekdays : goog.array.rotate(weekdays, -fdow),
    narrowweekdays : goog.array.rotate(narrow, -fdow)
  });
  this.getChild('toolbar').renderBefore(el.firstChild);

  this.getChild('tags').setTags(this.tags);
  this.getChild('tags').render(this.getElementByClass(goog.getCssName('_tags')));
  this.getChild('dstart').decorate(this.getElementByClass(goog.getCssName('_dstart')));
  this.getChild('dstart').setDate(model.dtstart);

  this.getChild('tstart').render(this.getElementByClass(goog.getCssName('_tstart')));
  this.getChild('dend').decorate(this.getElementByClass(goog.getCssName('_dend')));
  if (model.states.allday) {
    var e = model.dtend.clone();
    e.add(new goog.date.Interval(0, 0, -1));
    this.getChild('dend').setDate(e);
  } else {
    this.getChild('dend').setDate(model.dtend);
  }

  this.getChild('tend').render(this.getElementByClass(goog.getCssName('_tend')));
  // if rrule
  this.getChild('until').decorate(this.getElementByClass(goog.getCssName('_until')));
  this.getChild('count').decorate(this.getElementByClass(goog.getCssName('_count')));

  // if meeting

  this.getChild('details').decorate(this.getElementByClass(goog.getCssName('_details')));
  this.getChild('attendee-autocomplete').decorate(this.getElementByClass(goog.getCssName('bm-ui-form-attendee-input')));
  this.ac_.attachInputs(this.getChild('attendee-autocomplete').getElement());
  this.getChild('reminder').decorate(this.getElementByClass(goog.getCssName('bm-ui-form-reminder-block')));

};

net.bluemind.calendar.vevent.ui.Form.prototype.showHistory = function(entries) {
  this.getChild('history-dialog').show(entries);
}

/** @override */
net.bluemind.calendar.vevent.ui.Form.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var dom = this.getDomHelper();
  var handler = this.getHandler();
  this.getChild('freebusy').decorate(this.getElementByClass(goog.getCssName('freebusy-root-panel')));
  this.getChild('counters').decorate(this.getElementByClass(goog.getCssName('counters-root-panel')));

  this.getChild('notifications').render(this.getElement());

  if (!this.editor_) {
    this.editor_ = new bluemind.ui.Editor('bm-ui-form-description');
    this.registerDisposable(this.editor_);
  }

  this.setModelValues_();

  handler.listen(this.getChild('details'), goog.ui.Component.EventType.SELECT, function(e) {
    this.switchTab_(e.target);
  });

  handler.listen(this.getChild('toolbar').getChild('delete'), goog.ui.Component.EventType.ACTION, function(e) {
      this.getChild('delete-dialog').setVisible(true);
  });

  handler.listen(this.getChild('delete-dialog'), goog.ui.Dialog.EventType.SELECT, function(e) {
    if (e.key == 'yes') {
      var type = net.bluemind.calendar.vevent.EventType.REMOVE;
      var evt = new net.bluemind.calendar.vevent.VEventEvent(type, this.getModel());
      this.dispatchEvent(evt);
    }
  });
  
  handler.listen(this.getChild('toolbar').getChild('back'), goog.ui.Component.EventType.ACTION, function(e) {
    var model = this.getModel();
    if (this.adaptor.isModified(model.old, model)) {
      /** @meaning calendar.event.leave.dialog.unsaved */
      var MSG_UNSAVED_MODIFICATION = goog.getMsg('You have unsaved modifications, changes you made will be lost. Do you really want to leave ?');
      this.getChild('leave-dialog').setContent(MSG_UNSAVED_MODIFICATION);        
      this.getChild('leave-dialog').setVisible(true);
    } else if (model.states.meeting && model.states.draft) {
        /** @meaning calendar.event.leave.dialog.unsent */
      var MSG_UNSENT_MODIFICATION = goog.getMsg('You have unsent modifications, changes you made will not be sent to attendees. Do you really want to leave ?');
      this.getChild('leave-dialog').setContent(MSG_UNSENT_MODIFICATION);
      this.getChild('leave-dialog').setVisible(true);
    } else  {
      this.dispatchEvent(net.bluemind.calendar.vevent.EventType.BACK);
    }
  });
  handler.listen(this.getChild('leave-dialog'), goog.ui.Dialog.EventType.SELECT, function(e) {
    if (e.key == goog.ui.Dialog.DefaultButtonKeys.YES) {
      this.dispatchEvent(net.bluemind.calendar.vevent.EventType.BACK);
    }
  });
  handler.listen(this.getChild('attendee-dialog'), goog.ui.Dialog.EventType.SELECT, function(e) {
    if (e.key == goog.ui.Dialog.DefaultButtonKeys.NO) {
      this.getModel().sequence = (this.getModel().sequence || 0) + 1;
    }
    if (e.key != goog.ui.Dialog.DefaultButtonKeys.CANCEL ) {
      var type = net.bluemind.calendar.vevent.EventType.SEND;
      var evt = new net.bluemind.calendar.vevent.VEventEvent(type, this.getModel());
      this.dispatchEvent(evt);
    }
  });
  handler.listen(this.getChild('toolbar').getChild('save'), goog.ui.Component.EventType.ACTION, function(e) {
    var type = net.bluemind.calendar.vevent.EventType.SAVE;
    var evt = new net.bluemind.calendar.vevent.VEventEvent(type, this.getModel());
    this.dispatchEvent(evt);
  });

  handler.listen(this.getChild('toolbar').getChild('send'), goog.ui.Component.EventType.ACTION, function(e) {
    var model = this.getModel();
    if (!model.states.draft && this.adaptor.isPublicChanges(model.old, model) && !this.adaptor.contentHasBeenModified(model.old, model)) {
      this.getChild('attendee-dialog').setVisible(true);
    } else {
      var type = net.bluemind.calendar.vevent.EventType.SEND;
      var evt = new net.bluemind.calendar.vevent.VEventEvent(type, this.getModel());
      this.dispatchEvent(evt);
    }
  });


  handler.listen(this.getChild('toolbar').getChild('others'), goog.ui.Component.EventType.ACTION, this.menuActions_);

  // DTSTART
  handler.listen(this.getChild('dstart'), goog.ui.DatePicker.Events.CHANGE, this.onDStartChange_).listen(
      this.getChild('dstart').getElement(), goog.events.EventType.CHANGE, this.onDStartChange_).listen(
      this.getChild('tstart'), net.bluemind.calendar.vevent.ui.TimePicker.EventType.UPDATE, this.onTStartChange_);

  // DTEND
  handler.listen(this.getChild('dend'), goog.ui.DatePicker.Events.CHANGE, this.onDEndChange_).listen(
      this.getChild('dend').getElement(), goog.events.EventType.CHANGE, this.onDEndChange_).listen(
      this.getChild('tend'), net.bluemind.calendar.vevent.ui.TimePicker.EventType.UPDATE, this.onTEndChange_);

  // TITLE
  var el = dom.getElement('bm-ui-form-title');
  var ih = new goog.events.InputHandler(el);
  this.registerDisposable(ih);
  handler.listen(el, goog.events.EventType.BLUR, this.checkTitle_);
  handler.listen(el, goog.events.InputHandler.EventType.INPUT, function(e) {
    this.getModel().summary = goog.dom.forms.getValue(e.target);
    this.setFormActions_();
  });

  // REMINDER
  handler.listen(dom.getElement('bm-ui-form-add-reminder'), goog.events.EventType.CLICK, function() {
    var defaultAlert = this.ctx.settings.get('default_event_alert');
    if(this.getModel().states.allday){
      defaultAlert = this.ctx.settings.get('default_allday_event_alert');
    }
    var alarmAction = net.bluemind.calendar.vevent.defaultValues.action;
    if (this.ctx.settings.get('default_event_alert_mode')){
      alarmAction = this.ctx.settings.get('default_event_alert_mode');
    }
    this.addReminder_({
      trigger : defaultAlert,
      action : alarmAction
    });
  });
  el = dom.getElement('bm-ui-form-reminder');
  
  // ATTACHMENTS
  goog.array.map(this.getModel().attachments, function(attachment) {
    handler.listen(dom.getElement('bm-ui-form-delete-attachment-'+attachment.index), goog.events.EventType.CLICK, this.delAttachment(attachment));
  }, this);
  
  var canRemoteAttach = goog.global['bmcSessionInfos']['roles'].split(',').indexOf('canRemoteAttach') >= 0;
  if (!canRemoteAttach){
    this.getDomHelper().removeNode(dom.getElement('bm-ui-form-no-attachment-block'));
    this.getDomHelper().removeNode(dom.getElement('add-attachment-label'));
    if (this.getModel().attachments.length == 0){
      this.getDomHelper().removeNode(dom.getElement('attachment-label'));
    }
  } else {
    handler.listen(dom.getElement('localAttachmentFile'), goog.events.EventType.CHANGE, function() {
      this.resetError_('details');
      var fileInput = document.getElementById('localAttachmentFile');
      var file = fileInput.files[0];
      var sid = goog.global['bmcSessionInfos']['sid'];
      var domain = goog.global['bmcSessionInfos']['domain'];
      var url = '/api/attachment/' + encodeURIComponent(domain) + '/' + encodeURIComponent(file.name) + '/share';
      var xhr = new XMLHttpRequest();
      xhr.open('PUT', url, true);
      xhr.setRequestHeader('X-BM-ApiKey', sid);
      var that = this;  
      that.getDomHelper().getElement('local-att-progress').style.visibility = 'visible';
      xhr.upload.onprogress = function(e){
        console.log(e.loaded + "  - > " + e.total);
        var p = Math.min((e.loaded/e.total)*100, 80);
        that.getDomHelper().getElement('local-att-progress').value = p;
      }
      xhr.onload = function () {
          that.getDomHelper().getElement('local-att-progress').style.visibility = 'hidden';
          that.getDomHelper().getElement('local-att-progress').value = 0;
          if(this.status == 413){
            /** @meaning calendar.form.error.attachment.size */
            var MSG_ATTACHMENT_SIZE = goog.getMsg('The selected attachment exceeds the configured max size');
            that.addError_('details', that.getDomHelper().getElement('add-attachment-label'), MSG_ATTACHMENT_SIZE);
            return;
          } 
          if(this.status != 200){
            /** @meaning calendar.form.error.attachment */
            var MSG_ATTACHMENT = goog.getMsg('an unknown error occurred while uploading the document');
            that.addError_('details', that.getDomHelper().getElement('add-attachment-label'), MSG_ATTACHMENT);
            return;
          }
          var ret = JSON.parse(this.response);
          that.addAttachment(that, ret, dom);
          dom.getElement('localAttachmentFile').value = "";
      };
      xhr.send(file);
    });
   
    handler.listen(dom.getElement('bm-ui-form-add-attachment-server'), goog.events.EventType.CLICK, function() {
        var that = this; 
        var options = {
          'success': function(links) {
            goog.array.forEach(links, function(link) {
              var client = new net.bluemind.filehosting.api.FileHostingClient(that.ctx.rpc, '', that.ctx.user.domainUid);
              var ret = client.share(link.path, 0, null).then(function(linkInfo) {
                 linkInfo['publicUrl'] = linkInfo['url'];
                 linkInfo['name'] = link['name'];
                 that.addAttachment(that, linkInfo, dom);
              });
            });
          },
          'multi': true,
          'close': true
        };
        var w = 640, h = 512;
        var t = (window.screenY || window.screenTop) + ((window.outerHeight || document.documentElement.offsetHeight) - h) / 2;
        var l = (window.screenX || window.screenLeft) + ((window.outerWidth || document.documentElement.offsetWidth) - w) / 2;
        var child = window.open('/chooser/#', 'chooser', "width=" + w + ",height=" + h + ",left=" + l + ",top=" + t)
        var setOptions = function() {
          if (child['application']) {
            child['application']['setOptions'](options);
          } else {
            setTimeout(setOptions, 50);
          };
        }
        setOptions();
    });
  }

  // LOCATION
  el = dom.getElement('bm-ui-form-location');
  ih = new goog.events.InputHandler(el);
  this.registerDisposable(ih);
  handler.listen(el, goog.events.InputHandler.EventType.INPUT, function(e) {
    this.getModel().location = goog.dom.forms.getValue(e.target);
    this.setFormActions_();
  });

  // URL
  el = dom.getElement('bm-ui-form-url');
  ih = new goog.events.InputHandler(el);
  this.registerDisposable(ih);
  handler.listen(el, goog.events.InputHandler.EventType.INPUT, function(e) {
    var url = goog.dom.forms.getValue(e.target);
    this.getModel().url = url;
    this.setFormActions_();

    if( url ) {
      if (!goog.Uri.parse(url).hasScheme()) {
        url = 'http://' + url;
      }
      goog.style.setElementShown( this.getDomHelper().getElement('bm-ui-form-vevent-url'), true);
      this.getDomHelper().getElement('bm-ui-form-vevent-url').href = url;
    } else {
      goog.style.setElementShown( this.getDomHelper().getElement('bm-ui-form-vevent-url'), false);
    }
  });

  // ALLDAY
  handler.listen(dom.getElement('bm-ui-form-allday'), goog.events.EventType.CHANGE, this.onAllDayChangeAndReminderUpdate_);

  // ACCEPT COUNTERS
  handler.listen(dom.getElement('bm-ui-form-accept-counters'), goog.events.EventType.CHANGE, this.onAcceptCountersUpdate_);

  // OPACITY
  handler.listen(dom.getElement('bm-ui-form-opacity-busy'), goog.events.EventType.CHANGE, function(e) {
    this.getModel().transp = 'Opaque';
    this.getModel().states.busy = true;
    this.setFormActions_();
    this.getChild('freebusy').checkAvailability();
  }).listen(dom.getElement('bm-ui-form-opacity-free'), goog.events.EventType.CHANGE, function(e) {
    this.getModel().transp = 'Transparent';
    this.getModel().states.busy = false;
    this.setFormActions_();

    this.availabilityWarn(false);
  });
  // PRIVACY
  handler.listen(dom.getElement('bm-ui-form-privacy-private'), goog.events.EventType.CHANGE, function(e) {
    this.getModel().class = 'Private';
    this.getModel().states.private_ = true;
    this.setFormActions_();

  }).listen(dom.getElement('bm-ui-form-privacy-public'), goog.events.EventType.CHANGE, function(e) {
    this.getModel().class = 'Public';
    this.getModel().states.private_ = false;
    this.setFormActions_();

  });
  // DESCRIPTION
  handler.listen(this.editor_, goog.editor.Field.EventType.DELAYEDCHANGE, this.onEditorChange_);

  handler.listen(dom.getElement('bm-ui-form-owner'), goog.events.EventType.CHANGE, this.onOwnerChange_);

  // RRULE.FREQ
  handler.listen(dom.getElement('bm-ui-form-repeat'), goog.events.EventType.CHANGE, this.onRepeatChange_);

  // RRULE.INTERVAL
  handler.listen(dom.getElement('bm-ui-form-repeat-periodicity'), goog.events.EventType.BLUR,
      this.onRepeatPeriodChange_);

  // RRULE.UNTIL THE END OF TIME
  handler
      .listen(dom.getElement('bm-ui-form-repeat-end-never'), goog.events.EventType.CHANGE, this.onRepeatEndOnChange_);
  // RRULE.UNTIL A GIVEN DATE
  handler.listen(dom.getElement('bm-ui-form-repeat-end-on'), goog.events.EventType.CHANGE, this.onRepeatEndOnChange_);
  handler.listen(this.getChild('until'), goog.ui.DatePicker.Events.CHANGE, this.onUntilChange_).listen(
      this.getChild('until').getElement(), goog.ui.DatePicker.Events.CHANGE, this.onUntilChange_);
  // RRULE.UNTIL COUNT
  handler.listen(dom.getElement('bm-ui-form-repeat-count'), goog.events.EventType.CHANGE, this.onRepeatEndOnChange_);
  handler.listen(dom.getElement('bm-ui-form-repeat-count-value'), goog.events.EventType.BLUR, this.onCountChange_);

  for (var i = 0; i < 7; i++) {
    var day = goog.i18n.DateTimeSymbols_en.WEEKDAYS[i];
    var element = goog.dom.getElement('bm-ui-form-repeat-days-' + day);
    handler.listen(element, goog.events.EventType.CHANGE, this.onRepeatDaysChange_);
  }

  element = goog.dom.getElement('bm-ui-form-repeat-by-date');
  handler.listen(element, goog.events.EventType.CHANGE, this.onRepeatByChange_);
  element = goog.dom.getElement('bm-ui-form-repeat-by-day');
  handler.listen(element, goog.events.EventType.CHANGE, this.onRepeatByChange_);

  handler.listen(this.getChild('tags'), goog.ui.Component.EventType.CHANGE, function() {
    this.setFormActions_();

    this.getModel().tags = this.getChild('tags').getValue();
  });

  this.getHandler().listen(this.ac_, goog.ui.ac.AutoComplete.EventType.UPDATE, this.handleAddAttendee_);
  this.sizeMonitor_ = new goog.dom.ViewportSizeMonitor();
  this.getHandler().listen(this.sizeMonitor_, goog.events.EventType.RESIZE, this.handleResize_);
  this.resize_();

  // focus on title field
  dom.getElement('bm-ui-form-title').focus();
};

/**
 * private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.addAttachment = function(that, ret, dom){
  var index = 0;
  for (var i = 0; i < that.getModel().attachments.length; i++) { 
    index = Math.max(that.getModel().attachments[i].index, index);
  } 
  index++;
  var publicUrl = ret['publicUrl'];
  var name = ret['name'];
  var newAttachment  = {
      publicUrl : publicUrl,
      name : name,
      index : index
  }
  
  that.getModel().attachments.push(newAttachment);
  var entry = soy.renderAsFragment(net.bluemind.calendar.vevent.templates.attachmentEntry, {
    attachment : newAttachment
  });

  that.getDomHelper().appendChild(dom.getElement('bm-attachment-list'), entry);
  that.getHandler().listen(dom.getElement('bm-ui-form-delete-attachment-'+newAttachment.index), goog.events.EventType.CLICK, that.delAttachment(newAttachment));
  this.setFormActions_();

}

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.delAttachment = function(attachment){
  return function() {
    for( var i = 0; i < this.getModel().attachments.length; i++){ 
      if (this.getModel().attachments[i].name === attachment.name) {
        this.getModel().attachments.splice(i, 1); 
      }
    }
    this.getDomHelper().removeNode(this.getDomHelper().getElement('div-bm-ui-form-delete-attachment-'+attachment.index));
    this.setFormActions_();
   }
}

net.bluemind.calendar.vevent.ui.Form.prototype.applyCounterDates = function(dateBegin, dateEnd) {
  var dom = this.getDomHelper();

  var startCloned = dateBegin.clone();
  var endCloned = dateEnd.clone();

  if (!endCloned.date.getHours()){
    endCloned.add(new goog.date.Interval(0, 0, -1));
  }
  this.getChild('dstart').setDate(startCloned);
  this.getChild('dend').setDate(endCloned);
  if (!startCloned.date.getHours()){
    dom.getElement('bm-ui-form-allday').checked = true;
  } else {
    this.getChild('tstart').setTime(startCloned);
    this.getChild('tend').setTime(endCloned);
    dom.getElement('bm-ui-form-allday').checked = false;
  }
  this.onDStartChange_();
  this.onDEndChange_();
}


/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.setDTStart = function(date) {
  this.getChild('dstart').setDate(date);
  if (!this.getModel().states.allday) {
    this.getChild('tstart').setTime(date);
  }
};

/**
 * Handle Resize grid
 * 
 * @param {goog.events.Event=} opt_evt
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.handleResize_ = function(opt_evt) {
  this.resize_();
};

/**
 * Resize grid
 * 
 * @param {goog.events.Event=} opt_evt
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.resize_ = function() {
  var attendees = this.getDomHelper().getElement("bm-ui-form-fieldset-attendees-container");
  var size = this.sizeMonitor_.getSize();
  var height = size.height - attendees.offsetTop - 5;
  attendees.style.height = height + 'px';

  var mainform = this.getDomHelper().getElement("bm-ui-mainform");
  var size = this.sizeMonitor_.getSize();
  var height = size.height - mainform.offsetTop - 5;
  mainform.style.height = height + 'px';
};

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.setDTEnd = function(date) {
  this.getChild('dend').setDate(date);
  if (!this.getModel().states.allday) {
    this.getChild('tend').setTime(date);
  }
};
/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.setModelValues_ = function() {
  if (this.isInDocument() && this.getModel()) {
    var model = this.getModel();
    this.getChild('toolbar').getChild('delete').setVisible(model.states.removable);
    this.getChild('toolbar').getChild('others').setVisible(model.states.updating);
    this.getChild('toolbar').getChild('others').getMenu().getChild('duplicate-occurrence').setVisible(model.states.exception);

    this.setFormValue_('title', model.summary);
    this.getChild('dstart').setDate(model.dtstart);
    if (!model.states.allday) {
      this.getChild('tstart').setValue(this.formatter.time.format(model.dtstart));
      this.getChild('tend').setValue(this.formatter.time.format(model.dtend));
      this.getChild('dend').setDate(model.dtend);
    } else {
      var e = model.dtend.clone();
      e.add(new goog.date.Interval(0, 0, -1));
      this.getChild('dend').setDate(e);
    }

    this.setFormValue_('allday', model.states.allday);
    this.onAllDayChange_();
    
    if (!model.states.master){
      var acceptCountersLabel = this.getDomHelper().getElement('bm-ui-form-accept-counters-label');
      var acceptCountersLabel2 = this.getDomHelper().getElement('bm-ui-form-accept-counters-label2');
      acceptCountersLabel.style.visibility = 'hidden';
      acceptCountersLabel2.style.visibility = 'hidden';
    } else {
      var acceptCountersElement = this.getDomHelper().getElement('bm-ui-form-accept-counters');
      if (model.states.master && model.states.main){
        if (typeof model.acceptCounters !== 'undefined'){
          this.setFormValue_('accept-counters', model.acceptCounters);
        } else {
          this.setFormValue_('accept-counters', true);
        }
      } else {
          acceptCountersElement.disabled = true;
      }
    }

    this.setFormValue_('location', model.location);

    this.setFormValue_('url', model.url);
    if( model.url) {
      var url = model.url;
      if (!goog.Uri.parse(url).hasScheme()) {
        url = 'http://' + url;
      }
      this.getDomHelper().getElement('bm-ui-form-vevent-url').href = url;
      goog.style.setElementShown( this.getDomHelper().getElement('bm-ui-form-vevent-url'), true);
    } else {
      this.getDomHelper().getElement('bm-ui-form-vevent-url').href = "#";
      goog.style.setElementShown( this.getDomHelper().getElement('bm-ui-form-vevent-url'), false);
    }

    this.updateAttendeeBox_();
    this.updateReminderForm_();

    this.getChild('tags').setValue(model.tags);
    this.getChild('tags').setEnabled(model.states.main)
    
    this.editor_.setValue(model.description);

    if (model.states.busy) {
      this.setFormValue_('opacity-busy', true);
    } else {
      this.setFormValue_('opacity-free', true);
    }
    if (model.states.private_) {
      this.setFormValue_('privacy-private', true);
    } else {
      this.setFormValue_('privacy-public', true);
    }

    if (!model.states.repeat) {
      this.setFormValue_('repeat', 'NONE');
      this.onRepeatChange_(null);
    } else {
      this.setFormValue_('repeat', model.rrule.freq);
      this.setFormValue_('repeat-periodicity', model.rrule.interval);
      if (model.states.forever) {
        this.setFormValue_('repeat-end-never', true);
      } else {
        if (model.states.count) {
          this.setFormValue_('repeat-count', true);
          this.getChild('count').setValue(model.rrule.count);
        } else {
          this.setFormValue_('repeat-end-on', true);
          this.getChild('until').setDate(model.rrule.until);
        }
      }
      if (model.rrule.freq == 'WEEKLY') {
        goog.array.forEach(goog.i18n.DateTimeSymbols_en.WEEKDAYS, function(day) {
          this.setFormValue_('repeat-days-' + day, !!goog.array.find(model.rrule.byday, goog.partial(this.findByDay_,
              day)));
        }, this);

      } else if (model.rrule.freq != 'DAILY') {
        if (model.rrule.byday && model.rrule.byday.length > 0) {
          this.setFormValue_("repeat-by-day", true);
        }
      }
      this.onRepeatChange_(null);
      this.onRepeatEndOnChange_(null);
    }
    goog.style.setElementShown(goog.dom.getElement('bm-ui-form-tab-repeat'), model.states.repeatable);
    this.getChild('freebusy').setModel(model);
    this.getChild('freebusy').initToolbar();
    this.getChild('freebusy').initGrid();

    if (model.states.meeting) {
      goog.array.forEach(model.attendees, function(attendee) {
        this.addAttendee_(attendee);
      }, this);
      this.onAttendeeChange_();
    }

    this.getChild('reminder').forEachChild(function(child) {
      if (this.getChildCount() > 1) {
        this.removeFormField(child);
      }
    }, this);

    model.alarm = model.alarm || [];
    goog.array.forEach(model.alarm, function(value) {
      this.addReminder_(value);
    }, this);
    model.initalContainer = this.ctx.params.get('container');

    if (model.states.master) {
      // BM-9075
      this.setFormValue_('owner', model.calendar);
      this.onOwnerChange_();
      if (!model.states.exception) {
        var el = this.getDomHelper().getElement('bm-ui-form-owner');
        goog.style.setElementShown(el, true);
      } else {
        var el = this.getDomHelper().getElement('bm-ui-form-owner');
        goog.style.setElementShown(el, false);
        el = this.getDomHelper().getElement('bm-ui-form-owner-ro');
        el.innerHTML = model.organizer['commonName'] || model.organizer['mailto'] || '';
      }
      var at = this.getElementByClass(goog.getCssName('bm-ui-form-attendee-input'))
      goog.style.setElementShown(at, true);
    } else {
      this.onOwnerChange_();
      var el = this.getDomHelper().getElement('bm-ui-form-owner');
      goog.style.setElementShown(el, false);
      el = this.getElementByClass(goog.getCssName('bm-ui-form-attendee-input'))
      goog.style.setElementShown(el, false);
      var el = this.getDomHelper().getElement('bm-ui-form-owner-ro');
      el.innerHTML = model.organizer['commonName'] || model.organizer['mailto'] || '';
      /** @meaning calendar.privateChanges.title */
      var MSG_WARN_MASTER = goog.getMsg('Those changes will remain private');
      this.addWarn_('master', [ 'master' ], MSG_WARN_MASTER);
    }

    if (model.states.meeting && model.states.master){
      if (this.counters.length > 0){
        this.getChild('counters').setModel(model);
        this.getChild('counters').initialDtstart = model.dtstart.clone();
        this.getChild('counters').initialDtend = model.dtend.clone();
        this.getChild('counters').initToolbar();
        this.getChild('counters').initGrid();
        this.getChild('counters').setCounters(this.counters);
        this.getChild('counters').setAttendees(model.attendees);
        var el = this.getDomHelper().getElement('bm-ui-form-tab-counters');
        goog.style.setElementShown(el, true);
        this.getChild('details').setSelectedTabIndex(0);
        this.switchTabById_('bm-ui-form-tab-counters');
      }
    }
  }  

};


net.bluemind.calendar.vevent.ui.Form.prototype.updateReminderForm_ = function() {
  var elem = this.getElementByClass(goog.getCssName('bm-ui-form-reminder'))
  var userHasEmail = this.ctx.user['value']['routing'] != 'none';
  var canSetReminder = this.getModel().states.defaultCalendar && userHasEmail;
  goog.style.setElementShown(elem, canSetReminder);
  if (!canSetReminder) {
    this.getModel().alarm = [];
  }
};

net.bluemind.calendar.vevent.ui.Form.prototype.updateAttendeeBox_ = function() {
  // do not allow attendees, if the event is not part of the default calendar
  var attendeesEl = this.getElementByClass(goog.getCssName('bm-ui-form-fieldset-attendees-container'))
  var userHasEmail = this.ctx.user['value']['routing'] != 'none';
  var canCreateMeeting = this.getModel().states.defaultCalendar && userHasEmail;
  goog.style.setElementShown(attendeesEl, canCreateMeeting);
  if (!canCreateMeeting) {
    this.getModel().attendees = [];
    var element = this.getElementByClass(goog.getCssName('bm-ui-form-attendee'));
    this.getDomHelper().removeChildren(element);
    /** @meaning calendar.event.agenda */
    var MSG_EVENT_OWNER = goog.getMsg('Calendar');
    this.getDomHelper().getElement('owner-label').innerHTML = MSG_EVENT_OWNER;
  } else {
    /** @meaning calendar.event.organizer */
    var MSG_EVENT_ORGANIZER = goog.getMsg('Organizer');
    this.getDomHelper().getElement('owner-label').innerHTML = MSG_EVENT_ORGANIZER;
  }
}

/**
 * Method to use in goog.array.find to find the element matching the given day.
 * 
 * @param {string} day
 * @param {Object} element
 * @return {boolean}
 */
net.bluemind.calendar.vevent.ui.Form.prototype.findByDay_ = function(day, element) {
  return (element.day == day);
};

/**
 * @param {string} id Element id
 * @return {string | boolean | Array.<string>} element value
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.getFormValue_ = function(id) {
  var element = this.getDomHelper().getElement('bm-ui-form-' + id);
  var value = goog.dom.forms.getValue(element);
  switch (element.type.toLowerCase()) {
  case 'checkbox':
  case 'radio':
    return !!value;
    break;
  default:
    return value;
  }
};

/**
 * @param {string} id Element id param {string | boolean | Array.<string>}
 * value element value
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.setFormValue_ = function(id, value) {
var element = this.getDomHelper().getElement('bm-ui-form-' + id);
  goog.dom.forms.setValue(element, value);
  if (element.type.toLowerCase() == 'checkbox') {
    goog.dom.classlist.enable(element.parentNode, goog.getCssName('active'), !!value);
  }
};

/**
 * Switch tab
 * 
 * @param {Element} tabSelected tab to focus.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.switchTab_ = function(tabSelected) {
  this.switchTabById_(tabSelected.getElement().id);
};

net.bluemind.calendar.vevent.ui.Form.prototype.switchTabById_ = function(tabId) {
  switch (tabId) {
    case 'bm-ui-form-tab-repeat':
      goog.style.showElement(goog.dom.getElement('bm-ui-form-fieldset-details'), false);
      goog.style.showElement(goog.dom.getElement('bm-ui-form-fieldset-repeat'), true);
      this.getChild('counters').setVisible(false);
      this.getChild('freebusy').setVisible(false);
      break;
    case 'bm-ui-form-tab-freebusy':
      goog.style.showElement(goog.dom.getElement('bm-ui-form-fieldset-details'), false);
      goog.style.showElement(goog.dom.getElement('bm-ui-form-fieldset-repeat'), false);
      this.getChild('counters').setVisible(false);
      this.getChild('freebusy').setVisible(true);
      break;
    case 'bm-ui-form-tab-counters':
      this.getChild('freebusy').setVisible(false);
      goog.style.showElement(goog.dom.getElement('bm-ui-form-fieldset-details'), false);
      goog.style.showElement(goog.dom.getElement('bm-ui-form-fieldset-repeat'), false);
      this.getChild('counters').setVisible(true);
      break;
    case 'bm-ui-form-tab-details':
    default:
      goog.style.showElement(goog.dom.getElement('bm-ui-form-fieldset-repeat'), false);
      goog.style.showElement(goog.dom.getElement('bm-ui-form-fieldset-details'), true);
      this.getChild('freebusy').setVisible(false);
      this.getChild('counters').setVisible(false);
    }
}

/**
 * @return {goog.events.Event} e Change event.
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onDStartChange_ = function(e) {
  if (e) e.stopPropagation();
  var model = this.getModel();
  var date = this.getChild('dstart').getDate();

  if (!date) {
    this.getChild('dstart').setDate(model.dtstart);
  } else {
    var old = model.dtstart.clone();
    model.dtstart.setDatePart(date);

    if (model.states.repeat) {
      if (model.rrule.freq != 'DAILY') {
        this.autoSetRepeatDays_(old);
      }
      if (goog.dom.forms.getValue(goog.dom.getElement('bm-ui-form-repeat-end-on'))) {
        this.autoSetEndRepeat_();
      }
      this.autoSetRepeatSentence_();
    }

    this.onDTStartChange_(old, model.dtstart);
  }
};

/**
 * @return {goog.events.Event} e Change event.
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onTStartChange_ = function(e) {
  if (e){
    e.stopPropagation();
  }
  var model = this.getModel();
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
};
/**
 * @return {goog.events.Event} e Change event.
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onDEndChange_ = function(e) {
  if (e){
    e.stopPropagation();
  }
  var model = this.getModel();
  var date = this.getChild('dend').getDate();
  if (!date) {
    /** @meaning calendar.form.error.date */
    var MSG_DATE_FORMAT_ERROR = goog.getMsg('Invalid date format');
    this.addError_('dates', [], MSG_DATE_FORMAT_ERROR);
    date = model.dtend.clone();
    if (model.states.allday || !date.date.getHours()) {
      date.add(new goog.date.Interval(goog.date.Interval.DAYS, -1));
    }
    this.getChild('dend').setDate(date)
  } else {
    if (model.states.allday || !date.date.getHours()) {
      date.add(new goog.date.Interval(0, 0, 1));
    }
    var old = model.dtend.clone();
    model.dtend.setDatePart(date);
    this.onDTEndChange_(old, model.dtend);
  }
};

/**
 * @return {goog.events.Event} e Change event.
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onTEndChange_ = function(e) {
  e.stopPropagation();
  var model = this.getModel();
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
};

/**
 * Prevent max data in editor
 * 
 * @return {goog.events.Event} e Change event.
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onEditorChange_ = function(e) {
  var value = this.editor_.getValue();
  if (value.length > (1024 * 1024)) {
    this.editor_.setValue(this.getModel().description || '');
  } else {
    this.getModel().description = value;
    this.setFormActions_();
  }
};

/**
 * Show reminder fields
 * 
 * @param {{trigger:number, action:string}} value Reminder in seconds
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.addReminder_ = function(value) {
  function getDurationUnit(trigger) {
    var duration = trigger;
    var unit = 1;
    if (trigger == 0) {
      duration = 0;
      unit = 1;
    } else if (trigger % 86400 == 0) {
      duration = trigger / 86400;
      unit = 86400;
    } else if (trigger % 3600 == 0) {
      duration = trigger / 3600;
      unit = 3600;
    } else if (trigger % 60 == 0) {
      duration = trigger / 60;
      unit = 60;
    }
    return { duration: duration, unit: unit };
  }

  function createTriggerInput(duration) {
    var control = new goog.ui.LabelInput();
    control.createDom();
    control.setId('field');
    control.setValue('' + duration);
    return control;
  }

  function createTriggerSelect(unit) {
    var control = new goog.ui.Select();
    control.addClassName(goog.getCssName('goog-button-base'));
    control.addClassName(goog.getCssName('goog-select'));
    /** @meaning general.seconds */
    var MSG_SECONDS = goog.getMsg('seconds');
    control.addItem(new goog.ui.MenuItem(MSG_SECONDS, 1));
    /** @meaning general.minutes */
    var MSG_MINUTES = goog.getMsg('minutes');
    control.addItem(new goog.ui.MenuItem(MSG_MINUTES, 60));
    /** @meaning general.hours */
    var MSG_HOURS = goog.getMsg('hours');
    control.addItem(new goog.ui.MenuItem(MSG_HOURS, 3600));
    /** @meaning general.days */
    var MSG_DAYS = goog.getMsg('days');
    control.addItem(new goog.ui.MenuItem(MSG_DAYS, 86400));
    control.setId('unit');
    control.setValue(unit);
    return control;
  }

  function createActionSelect(action) {
    var control = new goog.ui.Select();
    control.addClassName(goog.getCssName('goog-button-base'));
    control.addClassName(goog.getCssName('goog-select'));
    control.setId('action');

    var defaultValue = net.bluemind.calendar.vevent.defaultValues.action;
    /** @meaning calendar.reminder.action.Email */
    var MSG_EMAIL = goog.getMsg('Email');
    /** @meaning calendar.reminder.action.Display */
    var MSG_DISPLAY = goog.getMsg('Display');
    var values = [
      { value: 'Email', msg: MSG_EMAIL },
      { value: defaultValue, msg: MSG_DISPLAY }
    ];
    values.forEach(function(item) {
      control.addItem(new goog.ui.MenuItem(item.msg, item.value));
    });
    control.setValue(action);
    if (control.getValue() === null) {
      control.setValue(defaultValue);
    }
    return control;
  }

  var durationvalue = getDurationUnit(value.trigger);
  var triggerInput = createTriggerInput(durationvalue.duration);
  var ih = new goog.events.InputHandler(triggerInput.getElement());
  var container = new goog.ui.Component();
  container.registerDisposable(ih);
  container.addChild(triggerInput, true);
  container.addChild(createTriggerSelect(durationvalue.unit), true);
  container.addChild(createActionSelect(value.action), true);

  var control = new goog.ui.Button('X', goog.ui.FlatButtonRenderer.getInstance());
  container.addChild(control, true);

  this.getChild('reminder').addChild(container, true);
  this.updateModel_();

  this.getHandler().listen(
    control,
    goog.ui.Component.EventType.ACTION,
    this.removeReminder_
  );

  this.getHandler().listen(
    ih,
    goog.events.InputHandler.EventType.INPUT,
    this.updateModel_
  );
  this.getHandler().listen(
    container.getChild('unit'),
    goog.ui.Component.EventType.CHANGE,
    this.updateModel_
  );
  this.getHandler().listen(
    container.getChild('action'),
    goog.ui.Component.EventType.CHANGE,
    this.updateModel_
  );
};

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.updateModel_ = function() {
  this.getModel().alarm = []
  this.getChild('reminder').forEachChild(function(child) {
    if (child.getChild('field') && child.getChild('unit') && child.getChild('action')) {
      var duration = goog.string.toNumber(child.getChild('field').getValue()) * child.getChild('unit').getValue();
      var action = child.getChild('action').getValue();
      this.getModel().alarm.push({
        trigger : duration,
        action : action
      });
    }
  }, this);
  this.setFormActions_();

}

/**
 * hide reminder fields
 * 
 * @param {goog.events.BrowserEvent} e Event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.removeReminder_ = function(e) {
  var control = e.target.getParent();
  this.getHandler().unlisten((e.target), goog.ui.Component.EventType.ACTION);
  this.getChild('reminder').removeChild(control).dispose();
  this.updateModel_();
};

/**
 * Apply check and modification on periodicity change
 * 
 * @param {goog.events.Event} e Change event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onRepeatPeriodChange_ = function(e) {
  var input = goog.dom.getElement('bm-ui-form-repeat-periodicity');
  var value = goog.dom.forms.getValue(input);
  this.getModel().rrule.interval = value
  this.setFormActions_();

  if (this.checkRepeat_()) {
    this.autoSetRepeatSentence_();
  }
};

/**
 * Apply check and modification when the event end repeat status change
 * 
 * @param {goog.events.Event} e Change event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onRepeatEndOnChange_ = function(e) {
  var model = this.getModel();
  if (model.states.repeat) {
    model.states.forever = this.getFormValue_('repeat-end-never');
    if (model.states.forever) {
      this.getChild('until').getElement().disabled = true;
      this.getModel().rrule.until = null;
      this.getChild('until').setDate(null);
      this.getChild('count').getElement().disabled = true;
      this.getModel().rrule.count = null;
      this.getChild('count').setValue(null);
      this.autoSetRepeatSentence_();
    } else {
      model.states.count = !!this.getFormValue_('repeat-count');
      if (model.states.count) {
        this.getChild('until').getElement().disabled = true;
        this.getModel().rrule.until = null;
        this.getChild('until').setDate(null);
        this.getChild('count').getElement().disabled = false;
        this.getChild('count').setValue(model.rrule.count ? model.rrule.count : 10);
        this.onCountChange_();
      } else { // until date
        this.getChild('until').getElement().disabled = false;
        this.getChild('count').getElement().disabled = true;
        this.getModel().rrule.count = null;
        this.getChild('count').setValue(null);
        this.autoSetEndRepeat_();
      }
    }
    this.setFormActions_();

  }

};

/**
 * Apply check and modification when the event repeat count changes
 * 
 * @param {goog.events.Event} e Change event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onCountChange_ = function(e) {
  var input = goog.dom.getElement('bm-ui-form-repeat-count-value');
  var value = parseInt(goog.dom.forms.getValue(input), 10);
  this.getModel().rrule.count = value;
  this.getModel().rrule.until = null;
  this.setFormActions_();

  if (this.checkRepeat_()) {
    this.autoSetRepeatSentence_();
  }
};

/**
 * Apply check and modification when the event end repeat status change
 * 
 * @param {goog.events.Event} e Change event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onUntilChange_ = function(e) {
  var date = this.getChild('until').getDate();
  var rrule = this.getModel().rrule;
  if (date) {
    rrule.until = this.getModel().dtstart.clone();
    rrule.until.setDatePart(date);
  } else {
    rrule.until = null;
  }
  rrule.count = null;
  this.setFormActions_();

  if (this.checkRepeat_()) {
    this.autoSetRepeatSentence_();
  }
};

/**
 * Apply check and modification when the repeat kind change
 * 
 * @param {goog.events.Event} e Change event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onRepeatChange_ = function(e) {
  var model = this.getModel();
  var select = goog.dom.getElement('bm-ui-form-repeat');
  var value = goog.dom.forms.getValue(select);
  var period = goog.dom.getElement('bm-ui-form-fieldset-period-block');
  var end = goog.dom.getElement('bm-ui-form-fieldset-end-block');
  var days = goog.dom.getElement('bm-ui-form-repeat-days-block');
  var month = goog.dom.getElement('bm-ui-form-repeat-by-block');

  if (value && value != 'NONE') {
    model.states.forever = this.getFormValue_('repeat-end-never');
    
    model.rrule = {
      freq : value,
      until : null, 
      interval : parseInt(this.getFormValue_('repeat-periodicity'), 10) || 1,
      byday : (model.rrule ? model.rrule.byday : []),
      count : null 
    };
    if (model.states.forever) {
      this.getChild('until').getElement().disabled = true;
      this.getChild('count').getElement().disabled = true;
      this.getChild('until').setDate(null);
    } else if (model.states.count) {
      this.getChild('until').getElement().disabled = true;
      this.getChild('until').setDate(null);
      this.getChild('count').getElement().disabled = false;
      model.rrule.count = parseInt(this.getChild('count').getValue(), 10);
    } else {
      this.getChild('until').getElement().disabled = false;
      var date = this.getChild('until').getDate();
      model.rrule.until = this.getModel().dtstart.clone();
      model.rrule.until.setDatePart(date);
      this.getChild('until').setDate(model.rrule.until);
      this.getChild('count').getElement().disabled = true;
      this.getChild('count').setValue(null);
    }
    goog.style.setElementShown(end, true);
    goog.style.setElementShown(period, true);

    this.getChild('until').getElement().disabled = model.states.forever || model.states.count;
    this.getChild('until').setDate(model.rrule.until);
    this.getChild('count').getElement().disabled = !model.states.count;
    this.getChild('count').setValue(model.rrule.count);

  } else {
    goog.style.setElementShown(end, false);
    goog.style.setElementShown(period, false);
    goog.style.setElementShown(days, false);
    goog.style.setElementShown(month, false);
    model.rrule = null;
    model.states.forever = false;
  }

  if (value == 'WEEKLY') {
    this.autoSetRepeatDays_();
    goog.style.setElementShown(days, true);
  } else if (value != 'NONE') {
    this.resetDayRepeat_();
    goog.style.setElementShown(days, false);
  }
  if (value == 'MONTHLY' || value == 'YEARLY') {
    this.autoSetRepeatDays_();
    goog.style.setElementShown(month, true);
  } else if (value != 'NONE') {
    goog.style.setElementShown(month, false);
  }

  model.states.repeat = (model.rrule != null);
  this.setFormActions_();

  if (this.checkRepeat_()) {
    this.autoSetRepeatSentence_();
  }
};


/**
 * Apply check and modification when the all day status change and set reminders
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onAllDayChangeAndReminderUpdate_ = function() {
  this.onAllDayChange_();

  var model = this.getModel();
  var allday = model.states.allday;
  this.alarm_.set(!allday ? 'allday' : 'inday', model.alarm);


  this.getChild('reminder').forEachChild(function(child) {
    child.dispose();
  }, this);
  goog.array.forEach(this.alarm_.get(allday ? 'allday' : 'inday'), function(value) {
    this.addReminder_(value);
  }, this);

};

net.bluemind.calendar.vevent.ui.Form.prototype.onAcceptCountersUpdate_ = function() {
  var dom = this.getDomHelper();
  
  var model = this.getModel();
  var checkbox = dom.getElement('bm-ui-form-accept-counters');
  var value = goog.dom.forms.getValue(checkbox);
  model.acceptCounters = !!value;

  goog.dom.classlist.enable(checkbox.parentNode, goog.getCssName('active'), model.acceptCounters);
}

/**
 * Apply check and modification when the all day status change
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onAllDayChange_ = function() {
  var dom = this.getDomHelper();

  var checkbox = dom.getElement('bm-ui-form-allday');
  var value = goog.dom.forms.getValue(checkbox);
  var model = this.getModel();

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
  
  this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, true);

  this.checkDate_();
};

/**
 * Apply check and modification when the repeat days change
 * 
 * @param {goog.events.Event} e Change event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onRepeatDaysChange_ = function(e) {
  this.getModel().rrule.byday = [];
  for (var i = 0; i < 7; i++) {
    var day = goog.i18n.DateTimeSymbols_en.WEEKDAYS[i];
    var el = goog.dom.getElement('bm-ui-form-repeat-days-' + day);
    if (goog.dom.forms.getValue(el)) {
      this.getModel().rrule.byday.push({
        day : day,
        offset : 0
      });
      goog.dom.classlist.add(el.parentNode, goog.getCssName('active'));
    } else {
      goog.dom.classlist.remove(el.parentNode, goog.getCssName('active'));
    }
  }
  this.setFormActions_();

  if (this.checkRepeat_()) {
    this.autoSetRepeatSentence_();
  }
};

/**
 * Apply check and modification when the repeat days change
 * 
 * @param {goog.events.Event} e Change event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onRepeatByChange_ = function(e) {
  var model = this.getModel();
  model.rrule.byday = [];
  model.rrule.bymonth = null;
  if (this.getFormValue_('repeat-by-day')) {
    var pos = Math.ceil(model.dtstart.getDate() / 7);
    if (pos == 5) {
      pos = -1;
    }
    var weekdays = goog.array.clone(goog.i18n.DateTimeSymbols_en.WEEKDAYS);
    var fdow = (goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK + 1) % 7;
    goog.array.rotate(weekdays, -fdow);
    var day = weekdays[model.dtstart.getWeekday()];
    model.rrule.byday = [ {
      day : day,
      offset : pos
    } ];
    if (model.rrule.freq == 'YEARLY') {
      model.rrule.bymonth = model.dtstart.getMonth();
    }
  }
  this.setFormActions_();

  if (this.checkRepeat_()) {
    this.autoSetRepeatSentence_();
  }
};

/**
 * Apply check and modification when the dateend or timend change
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onDTEndChange_ = function() {
  if (this.checkDate_()) {
    if (this.getModel().states.busy) {
      this.getChild('freebusy').checkAvailability();
    }
  }
  this.setFormActions_();
  this.getChild('freebusy').updateDummyEventOnFormUpdate(this.getModel().dtstart, this.getModel().dtend, false);
};

/**
 * Apply check and modification when the datebegin, or timebegin change
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onDTStartChange_ = function(old) {
  var e, model = this.getModel();

  this.adaptor.adjustDTend(model, old);
  if (!model.states.allday) {
    this.getChild('tend').setValue(this.formatter.time.format(model.dtend));
    e = model.dtend;
  } else {
    e = model.dtend.clone();
    e.add(new goog.date.Interval(goog.date.Interval.DAYS, -1));
  }
  this.getChild('dend').setDate(e)
  
  if (this.checkDate_()) {
    if (model.states.busy && bluemind.net.OnlineHandler.getInstance().isOnline()) {
      this.getChild('freebusy').checkAvailability();
    }
  }
  
  this.setFormActions_();
  this.checkRepeat_();
  this.getChild('freebusy').updateDummyEventOnFormUpdate(model.dtstart, model.dtend, false);
};

/**
 * Auto fill the end repeat
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.autoSetEndRepeat_ = function() {
  var model = this.getModel();
  if (!model.rrule.until) {
    var end = model.dtstart.clone();
    var period = model.rrule.interval;
    period = (period > 1) ? period : 1;
    var interval;
    switch (model.rrule.freq) {
    case 'DAILY':
      var occurrences = 5;
      interval = new goog.date.Interval(0, 0, period * occurrences);
      break;
    case 'WEEKLY':
      var occurrences = 53;
      interval = new goog.date.Interval(0, 0, 7 * period * occurrences);
      break;
    case 'MONTHLY':
      var occurrences = 12;
      interval = new goog.date.Interval(0, period * occurrences);
      break;
    case 'YEARLY':
      var occurrences = 5;
      interval = new goog.date.Interval(period * occurrences);
      break;
    }
    end.add(interval);
    model.rrule.until = end;
    this.getChild('until').setDate(end);
  } else {
    var date = model.rrule.until;
    model.rrule.until = model.dtstart.clone();
    model.rrule.until.setDatePart(date);
  }
  this.setFormActions_();

};

/**
 * Auto set the repetition day
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.autoSetRepeatDays_ = function(opt_old) {
  var model = this.getModel();
  var weekdays = goog.array.clone(goog.i18n.DateTimeSymbols_en.WEEKDAYS);
  var fdow = (goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK + 1) % 7;
  goog.array.rotate(weekdays, -fdow);
  if (!model.rrule.byday) {
    model.rrule.byday = [];
  }
  if (!opt_old || !goog.date.isSameDay(opt_old, model.dtstart)) {
    var day = weekdays[model.dtstart.getWeekday()];
    if (model.rrule.freq == 'WEEKLY') {
      var e = this.getDomHelper().getElement('bm-ui-form-repeat-days-' + day);
      goog.dom.classlist.add(e.parentNode, goog.getCssName('active'));
      var checked = !!goog.dom.forms.getValue(e);
      if (!checked) model.rrule.byday.push({
        day : day,
        offset : 0
      });
      goog.dom.forms.setValue(e, true)
      if (!checked && opt_old) {
        day = weekdays[opt_old.getWeekday()];
        e = this.getDomHelper().getElement('bm-ui-form-repeat-days-' + day);
        checked = !!goog.dom.forms.getValue(e);
        if (checked) goog.array.removeIf(model.rrule.byday, goog.partial(this.findByDay_, day));
        goog.dom.forms.setValue(e, false)
        goog.dom.classlist.remove(e.parentNode, goog.getCssName('active'));
      }
    } else if (model.rrule.freq == 'MONTHLY' || model.rrule.freq == 'YEARLY') {
      if (this.getFormValue_('repeat-by-day')) {
        model.rrule.byday = [];
        var pos = Math.ceil(model.dtstart.getDate() / 7);
        if (pos == 5) {
          pos = -1;
        }
        model.rrule.byday = [ {
          day : day,
          offset : pos
        } ];
        if (model.rrule.freq == 'YEARLY') {
          model.rrule.bymonth = model.dtstart.getMonth();
        }
      }

    }
  }
};

/**
 * Auto set the repeat
 * 
 * @param {string} opt_error Optional error message.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.autoSetRepeatSentence_ = function(opt_error) {
  if (this.getModel().states.repeat) {
    var rrule = this.getModel().rrule;
    var dtstart = this.getModel().dtstart;
    var data = {
      rrule : {
        freq : rrule.freq,
        interval : rrule.interval
      },
      error : opt_error
    };

    if (rrule.until) {
      data.rrule.until = this.formatter.date.format(rrule.until);
    }

    if (rrule.count) {
      data.rrule.count = rrule.count;
    }

    data.rrule.byday = [];
    if (rrule.byday.length > 0) {

      var weekdays = goog.array.clone(goog.i18n.DateTimeSymbols_en.STANDALONEWEEKDAYS);
      var i18n = goog.array.clone(goog.i18n.DateTimeSymbols.STANDALONEWEEKDAYS);
      for (var i = 0; i < rrule.byday.length; i++) {
        var day = rrule.byday[i].day;
        var offset = rrule.byday[i].offset;
        var index = goog.array.indexOf(goog.i18n.DateTimeSymbols_en.STANDALONEWEEKDAYS, day);
        if (offset == 0) {
          data.rrule.byday.push(i18n[index]);
        } else if (!goog.isDefAndNotNull(rrule.bymonth)) {
          data.rrule.byday.push({
            day : i18n[index],
            offset : offset
          });
        } else {
          data.rrule.byday.push({
            day : i18n[index],
            month : goog.i18n.DateTimeSymbols.STANDALONEMONTHS[rrule.bymonth],
            offset : offset
          });
        }
      }
    } else {
      switch (rrule.freq) {
      case 'MONTHLY':
        data.rrule.bydate = dtstart.getDate();
        break;
      case 'YEARLY':
        var format = goog.i18n.DateTimeSymbols.DATEFORMATS[goog.i18n.DateTimeFormat.Format.LONG_DATE].replace(/[y,]/g,
            '');
        data.rrule.bydate = new goog.i18n.DateTimeFormat(format).format(dtstart);
        break;
      }
    }
    this.getDomHelper().getElement('bm-ui-form-repeat-sentence').innerHTML = net.bluemind.calendar.vevent.templates
        .rrule(data);
  } else {
    this.getDomHelper().getElement('bm-ui-form-repeat-sentence').innerHTML = '';
  }

};

/**
 * Reset repeat days checkbox
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.resetDayRepeat_ = function() {
  for (var i = 0; i < 7; i++) {
    var day = goog.i18n.DateTimeSymbols_en.WEEKDAYS[i];
    var elem = goog.dom.getElement('bm-ui-form-repeat-days-' + day);
    goog.dom.forms.setValue(elem, false);
    goog.dom.classlist.remove(goog.dom.getElement('bm-ui-form-repeat-days-' + day).parentNode, goog
        .getCssName('active'));
  }
  this.getModel().rrule.byday = [];
  this.getModel().rrule.bymonth = null;
};

/**
 * Check date validity
 * 
 * @return {boolean} Is Date valid.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.checkDate_ = function() {
  this.resetError_('dates');
  var model = this.getModel();
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

  this.checkIfInThePast_();

  return ret;
};

/**
 * @param {boolean} availability attendee availability.
 */
net.bluemind.calendar.vevent.ui.Form.prototype.availabilityWarn = function(availability) {
  if (this.getModel().states.busy && !availability) {
    /** @meaning calendar.form.warning.availability */
    var MSG_AVAILABILITY_WARN = goog.getMsg('Not all attendees are available.');
    var fields = new Array();
    fields.push(this.getChild('attendee-autocomplete').getElement());
    this.addWarn_('attendees', fields, MSG_AVAILABILITY_WARN);
  } else {
    this.resetWarn_('attendees');
  }
};

/**
 * Check the title validity
 * 
 * @return {boolean} Is title valid.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.checkTitle_ = function() {
  var ret = true;
  this.resetError_('title');
  if (goog.dom.forms.getValue(goog.dom.getElement('bm-ui-form-title')) == '') {
    /** @meaning calendar.form.error.title */
    var MSG_EMPTY_TITLE = goog.getMsg('Title cannot be empty');
    this.addError_('title', 'bm-ui-form-title', MSG_EMPTY_TITLE);
    ret = false;
  }
  return ret;
};

/**
 * Check the details validity
 * 
 * @return {boolean} Is details block valid.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.checkDetails_ = function() {
  var ret = true;
  this.resetDetailsError_();
  var element = goog.dom.getElement('bm-ui-form-reminder');
  if ((element != null) && !(goog.dom.forms.getValue(element) >= -1)) {
    this.addDetailsError_('bm-ui-form-reminder');
    ret = false;
  }

  return ret;
};

/**
 * Check attendees
 * 
 * @private
 * @return {boolean} check attendees.
 */
net.bluemind.calendar.vevent.ui.Form.prototype.checkAttendees_ = function() {
  var data = goog.dom.forms.getFormDataMap(goog.dom.getElement('bm-ui-form'));
  var attendees = data.get('attendees');
  var ret = false;
  var model = this.getModel();
  if (attendees) {
    var checkbox = goog.dom.getElement('bm-ui-form-allday');
    var allday = !!goog.dom.forms.getValue(checkbox);
    var evtBegin = model.dtstart;
    var evtEnd = model.dtend;
    if (allday) {
      evtBegin.setHours(0);
      evtBegin.setMinutes(0);
      evtEnd.setHours(0);
      evtEnd.setMinutes(0);
      evtEnd.add(new goog.date.Interval(0, 0, 1));
    }
    var evtDuration = (evtEnd.getTime() - evtBegin.getTime()) / 1000;

    var firstDayOfWeek = evtBegin.clone();
    firstDayOfWeek.setHours(0);
    firstDayOfWeek.setMinutes(0);
    firstDayOfWeek.setSeconds(0);
    firstDayOfWeek.setMilliseconds(0);
    firstDayOfWeek.add(new goog.date.Interval(goog.date.Interval.DAYS, -firstDayOfWeek.getWeekday()));

    var shortWeekDays = [ 'mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun' ];

    ret = true;
    var failDuration = new Array();
    var failWorkingHours = new Array();

    goog.iter.forEach(this.availabilities_, function(c, i) {
      // Check resource working days, day start, day end.
      var workingDays = goog.object.get(c, 'workingDays').split(',');
      var dayStart = goog.object.get(c, 'dayStart');
      var minDuration = goog.object.get(c, 'minDuration');
      var dsh = dayStart;
      var dsm = 0;
      if (goog.string.endsWith(dayStart, '.5')) {
        dsh = dayStart.split('.')[0];
        dsm = 30;
      }
      var dayEnd = goog.object.get(c, 'dayEnd');
      var deh = dayEnd;
      var dem = 0;
      if (goog.string.endsWith(dayEnd, '.5')) {
        deh = dayEnd.split('.')[0];
        dem = 30;
      }
      var available = false;
      for (var i = 0; i < workingDays.length; i++) {
        var d = workingDays[i];
        var idx = goog.array.indexOf(shortWeekDays, d);

        var testStartDay = firstDayOfWeek.clone();
        testStartDay.add(new goog.date.Interval(goog.date.Interval.DAYS, idx));

        var testEndDay = testStartDay.clone();
        if (allday) {
          testStartDay.setHours(0);
          testStartDay.setMinutes(0);
          testEndDay.setHours(0);
          testEndDay.setMinutes(0);
          testEndDay.add(new goog.date.Interval(0, 0, 1));
        } else {
          testStartDay.setHours(dsh);
          testStartDay.setMinutes(dsm);
          testEndDay.setHours(deh);
          testEndDay.setMinutes(dem);

        }
        if (goog.date.Date.compare(evtBegin, testStartDay) >= 0 && goog.date.Date.compare(evtEnd, testEndDay) <= 0) {
          available = true;
        }

      }

      if (!available) {
        failWorkingHours.push(c);
        ret = false;
      }

      if (evtDuration < minDuration) {
        failDuration.push(c);
        ret = false;
      }

    }, false, this);

    if (!ret) {
      // FIXME Resource overbooking.
    }

  } else {
    /** @meaning calendar.form.error.noAttendee */
    var MSG_NO_ATTENDEE = goog.getMsg('Event must contain attendee');
    var fields = new Array();
    fields.push(this.getChild('attendee-autocomplete').getElement());
    this.addError_('attendees', fields, MSG_NO_ATTENDEE);
    ret = false;
  }
  return ret;
};

/**
 * Check repeat block validity
 * 
 * @return {boolean} Is repeat block valid.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.checkRepeat_ = function() {
  this.resetRepeatError_();
  var model = this.getModel();
  var ret = true;
  if (model.states.repeat) {
    if (!model.states.forever) {
      if (model.states.count) {
        if (!(model.rrule.count > 0)) {
          this.addRepeatError_('bm-ui-form-repeat-count-value');
          ret = false;
        }
      } else {
        var end = model.rrule.until;
        var begin = model.dtstart;
        if (goog.date.Date.compare(end, begin) <= 0) {
          /** @meaning calendar.form.error.repeat.endBegin */
          var MSG_REPEAT_END_BEGIN = goog.getMsg('End of repetition cannot be before begin date');
          this.addRepeatError_(this.getChild('until').getElement(), MSG_REPEAT_END_BEGIN);
          ret = false;
        }
      }
    }
    if (!(model.rrule.interval > 0)) {
      this.addRepeatError_('bm-ui-form-repeat-periodicity');
      ret = false;
    }

    if (model.rrule.freq == 'WEEKLY') {
      if (model.rrule.byday.length == 0) {
        /** @meaning calendar.form.error.repeat.emptyDays */
        var MSG_EMPTY_REPEAT_DAYS = goog.getMsg('You must set at least one repeat day');
        this.addRepeatError_('bm-ui-form-repeat-days-container', MSG_EMPTY_REPEAT_DAYS);
        ret = false;
      }
    }
  }

  this.checkIfInThePast_();
  return ret;
};

/**
 * Check form validity
 * 
 * @param {boolean} checkAttendees check attendees flag.
 * @return {boolean} Is form valid.
 */
net.bluemind.calendar.vevent.ui.Form.prototype.checkForm_ = function(checkAttendees) {
  var ret = true;
  if (checkAttendees) {
    ret = (this.checkTitle_() && this.checkDate_() && this.checkRepeat_() && this.checkDetails_() && this
        .checkAttendees_());
  } else {
    ret = (this.checkTitle_() && this.checkDate_() && this.checkRepeat_() && this.checkDetails_());
  }
  return ret;
};

/**
 * Add a warning to the form
 * 
 * @param {string} module Module that own this error.
 * @param {string | Element | Array} fields Fields concerned by this error.
 * @param {string} opt_text Optional error text.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.addWarn_ = function(module, fields, opt_text) {
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
net.bluemind.calendar.vevent.ui.Form.prototype.addError_ = function(module, fields, opt_text) {
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

/**
 * Add an error to the form for the "repeat" module
 * 
 * @param {string | Element | Array} fields Fields concerned by this error.
 * @param {string} opt_text Optional error text.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.addRepeatError_ = function(fields, opt_text) {
  this.addError_('repeat', fields, opt_text);
  goog.dom.classlist.add(goog.dom.getElement('bm-ui-form-tab-repeat'), goog.getCssName('error'));
  /** @meaning calendar.form.error.default */
  var MSG_DEFAULT_ERROR = goog.getMsg('Invalid format');
  this.autoSetRepeatSentence_(MSG_DEFAULT_ERROR);
};

/**
 * Add an error to the form for the "details" module
 * 
 * @param {string | Element | Array} fields Fields concerned by this error.
 * @param {string} opt_text Optional error text.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.addDetailsError_ = function(fields, opt_text) {
  this.addError_('details', fields, opt_text);
  goog.dom.classlist.add(goog.dom.getElement('bm-ui-form-tab-details'), goog.getCssName('error'));
};

/**
 * Reset errors for a module
 * 
 * @param {string} module Module to clear.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.resetWarn_ = function(module) {
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
net.bluemind.calendar.vevent.ui.Form.prototype.resetError_ = function(module) {
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

/**
 * Reset errors for module repeat
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.resetRepeatError_ = function() {
  this.resetError_('repeat');
  goog.dom.classlist.remove(goog.dom.getElement('bm-ui-form-tab-repeat'), goog.getCssName('error'));
};

/**
 * Reset errors for module details
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.resetDetailsError_ = function() {
  this.resetError_('details');
  goog.dom.classlist.remove(goog.dom.getElement('bm-ui-form-tab-details'), goog.getCssName('error'));
};

/**
 * BJR58
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.onOwnerChange_ = function() {
  var model = this.getModel();
  var attendee = goog.array.find(model.attendees, function(attendee) {
    return attendee['dir'] && attendee['dir'] == model.organizer['dir'];
  });
  if (!attendee && model.organizer) {
    this.getChild('freebusy').removeAttendee({
      'commonName' : model.organizer['commonName'],
      'dir' : model.organizer['dir'],
      'mailto' : model.organizer['mailto']
    });
  }
  var owner = goog.dom.forms.getValue(goog.dom.getElement('bm-ui-form-owner'));
  if (model.states.master) {
    var calendar = goog.array.find(this.calendars, function(calendar) {
      return owner == calendar.uid;
    }, this);
    model.states.defaultCalendar = calendar.states.defaultCalendar;
    model.calendar = calendar.uid;

    model.organizer = {
      'commonName' : calendar.name,
      'dir' : calendar.dir
    }
  }
  if (model.organizer) {
    this.getChild('freebusy').addAttendees([ {
      'commonName' : model.organizer['commonName'],
      'dir' : model.organizer['dir'],
      'mailto' : model.organizer['mailto']
    } ]);
  }
  this.setFormActions_();

  this.updateAttendeeBox_();
  this.updateReminderForm_();

};

/**
 * check if event is in the past
 * 
 * @return {boolean} is event in the past.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.checkIfInThePast_ = function() {
  var warn = false;
  var model = this.getModel();
  var today = new net.bluemind.date.Date();
  if (!model.states.repeat) {
    if (!model.states.allday && model.dtend.getTime() < goog.now()) {
      warn = true;
    } else if (model.states.allday && !goog.date.isSameDay(model.dtend, today) && model.dtend.getTime() < goog.now()) {
      warn = true;
    }
  } else if (!model.rrule.until == null && !goog.date.isSameDay(model.rrule.until, today)
      && model.dtend.getTime() < goog.now()) {
    warn = true;
  }

  if (warn) {
    var fields = new Array();
    fields.push(this.getChild('dend').getElement());
    if (!model.states.repeat) {
      /** @meaning calendar.form.warning.eventInThPast */
      var MSG_EVENT_IN_THE_PAST = goog.getMsg('The event occurs in the past');
      this.addWarn_('dates', fields, MSG_EVENT_IN_THE_PAST);
    } else {
      /** @meaning calendar.form.warning.serieInThPast */
      var MSG_SERIE_IN_THE_PAST = goog.getMsg('All the events in the series occur in the past');
      this.addWarn_('dates', fields, MSG_SERIE_IN_THE_PAST);
    }
  } else {
    this.resetWarn_('dates');
  }

  return warn;
};

net.bluemind.calendar.vevent.ui.Form.defaultEmail_ = function(emails) {
  if (emails && emails.length > 0) {
    var d = goog.array.find(emails, function(e) {
      return goog.array.find(e['parameters'], function(p) {
        return p['label'] == "DEFAULT" && p['value'] == 'true';
      }) != null;
    });
    if (d) {
      return d['value'];
    } else {
      return emails[0]['value'];
    }
  } else {
    return null;
  }
}
/**
 * Add an attendee from autocomplete
 * 
 * @param {goog.event.Event} e AC event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.handleAddAttendee_ = function(e) {
  this.resetError_('attendees');
  this.getChild('attendee-autocomplete').clear();

  var fAttendees = null;
  if (e.row['cutype'] == 'Group') {
    var c = e.row['uri'].split('/')[0];
    var uid = e.row['uri'].split('/')[1];

    fAttendees = this.ctx.service('addressbooks').expandGroup(c, uid).then(function(vcards) {
      return goog.array.map(vcards, function(vcard) {
        if (vcard['mailto']){
          var cn = vcard['cn'];
          var uri = null;
          var dir = null;
        } else {
          var cn = vcard['value']['identification']['formatedName']['value'];
          var uri = vcard['container'] + '/' + vcard['uid'];
          var dir = vcard['value']['source'];
        }
        var ret = {
          'cutype' : 'Individual',
          'member' : e.row['uri'],
          'role' : 'RequiredParticipant',
          'partStatus' : 'NeedsAction',
          'rsvp' : true,
          'delTo' : null,
          'delFrom' : null,
          'sentBy' : null,
          'commonName' : cn,
          'lang' : null,
          'mailto' : null,
          'uri' : uri,
          'dir' : dir,
          'internal' : true
        };

        if (vcard['mailto']){
          ret['cutype'] = 'Individual';
          ret['mailto'] = vcard['mailto'];
        } else {
          ret['mailto'] = net.bluemind.calendar.vevent.ui.Form.defaultEmail_(vcard['value']['communications']['emails']);
          if (vcard['value']['communications']['emails'] && vcard['value']['communications']['emails'].length > 0) {
            ret['mailto'] = vcard['value']['communications']['emails'][0]['value'];
          }
          if (vcard['value']['identification']['photo'] && ret['dir'] && goog.string.startsWith(ret['dir'], 'bm://')) {
            ret['icon'] = '/api/directory/' + this.ctx.user['domainUid'] + '/_icon/'
                + encodeURIComponent(goog.string.removeAt(ret['dir'], 0, 5));
          }
        
          if (vcard['value']['kind'] == 'individual' && ret['dir'] && ret['dir'].indexOf('/resources/') >= 0) {
            ret['cutype'] = 'Resource';
          } else if (vcard['value']['kind'] == 'individual') {
            ret['cutype'] = 'Individual'
          } else if (vcard['value']['kind'] == 'group') {
            ret['cutype'] = 'Group'; // WOOT ?
          } else {
            ret['cutype'] = 'Individual'
          }
        }
        return ret;
      }, this);

    }, null, this);
  } else {
    fAttendees = goog.Promise.resolve([ e.row ]);
  }
  
  fAttendees.then(function(attendees) {
    return goog.array.filter(attendees, function(attendee, index, array) {
      for (var i = 0; i < this.getModel().attendees.length; i++) {
        // don't add members which are already present
        if (this.getModel().attendees[i]['mailto'] == attendee['mailto']) {
          return false;
        }
      }
      // don't add the organizer as attendee
      if (this.getModel().organizer && this.getModel().organizer.dir) {
        if (this.getModel().organizer.dir == attendee['dir']) {
          return false;
        }
      }
      return true;
    }, this);
  }, null, this).then(function(attendees) {
    return goog.array.map(attendees, function(attendee) {
      return {
        'cutype' : attendee['cutype'],
        'member' : attendee['member'],
        'role' : 'RequiredParticipant',
        'partStatus' : 'NeedsAction',
        'rsvp' : attendee['rsvp'],
        'delTo' : attendee['delTo'],
        'delFrom' : attendee['delFrom'],
        'sentBy' : attendee['sentBy'],
        'commonName' : attendee['commonName'],
        'lang' : attendee['lang'],
        'mailto' : attendee['mailto'],
        'dir' : attendee['dir'],
        'uri' : attendee['uri'],
        'internal' : attendee['internal']
      };
    });
  }, null, this).then(function(attendees) {
    goog.array.forEach(attendees, function(attendee) {
      // default participation => RequiredParticipant
      attendee['role'] = 'RequiredParticipant';
      this.addAttendee_(attendee);
      
      if(attendee['cutype'] == "Resource") {
        this.addResourceTemplateToDescription(attendee);
      }
      
      this.getModel().attendees.push(attendee);
    }, this);
    this.getModel().states.meeting = this.getModel().attendees.length !== 0;
    this.ac_.setAttendees(this.getModel().attendees);
    if (e.row['cutype'] == 'Group') {
      var uid = e.row['uri'];
      this.ac_.addGroupAttendee(uid, attendees);
    }
    this.onAttendeeChange_();
  }, null, this);
};

/** @override */
net.bluemind.calendar.vevent.ui.Form.prototype.handleRemoveAttendee_ = function(attendee, row) {
  this.resetError_('attendees');
  this.getDomHelper().removeNode(row);
  goog.array.removeIf(this.getModel().attendees, function(a) {
    return attendee['mailto'] == a['mailto'];
  });
  this.getModel().states.meeting = this.getModel().attendees.length !== 0;
  this.getChild('freebusy').removeAttendee({
    'commonName' : attendee['commonName'],
    'dir' : attendee['dir'],
    'mailto' : attendee['mailto']
  });
  this.ac_.setAttendees(this.getModel().attendees);
  this.ac_.sanitizeGroups(attendee);
  if(attendee['cutype'] == "Resource") {
    this.removeResourceTemplateFromDescription(attendee);
  }
  this.onAttendeeChange_();
};


/**
 * Add attendee to form
 * 
 * @param {Object} attendee Attendee to add
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.addAttendee_ = function(attendee) {
  var element = this.getElementByClass(goog.getCssName('bm-ui-form-attendee'));
  attendee = this.adaptor.attendeeToModelView(attendee);

  var row = soy.renderAsFragment(net.bluemind.calendar.vevent.templates.attendee, {
    attendee : attendee
  });

  this.getDomHelper().appendChild(element, row);
  var remove = this.getDomHelper().getElementByClass(goog.getCssName('bm-ui-form-remove'), row);
  var role = this.getDomHelper().getElementByClass(goog.getCssName('bm-ui-form-role'), row);

  if (this.getModel().states.master) {
    this.getHandler().listen(remove, goog.events.EventType.CLICK, function() {
      this.handleRemoveAttendee_(attendee, row);
    });
    this.getHandler().listen(role, goog.events.EventType.CLICK, function() {
      this.toggleRole_(attendee, row);
    });
    this.getChild('freebusy').addAttendees([ attendee ]);

  } else {
    goog.style.setElementShown(remove, false);
  }
};

/**
 * Request the computing of a resource template if any and add it to the event
 * description if not already done.
 */
net.bluemind.calendar.vevent.ui.Form.prototype.addResourceTemplateToDescription = function (attendee) {
  this.addOrRemoveResourceTemplateFromDescription(attendee, "add");
}

/**
 * Request the removing of a resource template from the event description if
 * present.
 */
net.bluemind.calendar.vevent.ui.Form.prototype.removeResourceTemplateFromDescription = function (attendee) {
  this.addOrRemoveResourceTemplateFromDescription(attendee, "remove");
}

/** Add or remove a resource template to/from the event's description. */
net.bluemind.calendar.vevent.ui.Form.prototype.addOrRemoveResourceTemplateFromDescription = function (attendee, mode) {
  var resourceUid = attendee.dir.substring(attendee.dir.lastIndexOf("/") + 1);
  var descriptionEditor = this.editor_;
  var eventDescription = descriptionEditor.getValue();
  var domainUid = this.ctx.user["domainUid"];
  if (mode == "add") {
    var organizerUid = this.ctx.user.uid;
    this.ctx.service('resources').addToEventDescription(domainUid, resourceUid, organizerUid, eventDescription)
      .then(function (result) {
        descriptionEditor.setValue(result);
      });
  } else if (mode == "remove") {
    this.ctx.service('resources').removeFromEventDescription(domainUid, resourceUid, eventDescription)
      .then(function (result) {
        descriptionEditor.setValue(result);
      });
  }
}

net.bluemind.calendar.vevent.ui.Form.prototype.onAttendeeChange_ = function() {
  this.setFormActions_();
}

net.bluemind.calendar.vevent.ui.Form.prototype.setFormActions_ = function() {
  var model = this.getModel();
  this.setFormButtons_(this.adaptor.isPublic(model.old, model) && model.states.master, model.states.draft);
}

net.bluemind.calendar.vevent.ui.Form.prototype.setFormButtons_ = function(isMeeting, isDraft) {
  if (isMeeting) {
    /** @meaning general.send */
    var MSG_SEND = goog.getMsg('Send');
    this.getChild('toolbar').getChild("send").setCaption(MSG_SEND);
    this.getChild('toolbar').getChild("save").setVisible(isDraft);
  } else {
    /** @meaning general.save */
    var MSG_SAVE = goog.getMsg('Save');
    this.getChild('toolbar').getChild("send").setCaption(MSG_SAVE);
    this.getChild('toolbar').getChild("save").setVisible(false);
  }
}

/**
 * toggle attendee role.
 * 
 * @param {bluemind.calendar.model.Attendee} attendee attendee.
 * @param {Element} row Attendee displayed row.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.toggleRole_ = function(attendee, row) {
  var role = this.getDomHelper().getElementByClass(goog.getCssName('bm-ui-form-role'), row);
  if (attendee['role'] == 'OptionalParticipant') {
    attendee['role'] = 'RequiredParticipant';
    goog.dom.classlist.remove(role, goog.getCssName('fa-plus-circle'));
    goog.dom.classlist.add(role, goog.getCssName('fa-exclamation-circle'));
  } else {
    attendee['role'] = 'OptionalParticipant';
    goog.dom.classlist.add(role, goog.getCssName('fa-plus-circle'));
    goog.dom.classlist.remove(role, goog.getCssName('fa-exclamation-circle'));
  }

  goog.array.map(this.getModel().attendees, function(attendeeModel) {
    if (attendee['dir'] == attendeeModel['dir']) {
      attendeeModel['role'] = attendee['role'];
    }
  }, this);
};

/**
 * show notice message
 * 
 * @param {text} msg message to display.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.notice_ = function(msg) {
  this.getChild('notifications').addNotice(msg);
};

/**
 * show warning message
 * 
 * @param {text} msg message to display.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.prototype.warn_ = function(msg) {
  this.getChild('notifications').addWarn(goog.dom.getElement('bm-ui-form-title'), msg);
};

/**
 */
net.bluemind.calendar.vevent.ui.Form.prototype.menuActions_ = function(e) {
  var action = e.target.getId();
  var model = this.getModel();
  switch(action) {
    case "duplicate": 
      model.states.main = true;
    case "duplicate-occurrence": 
      var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.DUPLICATE, model);
      this.dispatchEvent(e);
      break;
    case "history": 
      this.dispatchEvent('history');
      break;
  }


}

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */

net.bluemind.calendar.vevent.ui.Form.Notification = function(opt_domHelper) {
  goog.base(this, opt_domHelper);

  this.errors_ = new goog.structs.Map();
  this.warnings_ = new goog.structs.Map();
  this.notices_ = new goog.structs.Map();

};
goog.inherits(net.bluemind.calendar.vevent.ui.Form.Notification, goog.ui.Component);

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.popup_;

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.container_;

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.btn_;

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.notificationBandal_;

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.errors_;

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.warnings_;

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.notices_;

/** @override */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.btn_ = goog.dom.getElement('bm-ui-form-notification-popup-btn');
  this.container_ = goog.dom.getElement('bm-ui-form-notification-popup');
  this.popup_ = new goog.ui.Popup(this.container_);
  this.popup_.setHideOnEscape(true);
  this.popup_.setAutoHide(true);
  this.popup_
      .setPosition(new goog.positioning.AnchoredViewportPosition(this.btn_, goog.positioning.Corner.BOTTOM_LEFT));
  this.notificationBandal_ = goog.dom.getElement('bm-ui-form-notification');

  this.getHandler().listen(this.btn_, goog.events.EventType.CLICK, function(e) {
    this.show_();
  });
};

/**
 * add an error message
 * 
 * @param {string | Element | Array} fields Fields concerned by this error.
 * @param {string} msg message.
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.addError = function(fields, msg) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }
  var data = {
    css : goog.getCssName('error'),
    msg : msg
  };

  this.add_(fields, this.errors_, data);
};

/**
 * remove a error msg
 * 
 * @param {string | Element | Array} fields Fields concerned by this warning.
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.removeError = function(fields) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }

  goog.array.forEach(fields, function(f) {
    this.errors_.remove(f.id);
  }, this);

  this.showBandalError_();
};

/**
 * add a warning message
 * 
 * @param {string | Element | Array} fields Fields concerned by this warning.
 * @param {string} msg message.
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.addWarn = function(fields, msg) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }
  var data = {
    css : goog.getCssName('warn'),
    msg : msg
  };

  this.add_(fields, this.warnings_, data);
};

/**
 * remove a warning msg
 * 
 * @param {string | Element | Array} fields Fields concerned by this warning.
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.removeWarn = function(fields) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }
  goog.array.forEach(fields, function(f) {
    this.warnings_.remove(f.id);
  }, this);

  this.showBandalError_();
};

/**
 * add a notice message
 * 
 * @param {string | Element | Array} fields Fields concerned by this notice.
 * @param {string} msg message.
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.addNotice = function(fields, msg) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }
  var data = {
    css : goog.getCssName('notice'),
    msg : msg
  };

  this.add_(fields, this.notices_, data);
};

/**
 * add a message
 * 
 * @param {string | Element | Array} fields Fields concerned by this notice.
 * @param {Array} messages messages.
 * @param {string} data data.
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.add_ = function(fields, messages, data) {
  goog.array.forEach(fields, function(f) {
    messages.set(f.id, data);
  });
  this.showBandalError_();
};

/**
 * remove a notice msg
 * 
 * @param {string | Element | Array} fields Fields concerned by this notice.
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.removeNotice = function(fields) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }
  goog.array.forEach(fields, function(f) {
    this.notices_.remove(f.id);
  }, this);

  this.showBandalError_();
};

/**
 * show message banner
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.showBandalError_ = function() {
  goog.dom.classlist.set(this.notificationBandal_, goog.getCssName('form-notification'));
  if (this.errors_.getValues().length > 0) {
    var d = this.errors_.getValues()[0];
    this.notificationBandal_.innerHTML = d.msg;
    goog.dom.classlist.add(this.notificationBandal_, d.css);
    goog.style.setStyle(this.notificationBandal_, 'visibility', 'visible');
  } else if (this.warnings_.getValues().length > 0) {
    var d = this.warnings_.getValues()[0];
    this.notificationBandal_.innerHTML = d.msg;
    goog.dom.classlist.add(this.notificationBandal_, d.css);
    goog.style.setStyle(this.notificationBandal_, 'visibility', 'visible');
  } else if (this.notices_.getValues().length > 0) {
    var d = this.notices_.getValues()[0];
    this.notificationBandal_.innerHTML = d.msg;
    goog.dom.classlist.add(this.notificationBandal_, d.css);
    goog.style.setStyle(this.notificationBandal_, 'visibility', 'visible');
  } else {
    goog.style.setStyle(this.notificationBandal_, 'visibility', 'hidden');
  }
  var count = this.errors_.getCount() + this.warnings_.getCount() + this.notices_.getCount();
  if (count > 1) {
    /** @meaning calendar.form.error.showAll */
    var MSG_SHOW_ERROR = goog.getMsg('Show all errors ({$nb})', {
      nb : count
    });
    this.btn_.innerHTML = MSG_SHOW_ERROR;
    goog.style.setStyle(this.btn_, 'visibility', 'visible');
  } else {
    goog.style.setStyle(this.btn_, 'visibility', 'hidden');
  }
};

/**
 * show error popup
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Form.Notification.prototype.show_ = function() {
  this.container_.innerHTML = '';

  var formNotification = bluemind.calendar.template.formNotification;

  goog.array.forEach(this.errors_.getValues(), function(e) {
    var msg = soy.renderAsFragment(formNotification, e);
    goog.dom.appendChild(this.container_, msg);
  }, this);

  goog.array.forEach(this.warnings_.getValues(), function(e) {
    var msg = soy.renderAsFragment(formNotification, e);
    goog.dom.appendChild(this.container_, msg);
  }, this);

  goog.array.forEach(this.notices_.getValues(), function(e) {
    var msg = soy.renderAsFragment(formNotification, e);
    goog.dom.appendChild(this.container_, msg);
  }, this);

  this.popup_.setVisible(true);
};

