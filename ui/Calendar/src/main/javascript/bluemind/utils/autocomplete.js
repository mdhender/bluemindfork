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
 * @fileoverview AutoComplete.
 */

goog.provide('bluemind.calendar.utils.AutoComplete');

goog.require('bluemind.calendar.model.Calendar');
goog.require('bluemind.calendar.template.i18n');
goog.require('bluemind.calendar.utils.AutoCompleteRemote');
goog.require('goog.dom');
goog.require('goog.soy');


/**
 * AutoComplete
 * @param {text} url url.
 * @param {text} inputTxt input text field.
 * @param {goog.structs.Map} elements elements.
 * @constructor
 */
bluemind.calendar.utils.AutoComplete = function(url, inputTxt, elements) {
  var input = goog.dom.getElement(inputTxt);
  var li = new goog.ui.LabelInput();
  li.decorate(input);

  if (!bluemind.net.OnlineHandler.getInstance().isOnline()) {
    li.setEnabled(false);
    input.title = bluemind.calendar.template.i18n.onlineOnly();
  }
  goog.events.listen(bluemind.net.OnlineHandler.getInstance(), 'offline',
    function() {
    li.setEnabled(false);
    input.title = bluemind.calendar.template.i18n.onlineOnly();
  });
  goog.events.listen(bluemind.net.OnlineHandler.getInstance(), 'online',
    function() {
    li.setEnabled(true);
    input.title = '';
  });

  var ac = new bluemind.calendar.utils.AutoCompleteRemote(url, input);
  ac.setMethod('POST');
  ac.setContent('filter=read');
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
        if (!(c['calendars'].length == 1 &&
          c['calendars'][0]['type'] == 'contact')) {
            calendars.set(
              goog.string.trim(c['label']).toLowerCase() + '' + i, c);
        }
      }
      var keys = calendars.getKeys().sort();
      var length = keys.length;
      if (length > 12) length = 12;

      for (var i = 0; i < length; ++i) {
        var d = '';
        var c = calendars.get(keys[i]);
        d += '"' + goog.string.trim(c['label']) + '"';

        if (c['calendars'].length != 1) {
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
      var rendRows = [];
      rendRows.push({
        id: null,
        data: bluemind.calendar.template.i18n.
          emptyCalendarSearchResult({'search': this.token_})
      });
      this.renderer_.renderRows(rendRows, this.token_, this.target_);
    }
  };

  goog.events.listen(ac, goog.ui.ac.AutoComplete.EventType.UPDATE, function(e) {
    if (e.row) {
      li.setValue('');
      var calendars = e.row['calendars'];
      for (var i = 0; i < calendars.length; i++) {
        var calendar = calendars[i];
        if (!bluemind.manager.getCalendar(calendar['calendar'])) {
          var cal = new bluemind.calendar.model.Calendar(
            calendar['calendar'], calendar['displayName'], calendar['email'],
            calendar['ownerId'], calendar['picture'], 0, calendar['type'],
            calendar['workingDays'], calendar['dayStart'], calendar['dayEnd'],
            calendar['minDuration']);
          bluemind.manager.registerCalendar(cal);
        }
      }
      bluemind.view.refresh();
      bluemind.view.getView().display();
    }
  });

  goog.events.listen(input, goog.events.EventType.KEYDOWN, function(e) {
    if (e.keyCode != goog.events.KeyCodes.ENTER &&
        e.keyCode != goog.events.KeyCodes.UP &&
        e.keyCode != goog.events.KeyCodes.DOWN) {
      ac.dismiss()
    }
  });

};
