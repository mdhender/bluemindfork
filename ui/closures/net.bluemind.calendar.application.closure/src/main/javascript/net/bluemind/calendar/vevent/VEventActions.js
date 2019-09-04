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

goog.provide("net.bluemind.calendar.vevent.VEventActions");

goog.require("goog.Disposable");
goog.require("goog.Uri");
goog.require("goog.array");
goog.require("goog.date.Interval");
goog.require("goog.structs.Map");
goog.require("net.bluemind.calendar.day.ui.PrivateChangesDialog");
goog.require("net.bluemind.calendar.day.ui.RecurringDeleteDialog");
goog.require("net.bluemind.calendar.day.ui.RecurringFormDialog");
goog.require("net.bluemind.calendar.day.ui.RecurringUpdateDialog");
goog.require("net.bluemind.calendar.day.ui.SendNotificationDialog");
goog.require("net.bluemind.calendar.day.ui.SendNoteDialog");
goog.require('bluemind.storage.StorageHelper');
goog.require("net.bluemind.calendar.Messages");
goog.require("net.bluemind.mvp.UID");

/**
 * @constructor
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @param {net.bluemind.calendar.vevent.VEventAdaptor} adaptor
 * @param {function()=} opt_success On success callback
 * @param {function()=} opt_failure On failure callback
 * @extends {goog.Disposable}
 */
net.bluemind.calendar.vevent.VEventActions = function(ctx, adaptor, opt_success, opt_failure) {
  goog.base(this);
  this.ctx_ = ctx;
  this.adaptor_ = adaptor;
  this.popups_ = new goog.structs.Map();
  this.onSuccess_ = goog.isFunction(opt_success) ? opt_success : goog.nullFunction;
  this.onFailure_ = goog.isFunction(opt_failure) ? opt_failure : goog.nullFunction;
  this.logger = goog.log.getLogger('net.bluemind.calendar.vevent.VEventActions');
};
goog.inherits(net.bluemind.calendar.vevent.VEventActions, goog.Disposable);

/**
 * @type {net.bluemind.calendar.vevent.VEventAdaptor}
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.adaptor_;

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.ctx_;

/**
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.popups_;

/**
 * @type {Array}
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.calendars_;

/**
 * @type {function()}
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.onSuccess_;

/**
 * @type {function()}
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.onFailure_;

/**
 * @param {Array} calendars
 */
net.bluemind.calendar.vevent.VEventActions.prototype.setCalendars = function(calendars) {
  this.calendars_ = calendars;
};
/**
 * @param {Array} features
 */
net.bluemind.calendar.vevent.VEventActions.prototype.setFeatures = function(features) {
  var popup;
  this.popups_.forEach(function(popup) {
    popup.dispose();
  });
  this.popups_.clear();

  if (goog.array.contains(features, 'recurring')) {
    popup = new net.bluemind.calendar.day.ui.RecurringUpdateDialog();
    popup.setId('recurring-update-popup');
    this.popups_.set('recurring-update', popup);
    this.registerDisposable(popup);

    popup = new net.bluemind.calendar.day.ui.RecurringDeleteDialog();
    popup.setId('recurring-delete-popup');
    this.popups_.set('recurring-delete', popup);
    this.registerDisposable(popup);

    popup = new net.bluemind.calendar.day.ui.RecurringFormDialog();
    popup.setId('recurring-form-popup');
    this.popups_.set('recurring-form', popup);
    this.registerDisposable(popup);
  }
  if (goog.array.contains(features, 'private')) {
    popup = new net.bluemind.calendar.day.ui.PrivateChangesDialog();
    popup.setId('private-changes-popup');
    this.popups_.set('private', popup);
    this.registerDisposable(popup);
  }
  if (goog.array.contains(features, 'notification')) {
    popup = new net.bluemind.calendar.day.ui.SendNotificationDialog();
    popup.setId('send-notification-popup');
    this.popups_.set('notification', popup);
    this.registerDisposable(popup);
  }

  if (goog.array.contains(features, 'note')) {
    popup = new net.bluemind.calendar.day.ui.SendNoteDialog();
    popup.setId('send-note-popup');
    this.popups_.set('note', popup);
    this.registerDisposable(popup);
  }

};

