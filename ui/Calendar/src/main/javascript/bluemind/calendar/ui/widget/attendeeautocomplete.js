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
 * @fileoverview AutoComplete for event attendees.
 */

goog.provide('bluemind.calendar.ui.widget.AttendeeAutoComplete');

goog.require('bluemind.calendar.template.i18n');
goog.require('bluemind.calendar.ui.autocomplete.Attendee');
goog.require('bluemind.calendar.utils.AutoCompleteRemote');
goog.require('bluemind.net.OnlineHandler');
goog.require('goog.format.EmailAddress');

/**
 * AutoComplete
 * @param {text} url url.
 * @param {text} inputTxt input text field.
 * @param {goog.structs.Map} elements Initial elements into the list.
 * @constructor
 */
bluemind.calendar.ui.widget.AttendeeAutoComplete =
  function(url, inputTxt, elements) {
  var input = goog.dom.getElement(inputTxt);
  var li = new goog.ui.LabelInput();
  li.decorate(input);

  var users = function(item) {
    item.render = function(node, token) {
      var dom_ = goog.dom.getDomHelper(node);
      var nameNode = dom_.createDom('span');
      dom_.appendChild(nameNode, dom_.createTextNode(item.label));
      dom_.appendChild(node, nameNode);
    };

    item.select = function(target) {
      target.value = item.label;
    };
  };

  var ac;
  if (bluemind.net.OnlineHandler.getInstance().isOnline()) {
    ac = new bluemind.calendar.utils.AutoCompleteRemote(url, input);
    ac.setMethod('POST');
    ac.setContent('filter=access');
  } else {
    bluemind.calendar.model.AttendeeHome.getInstance()
      .list().addCallback(function(attendees) {
      ac = new bluemind.calendar.ui.autocomplete.Attendee(attendees, input);
      input.title = bluemind.calendar.template.i18n.restrictAttendee();
    });
  }

  ac.renderRows = function(rows, opt_preserveHilited) {
    this.rows_ = new Array();
    if (rows.length > 0) {
      var indexToHilite = opt_preserveHilited ?
          this.getIndexOfId(this.hiliteId_) : null;

      // Current token matches the matcher's response token.
      this.firstRowId_ += this.rows_.length;
      var rendRows = [];

      var calendars = new goog.structs.Map();

      for (var i = 0; i < rows.length; ++i) {
        var c = rows[i];
        calendars.set(goog.string.trim(c['label']).toLowerCase() +
        ' ' + c['email'] + i, c);
      }

      var keys = calendars.getKeys().sort();
      var length = keys.length;
      if (length > 12) length = 12;

      for (var i = 0; i < length; ++i) {
        var c = calendars.get(keys[i]);

        var d = '"' + goog.string.trim(c['label']) + '"';

        if (c['calendars'] && c['calendars'].length != 1) {
          d += ' (' + c['calendars'].length + ')';
        }

        if (c['email'] != '') {
          d += ' <' + c['email'] + '>';
        }

        rendRows.push({
          id: this.getIdOfIndex_(i),
          data: d
        });

        this.rows_.push(c);

      }
      this.renderer_.renderRows(rendRows, this.token_, this.target_);

      if (this.autoHilite_ && rendRows.length != 0 && this.token_) {
        var idToHilite = indexToHilite != null ?
            this.getIdOfIndex_(indexToHilite) : this.firstRowId_;
        this.hiliteId(idToHilite);
      } else {
        this.hiliteId_ = -1;
      }
      this.dispatchEvent(goog.ui.ac.AutoComplete.EventType.SUGGESTIONS_UPDATE);
    } else {
      if (this.token_ != '') {
        this.hiliteId_ = -1;
        var rendRows = [];
        rendRows.push({
          id: null,
          data: this.token_
        });
        this.renderer_.renderRows(rendRows, this.token_, this.target_);
        this.dispatchEvent(goog.ui.ac.AutoComplete.EventType.SUGGESTIONS_UPDATE);
      }
    }
  };

  goog.events.listen(ac,
    goog.ui.ac.AutoComplete.EventType.UPDATE, function(e) {
    if (e.row) {
      var calendars;
      if (e.row['calendars']) {
        calendars = e.row['calendars'];
      } else {
        calendars = [e.row];
      }
      var view = bluemind.view.getView();
      var toAdd = new Array();
      for (var i = 0; i < calendars.length; i++) {
        var calendar = calendars[i];
        if (!view.hasAttendee(calendar['calendar']) &&
          !(calendar['email'] != '' && view.hasContactAttendee(calendar['email']))) {
          if (calendar['type'] == 'contact') {
            calendar['id'] = 'contact_' + goog.getUid(calendar);
            calendar['calendar'] = calendar['id'];
          }
          goog.array.insert(toAdd, calendar);
        }
      }
      view.addAttendees(toAdd);
    } else {
      var emailAddress = goog.format.EmailAddress.parse(li.getValue());
      if (emailAddress.isValid()) {
        var email = emailAddress.getAddress();
        var name = emailAddress.getName() || email;
        var a = new Array();
        a['id'] = 'contact_' + goog.getUid(a);
        a['calendar'] = a['id'];
        a['displayName'] = name;
        a['email'] = email;
        a['type'] = 'contact';
        bluemind.view.getView().addContact(a);
      } else {
        bluemind.notification.show(
          bluemind.calendar.template.i18n.invalidEmail());
      }
    }
    li.clear();
  });

};
