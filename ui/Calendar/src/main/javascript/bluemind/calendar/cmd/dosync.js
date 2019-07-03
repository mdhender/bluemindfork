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
 * @fileoverview
 *
 * Synchronize event data.
 */

goog.provide('bluemind.calendar.cmd.event.DoSync');

goog.require('bluemind.calendar.model.Event');
goog.require('bluemind.calendar.model.AttendeeParticipation');
goog.require('bluemind.calendar.model.AttendeeParticipationRole');
goog.require('bluemind.cmd.DeferredCommand');



/**
 * A command object for synchronizing event data with bm-core.
 * @param {Array.<bluemind.calendar.model.Event>} updated Locally updated
 *   event list.
 * @param {Array.<string>} removed Locally removed event id list.
 * @param {goog.structs.Map} notifications attendee's notification.
 * @param {goog.structs.Map} alerts attendee's alert.
 * @param {Array.<bluemind.calendar.model.SyncCalendar>} cals Calendars to sync.
 * @param {goog.async.Deferred} deferred Deferred object that will propagate
 *   the success or failure.
 *
 * @constructor
 * @extends {bluemind.cmd.DeferredCommand}
 */
bluemind.calendar.cmd.event.DoSync =
  function(updated, removed, notifications, alerts, cals, deferred) {
  var uid = 'bluemind.calendar.cmd.event.DoSync:' + goog.now();
  goog.base(this, deferred, uid, 'calendar/bmc', 'calendar', 'doSync');
  this.data.add('updated', this.serializer.serialize(updated));
  this.data.add('removed', this.serializer.serialize(removed));
  this.data.add('calendars', this.serializer.serialize(cals));
  this.data.add('notification', this.serializer.serialize(notifications));
  this.data.add('alerts', this.serializer.serialize(alerts));
};
goog.inherits(bluemind.calendar.cmd.event.DoSync,
  bluemind.cmd.DeferredCommand);

/** @override */
bluemind.calendar.cmd.event.DoSync.prototype.onSuccess = function(evt) {
  try {
    var xhr = evt.target;
    var sync = xhr.getResponseJson();
    var result = {};
  
    var updated = sync['updated'] || [];
    result.updated = [];
    for (var i = 0; i < updated.length; i++) {
      var evt = updated[i];
      var attendees = evt['attendees'];
      var owner = sync['owners'][evt['ownerId']];
      var atts = new Array()
      for(var j = 0; j < attendees.length; j++) {
        var a = attendees[j];
        var att = sync['attendees'][a.id];
        a['id'] = a['id'];
        a['role'] =
          bluemind.calendar.model.AttendeeParticipationRole.getFromInt(a['role']);
        a['participation'] =
          bluemind.calendar.model.AttendeeParticipation.getFromInt(a['part']);
        a['calendar'] = att['cal'];
        a['dayEnd'] = att['de'];
        a['dayStart'] = att['ds'];
        a['displayName'] = att['dn'];
        a['email'] = att['email'];
        a['id'] = att['id'];
        a['picture'] = att['pic'];
        a['type'] = att['type'];
        a['workingDays'] = att['wd'];
        atts.push(a);
      } 
      evt['owner'] = owner['owner'];
      evt['ownerCalId'] = owner['cal'];
      evt['ownerDN'] = owner['dn'] || '';
      evt['attendees'] = atts;
      evt['opacity'] = (evt['busy'] == '0' ? 'TRANSPARENT': 'OPAQUE');
      result.updated.push(bluemind.calendar.model.Event.parse(evt));
    }
  
    result.removed = sync['removed'] || [];
    result.lastSync = sync['lastSync'];
    this.callersOnSuccess(result);
  } catch(err){
    this.callersOnFailure(evt);
  }
};

/** @override */
bluemind.calendar.cmd.event.DoSync.prototype.onFailure = function(evt) {
  this.callersOnFailure(evt);
};