/**
 * Inject popup inside the view Grouic.
 * 
 * @param {goog.ui.Component} view
 */
net.bluemind.calendar.vevent.VEventActions.prototype.injectPopups = function(view) {
  this.popups_.forEach(function(popup) {
    view.addChild(popup, true)
  });
};

/**
 * Save event actions
 * 
 * @param {net.bluemind.calendar.vevent.VEventEvent} e
 */

net.bluemind.calendar.vevent.VEventActions.prototype.participation = function(e) {
  var model = e.vevent;
  this.ctx_.service('calendar').getItem(model.calendar, model.uid).then(function(vseries) {

    if (model.states.main) {
      var main = this.adaptor_.toModelView(vseries, goog.array.find(this.calendars_, function(calendar) {
        return calendar.uid == model.calendar;
      })).main;
      main.participation = model.participation;
      main.addNote = model.addNote;
      main.sendNotification = model.sendNotification;
      main.recurringDone = model.recurringDone;
      main.attendee.responseComment = model.attendee.responseComment;

      e.vevent = main;
    }
    e.force = true;
    return this.update_(e, vseries);
  }, null, this);

};

net.bluemind.calendar.vevent.VEventActions.prototype.collectAttendees_ = function(attendees) {
  if (attendees == null || attendees.length == 0) {
    return;
  }

  var toCollect = goog.array.filter(attendees, function(a) {
    return a.uri == null;
  });

  if (toCollect.length == 0) {
    return;
  }

  goog.array
      .forEach(
          toCollect,
          function(c) {
            var q = '(_exists_:value.communications.emails.value OR value.kind:group) AND (value.identification.formatedName.value:'
                + c['mailto'] + ' OR value.communications.emails.value:' + c['mailto'] + ')';

            this.ctx_.service('addressbooks').search(c['mailto'], 0, 1, 'Pertinance', q).then(function(res) {
              if (res.count == 0) {
                var vcard = {
                  'container' : 'book:CollectedContacts_' + this.ctx_.user['uid'],
                  'uid' : net.bluemind.mvp.UID.generate(),
                  'value' : {
                    'identification' : {
                      'name' : {
                        'familyNames' : c['commonName']
                      }
                    },
                    'organizational' : {},
                    'related' : {},
                    'explanatory' : {},
                    'communications' : {
                      'emails' : [ {
                        'parameters' : [ {
                          'label' : 'TYPE',
                          'value' : 'work'
                        } ],
                        'value' : c['mailto']
                      } ]
                    }
                  }
                };

                this.ctx_.service('addressbook').create(vcard);
              }
            }, null, this);
          }, this);
};

/**
 * Save event actions
 * 
 * @param {net.bluemind.calendar.vevent.VEventEvent} e
 */

net.bluemind.calendar.vevent.VEventActions.prototype.save = function(e) {
  var model = e.vevent;
  return this.ctx_.service('calendar').getItem(model.initalContainer || model.calendar, model.uid).then(
      function(existing) {
        if (existing && model.initalContainer && model.calendar != model.initalContainer) {
          this.move_(e, existing);
        } else if (!existing) {
          this.create_(e);
        } else {
          this.update_(e, existing);
        }
      }, null, this);
};

/**
 * Remove event actions
 * 
 * @param {goog.events.Event} e
 */
net.bluemind.calendar.vevent.VEventActions.prototype.remove = function(e) {
  var model = e.vevent;
  this.ctx_.service('calendar').getItem(model.calendar, model.uid).then(function(vseries) {
    var isPublic = this.adaptor_.isPublicChanges(vseries, model);
    if (!this.checkSendNotification_(model, isPublic, true)) {
      model.states.updating = true;
      this.showSendNotification_(model, e.type);
    } else if (!this.checkRecurringState_(vseries, model)) {
      this.showReccurringDeleteDialog_(vseries, model);
    } else if (!model.states.main && vseries['value']['main']) {
      var recurrence = this.ctx_.helper('date').toBMDateTime(model.recurrenceId, model.timezones.recurrence);
      vseries['value']['main']['exdate'] = vseries['value']['main']['exdate'] || [];
      vseries['value']['main']['exdate'].push(recurrence);
      this.adaptor_.addExdate(recurrence, vseries);
      return this.doUpdate_(vseries, model.sendNotification).then(this.resolve_, this.reject_, this);
    } else {
      return this.doRemove_(model.calendar, model.uid, model.sendNotification).then(this.resolve_, this.reject_, this);
    }
  }, null, this);
};

