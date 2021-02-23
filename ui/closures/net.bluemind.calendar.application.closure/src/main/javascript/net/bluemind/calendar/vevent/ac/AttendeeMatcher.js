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

goog.provide("net.bluemind.calendar.vevent.ac.AttendeeMatcher");

goog.require("goog.Disposable");
goog.require("goog.array");
goog.require("net.bluemind.calendar.vevent.VEventAdaptor");

/**
 * @constructor
 * @param {net.bluemind.calendar.api.CalendarAutocompleteClient} client Auto
 *          complete client.
 * @param {Array.<Object>=} opt_attendees Attendees already added
 * @extends {goog.Disposable}
 */
net.bluemind.calendar.vevent.ac.AttendeeMatcher = function(ctx, opt_attendees) {
  this.ctx_ = ctx;
  this.attendees = opt_attendees || [];
  this.groupAttendees = [];
  this.adaptor = new net.bluemind.calendar.vevent.VEventAdaptor(ctx);
  goog.base(this);
};
goog.inherits(net.bluemind.calendar.vevent.ac.AttendeeMatcher, goog.Disposable);

/**
 * @type {net.bluemind.calendar.api.CalendarAutocompleteClient}
 * @private
 */
net.bluemind.calendar.vevent.ac.AttendeeMatcher.prototype.client_;

/**
 * @type {Array}
 */
net.bluemind.calendar.vevent.ac.AttendeeMatcher.prototype.attendees;

/**
 * @type {Array}
 */
net.bluemind.calendar.vevent.ac.AttendeeMatcher.prototype.groupAttendees;

/**
 * Returns whether the suggestions should be updated? <b>Override this to
 * prevent updates eg - when token is empty.</b>
 * 
 * @param {string} token Current token in autocomplete.
 * @param {number} maxMatches Maximum number of matches required.
 * @param {string=} opt_fullString Complete text in the input element.
 * @return {boolean} Whether new matches be requested.
 * @protected
 */
net.bluemind.calendar.vevent.ac.AttendeeMatcher.prototype.shouldRequestMatches = function(token, maxMatches,
    opt_fullString) {
  return true;
};

/**
 * Sanitizes the group attendees after an attendee has been removed
 * 
 * @param {Object} attendee;
 */
net.bluemind.calendar.vevent.ac.AttendeeMatcher.prototype.attendeeRemoved = function(attendee) {
  for (var i = 0; i < this.groupAttendees.length; i++) {
    for (var j = 0; j < this.groupAttendees[i]['attendees'].length; j++) {
      if (this.groupAttendees[i]['attendees'][j]['uri'] == attendee['uri']) {
        goog.array.removeAt(this.groupAttendees, i);
        break;
      }
    }
  }
};

/**
 * Handles the XHR response.
 * 
 * @param {string} token The XHR autocomplete token.
 * @param {Function} matchHandler The AutoComplete match handler.
 * @param {Array.<Object>} result Search result.
 */
net.bluemind.calendar.vevent.ac.AttendeeMatcher.prototype.onMatch = function(token, matchHandler, result) {
  var attendees = goog.array.filter(result, function(row) {
    if (!row['uri']) {
      return true;
    }
    var valid = true;
    goog.array.forEach(this.attendees, function(att) {
      if (att['uri'] == row['uri']) {
        valid = false;
      }
    });
    if (valid) {
      goog.array.forEach(this.groupAttendees, function(att) {
        if (att['uri'] == row['uri']) {
          valid = false;
        }
      });
    }
    return valid;
  }, this)
  matchHandler(token, attendees);
};

/**
 * Retrieve a set of matching rows from the server via ajax.
 * 
 * @param {string} token The text that should be matched; passed to the server
 *          as the 'token' query param.
 * @param {number} maxMatches The maximum number of matches requested from the
 *          server; passed as the 'max_matches' query param. The server is
 *          responsible for limiting the number of matches that are returned.
 * @param {Function} matchHandler Callback to execute on the result after
 *          matching.
 * @param {string=} opt_fullString The full string from the input box.
 */
net.bluemind.calendar.vevent.ac.AttendeeMatcher.prototype.requestMatchingRows = function(raw, maxMatches,
    matchHandler, opt_fullString) {

  if (!this.shouldRequestMatches(raw, maxMatches, opt_fullString)) {
    return;
  }

  var token = this.ctx_.helper('elasticsearch').escape(raw);
  var q = '(_exists_:value.communications.emails.value OR value.kind:group) AND (value.identification.formatedName.value:'
      + token + ' OR value.communications.emails.value:' + token + ')';

  // exclude videoconferencing resources
  var videoConferencingResources = this.ctx_.service('videoConferencing').getVideoConferencingResources();
  if (videoConferencingResources != null && videoConferencingResources.length > 0) {
    var exclude = ' AND !(';
    var or = '';
    goog.array.forEach(videoConferencingResources, function(res) {
      exclude += or + " uid:" + res.uid;
      or = ' OR ';
    });
    exclude +=")";

    q += exclude;
  }

  var callback = goog.bind(this.onMatch, this, raw, matchHandler);
  this.ctx_
      .service('addressbooks')
      .search(token, 0, 10, 'Pertinance', q)
      .then(function(res) {
        var attendees = [];
        goog.array.forEach(res, function(vcard) {
        	
          var ret = {
            'cutype' : null,
            'commonName' : vcard['value']['identification']['formatedName']['value'],
            'dir' : vcard['value']['source'],
            'uri' : vcard['container'] + '/' + vcard['uid'],
            'mailto' : null,
            'rsvp' : true,
            'memberCount' : 0
          };

          if (vcard['value']['communications']['emails'] && vcard['value']['communications']['emails'].length > 0) {
            ret['mailto'] = goog.array.reduce(vcard['value']['communications']['emails'], function(p, email) {
              if (p == null) {
                return email['value'];
              } else if (goog.array.find(email['parameters'], function(v) {
                return v['label'] == 'DEFAULT' && v['value'] == 'true';
              })) {
                return email['value'];
              } else {
                return p;
              }
            }, null);
          }
          if (vcard['value']['identification']['photo'] && ret['dir'] && goog.string.startsWith(ret['dir'], 'bm://')) {
            ret['icon'] = '/api/directory/' + this.ctx_.user['domainUid'] + '/_icon/'
                + encodeURIComponent(goog.string.removeAt(ret['dir'], 0, 5));
          }
          if (vcard['value']['kind'] == 'individual' && ret['dir'] && ret['dir'].indexOf('/resources/') >= 0) {
            ret['cutype'] = 'Resource';
          } else if (vcard['value']['kind'] == 'individual') {
            ret['cutype'] = 'Individual'
          } else if (vcard['value']['kind'] == 'group') {
            ret['cutype'] = 'Group';
            ret['memberCount'] = vcard['value']['organizational']['member'].length;
          } else {
            ret['cutype'] = 'Individual'
          }
          if (ret['cutype'] != 'Group' || ret['memberCount'] != 0) {
            attendees.push(ret);
          }
        }, this);
        return attendees;
      }, null, this)
      .then(
          function(r) {
            if (raw
                && raw
                    .match("^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$")) {
              var ret = {
                'cutype' : 'Individual',
                'commonName' : raw,
                'dir' : null,
                'uri' : null,
                'mailto' : raw,
                'rsvp' : true,
                'memberCount' : 0
              };
              r.push(ret);
            }
            return r;
          }).then(function(r) {
        // FIXME remove duplicates here ?
        return r;
      }, null, this).then(callback);

};