/**
 * Show event details actions
 * 
 * @param {goog.events.Event} e
 */
net.bluemind.calendar.vevent.VEventActions.prototype.details = function(e) {
  var model = e.vevent;
  this.ctx_.service('calendar').getItem(model.calendar, model.uid).then(function(vseries) {
    if (vseries != null) {
      var isPublic = this.adaptor_.isPublicChanges(vseries, model);
      if (!this.checkRecurringState_(vseries, model)) {
        this.showReccurringFormDialog_(vseries, model);
      } else if (!this.checkPrivateState_(model, e.force, isPublic)) {
        this.showPrivateChangesDialog_(e);
      } else {
        this.goToForm_(model, vseries);
      }
    } else {
      this.goToForm_(model);
    }
  }, null, this);
};

/**
 * @param {net.bluemind.calendar.vevent.VEventEvent} e
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.move_ = function(e, existing) {
  var model = e.vevent;

  var isPublic = this.adaptor_.isPublicChanges(null, model);
  if (goog.isDefAndNotNull(model.addNote) && model.addNote) {
    return this.showSendNote_(model);
  } else if (!this.checkSendNotification_(model, isPublic)) {
    model.states.updating = true;
    return this.showSendNotification_(model, e.type);
  } else {
    var vseries = this.adaptor_.fromVEventModelView(model);
    vseries['value']['icsUid'] = existing['value']['icsUid'];
    this.doCreate_(vseries, model.sendNotification).then(function() {
      return this.doRemove_(model.initalContainer, vseries.uid, false);
    }, null, this).then(function() {
      return net.bluemind.calendar.Messages.successMove();
    }, null, this).then(this.resolve_, this.reject_, this);
  }
};

/**
 * @param {net.bluemind.calendar.vevent.VEventEvent} e
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.create_ = function(e) {
  var model = e.vevent;
  var isPublic = this.adaptor_.isPublicChanges(null, model);
  if (goog.isDefAndNotNull(model.addNote) && model.addNote) {
    return this.showSendNote_(model);
  } else if (!this.checkSendNotification_(model, isPublic)) {
    model.states.updating = false;
    return this.showSendNotification_(model, e.type);
  } else {
    var vseries = this.adaptor_.fromVEventModelView(model);
    this.collectAttendees_(model.attendees);
    return this.doCreate_(vseries, model.sendNotification).then(this.resolve_, this.reject_, this);
  }
};

/**
 * @param {net.bluemind.calendar.vevent.VEventEvent} e
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.update_ = function(e, vseries) {
  var model = e.vevent;
  var isPublic = this.adaptor_.isPublicChanges(vseries, model);
  if (goog.isDefAndNotNull(model.addNote) && model.addNote) {
    return this.showSendNote_(model);
  } else if (!this.checkSendNotification_(model, isPublic)) {
    model.states.updating = true;
    this.showSendNotification_(model, e.type);
  } else if (!this.checkRecurringState_(vseries, model)) {
    this.showReccurringUpdateDialog_(vseries, model);
  } else if (!this.checkPrivateState_(model, e.force, isPublic)) {
    this.showPrivateChangesDialog_(e);
  } else if (model.updateFollowing) {
    return this.doUpdateFollowing_(model, vseries).then(this.resolve_, this.reject_, this);
  } else {
    var old = this.adaptor_.getRawOccurrence(model.recurrenceId, vseries);
    vseries = this.adaptor_.fromVEventModelView(model, vseries);
    var updated = this.adaptor_.getRawOccurrence(model.recurrenceId, vseries);
    if (goog.isDefAndNotNull(model.sendNotification) && model.sendNotification) {
      if (model.states.master && this.requiresAttendeesReset_(old, updated)) {
        goog.array.forEach(updated['attendees'], function(a) {
          a['partStatus'] = 'NeedsAction';
          // BM-12048 rsvp on attendees reset
          a['rsvp'] = true;
        });
      }
      if (model.attendee) {
        goog.array.forEach(updated['attendees'], function(a) {
          if (model.attendee.rsvp && a['dir'] == model.attendee.id) {
            a['rsvp'] = model.partStatus == 'NeedsAction';
          }
        });
      }

    }
    this.collectAttendees_(model.attendees);
    return this.doUpdate_(vseries, model.sendNotification).then(this.resolve_, this.reject_, this);
  }

};

/**
 * @param {net.bluemind.calendar.vevent.VEventEvent} oldVersion
 * @param {net.bluemind.calendar.vevent.VEventEvent} newVersion
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.requiresAttendeesReset_ = function(oldVersion, newVersion) {
  if (!newVersion['attendees']) return false;
  var reset = (oldVersion == null);

  reset = reset || (oldVersion['location'] != newVersion['location']);
  reset = reset || this.adaptor_.recurrenceHasChanged(oldVersion, newVersion);

  return reset;
}

/**
 * Update following
 * 
 * @param {Object} model
 * @param {Object} vevent
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.doUpdateFollowing_ = function(model, vseries) {
  var helper = this.ctx_.helper('date');
  var dtstart = helper.create(vseries['value']['main']['dtstart']);
  var until = model.dtstart.clone();
  until.add(new goog.date.Interval(0, 0, -1));
  vseries['value']['main']['rrule']['until'] = this.adaptor_.adaptUntil(dtstart, until);
  model.uid = net.bluemind.mvp.UID.generate();
  var neo = this.adaptor_.fromVEventModelView(model);
  return this.doUpdate_(vseries, model.sendNotification).then(function() {
    return this.doCreate_(neo, model.sendNotification);
  }, null, this);
};

/**
 * Perform update actions.
 * 
 * @private
 * @param {Object} vevent VEvent Core object
 * @param {Object} model VEvent View model
 * @return {boolean}
 */
net.bluemind.calendar.vevent.VEventActions.prototype.doUpdate_ = function(vseries, sendNotification) {
  var tags = goog.isDefAndNotNull(vseries['value']['main']) ? vseries['value']['main']['categories']
      : vseries['value']['occurrences'][0]['categories'];
  tags = goog.array.filter(tags, function(tag) {
    return tag['itemUid'] == null;
  });
  return this.ctx_.service('tags').createTags(tags).then(function() {
    return this.ctx_.service('calendar').update(vseries, sendNotification)
  }, null, this).then(function() {
    return net.bluemind.calendar.Messages.successUpdate();
  }, function(error) {
    goog.log.error(this.logger, 'error during event update ' + vseries['uid'], error);
    throw net.bluemind.calendar.Messages.errorUpdate(error);
  }, this);
};

/**
 * Perform create actions.
 * 
 * @private
 * @param {Object} vevent VEvent Core object
 * @param {bollean} sendNotification Must send notification
 * @return {boolean}
 */
net.bluemind.calendar.vevent.VEventActions.prototype.doCreate_ = function(vseries, sendNotification) {
  var tags = goog.isDefAndNotNull(vseries['value']['main']) ? vseries['value']['main']['categories']
      : vseries['value']['occurrences'][0]['categories'];
  tags = goog.array.filter(tags, function(tag) {
    return tag['itemUid'] == null;
  });
  return this.ctx_.service('tags').createTags(tags).then(function() {
    return this.ctx_.service('calendar').create(vseries, sendNotification)
  }, null, this).then(function() {
    return net.bluemind.calendar.Messages.successCreate();
  }, function(error) {
    goog.log.error(this.logger, 'error during event create ' + vseries['uid'], error);
    throw net.bluemind.calendar.Messages.errorCreate(error);
  }, this);
};

/**
 * Perform removal actions.
 * 
 * @private
 * @param {string} calendar Calendar uid
 * @param {calendar} event Event uid
 * @param {bollean=} opt_sendNotification Must send notification
 * @return {boolean}
 */
net.bluemind.calendar.vevent.VEventActions.prototype.doRemove_ = function(calendar, event, opt_sendNotification) {
  return this.ctx_.service('calendar').deleteItem(calendar, event, !!opt_sendNotification).then(function(item) {
    return net.bluemind.calendar.Messages.successDelete();
  }, function(error) {
    goog.log.error(this.logger, 'error during delete of event ' + event, error);
    throw net.bluemind.calendar.Messages.errorDelete(error);
  }, this);
};

/**
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.resolve_ = function(message) {
  this.ctx_.notifyInfo(message);
  this.onSuccess_();
};

/**
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.reject_ = function(message) {
  this.ctx_.notifyError(message);
  this.onFailure_();
};

/**
 * @param {*} model
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.goToForm_ = function(model, opt_vseries) {

  var uri = new goog.Uri('/vevent/');
  uri.getQueryData().set('uid', model.uid);
  if (model.recurrenceId) {
    uri.getQueryData().set('recurrence-id', model.recurrenceId.toIsoString(true, true))
  }
  uri.getQueryData().set('container', model.calendar);
  // FIXME
  if (model.states.updatable) {
    var storage = bluemind.storage.StorageHelper.getExpiringStorage();
    var vseries = this.adaptor_.fromVEventModelView(model, opt_vseries);
    storage.set(model.uid, vseries, goog.now() + 600);
    uri.getQueryData().set('draft', true);
  }
  this.ctx_.helper('url').goTo(uri);
};

/**
 * Show dialog
 * 
 * @param {Object} model
 * @param {Object} event
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.showPrivateChangesDialog_ = function(e) {
  this.popups_.get('private').setModel(e);
  this.popups_.get('private').setVisible(true);
};

/**
 * Check if the occurrence to update is known or if the recurring popup must be
 * shown.
 * 
 * @private
 * @param {Object} model Vevent view model
 * @param {boolean} isPublic Event changes can be notified to other attendees or
 * organizer
 * @param {boolean} isDeleteAction deleted action?
 * @return {boolean}
 */
net.bluemind.calendar.vevent.VEventActions.prototype.checkSendNotification_ = function(model, isPublic, isDeleteAction) {
  if (this.popups_.containsKey('notification') && !goog.isDefAndNotNull(model.sendNotification) && isPublic) {
    var needNotif = isDeleteAction
        || this.needNotification(model.oldValue, this.adaptor_.veventAdaptor_.fromModelView(model));
    return !needNotif;
  }
  return true;
};

/**
 * Check if send notification state is ok or if the send notification popup must
 * be shown.
 * 
 * @private
 * @param {Object} vseries VEvent Core object
 * @param {Object} model VEvent View model
 * @return {boolean}
 */
net.bluemind.calendar.vevent.VEventActions.prototype.checkRecurringState_ = function(vseries, model) {
  if (this.popups_.containsKey('recurring-update') && (model.states.repeat) && !model.recurringDone) {
    return false;
  }
  return true;
};

/**
 * Check if the user must be advertised about private changes. be shown.
 * 
 * @private
 * @param {Object} model
 * @param {boolean} force Force insert
 * @param {boolean} isPublic Event changes can be notified to other attendees or
 * organizer
 * @return {boolean}
 */
net.bluemind.calendar.vevent.VEventActions.prototype.checkPrivateState_ = function(model, force, isPublic) {
  if (this.popups_.containsKey('private') && model.states.meeting && !force && !isPublic) {
    return false;
  }
  return true;
};

/**
 * Show dialog
 * 
 * @param {Object} model
 * @param {Object} event
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.showReccurringUpdateDialog_ = function(vseries, model) {
  var calendar = goog.array.find(this.calendars_, function(calendar) {
    return calendar.uid == model.calendar;
  });
  var vseries = this.adaptor_.toModelView(vseries, calendar);
  this.popups_.get('recurring-update').setModel(model);
  this.popups_.get('recurring-update').setVSeries(vseries);
  this.popups_.get('recurring-update').setVisible(true);
};

/**
 * Show dialog
 * 
 * @param {Object} model
 * @param {string|!goog.events.EventId} action Source action type
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.showSendNotification_ = function(model, action) {
  this.popups_.get('notification').setModel(model);
  this.popups_.get('notification').setVisible(true);
  this.popups_.get('notification').setOrigin(action);
};

/**
 * Show dialog
 * 
 * @param {Object} model
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.showSendNote_ = function(model) {
  this.popups_.get('note').setModel(model);
  this.popups_.get('note').setVisible(true);
};

/**
 * Show dialog
 * 
 * @param {Object} model
 * @param {Object} event
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.showReccurringDeleteDialog_ = function(vseries, model) {
  var calendar = goog.array.find(this.calendars_, function(calendar) {
    return calendar.uid == model.calendar;
  });
  var vseries = this.adaptor_.toModelView(vseries, calendar);
  this.popups_.get('recurring-delete').setModel(model);
  this.popups_.get('recurring-delete').setVSeries(vseries);
  this.popups_.get('recurring-delete').setVisible(true);
};

/**
 * Show dialog
 * 
 * @param {Object} model
 * @param {Object} event
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.vevent.VEventActions.prototype.showReccurringFormDialog_ = function(vseries, model) {
  var calendar = goog.array.find(this.calendars_, function(calendar) {
    return calendar.uid == model.calendar;
  });
  var vseries = this.adaptor_.toModelView(vseries, calendar);
  this.popups_.get('recurring-form').setModel(model);
  this.popups_.get('recurring-form').setVSeries(vseries);
  this.popups_.get('recurring-form').setVisible(true);
};

net.bluemind.calendar.vevent.VEventActions.prototype.needNotification = function(oldValue, actual) {
  var compareOrganizer = function(old, actual) {
    if (!old && actual || !actual && old) {
      return false;
    }
    return old == actual || old['dir'] == actual['dir'];
  };

  var compareAttendees = function(old, actual) {
    if (old == null && actual == null) {
      return true;
    } else if ((old == null && actual != null) || (old != null && actual == null)) {
      return false;
    } else if (old.length != actual.length) {
      return false;
    } else {
      return goog.array.equals(old, actual, function(oldAttendee, actualAttendee) {
        return oldAttendee['mailto'] == actualAttendee['mailto']
            && oldAttendee['partStatus'] == actualAttendee['partStatus'];
      });
    }

  };

  var compareAttachments = function(old, actual) {
    if (old == null && actual == null) {
      return true;
    } else if ((old == null && actual != null) || (old != null && actual == null)) {
      return false;
    } else if (old.length != actual.length) {
      return false;
    } else {
      return goog.array.equals(old, actual, function(oldAttachment, actualAttachment) {
        return oldAttachment['name'] == actualAttachment['name']
            && oldAttachment['publicUrl'] == actualAttachment['publicUrl'];
      });
    }

  };

  var compareExdate = function(old, actual) {
    var oldLength = goog.isDefAndNotNull(old) && goog.isArray(old) ? old.length : 0;
    var actualLength = goog.isDefAndNotNull(actual) && goog.isArray(actual) ? actual.length : 0;
    return actualLength == oldLength;
  }
  var ret = false;
  ret |= oldValue['summary'] != actual['summary'];
  ret |= oldValue['description'] != actual['description'];
  ret |= oldValue['classification'] != actual['classification'];
  ret |= oldValue['location'] != actual['location'];
  ret |= oldValue['priority'] != actual['priority'];
  ret |= !compareExdate(oldValue['exdate'], actual['exdate']);
  ret |= !compareAttendees(oldValue['attendees'], actual['attendees']);
  ret |= !compareAttachments(oldValue['attachments'], actual['attachments']);
  ret |= !compareOrganizer(oldValue['organizer'], actual['organizer']);

  ret |= this.adaptor_.recurrenceHasChanged(oldValue, actual);
  return ret;
}
