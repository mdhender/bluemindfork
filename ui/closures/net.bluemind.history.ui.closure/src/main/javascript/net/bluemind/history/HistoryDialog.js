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
goog.provide("net.bluemind.history.HistoryDialog");

goog.require("net.bluemind.history.templates");
goog.require("goog.i18n.DateTimeFormat");
goog.require("goog.i18n.DateTimeFormat.Format");
goog.require("goog.i18n.DateTimeSymbols");// FIXME - unresolved required symbol
goog.require("goog.i18n.DateTimeSymbols_en");

/**
 * @constructor
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {goog.ui.Dialog}
 */
net.bluemind.history.HistoryDialog = function(ctx) {
  goog.base(this);
  this.ctx_ = ctx;
  /** @meaning general.history */
  var MSG_HISTORY = goog.getMsg('History');
  this.setTitle(MSG_HISTORY);
  this.setVisible(false);
  this.setButtonSet(goog.ui.Dialog.ButtonSet.OK);
}
goog.inherits(net.bluemind.history.HistoryDialog, goog.ui.Dialog);

net.bluemind.history.HistoryDialog.prototype.show = function(entries) {
  var elem = this.getDialogElement();
  elem.setAttribute("style", "width:700px");
  entries = entries.reverse();
  var formatter = this.ctx_.helper('dateformat').formatter.datetime;
  entries = entries.map(function(entry) {
    entry['date'] = formatter.format(new Date(entry['date']));
    
    if (entry['author'] != null) {
      var lowerAuthor = entry['author'].toLowerCase();
      if (lowerAuthor.indexOf("system") != -1) {
        entry['author'] = "System";
      }
    } else {
      entry['author'] = "";
    }
    
    if (entry['authorDisplayName'] == null) {
      entry['authorDisplayName'] = entry['author'];
    }
    
    /*
      bm-dav
      bm-eas-partnership-provider
      bm-eas-SEC1E207FB8D21E0
      bm-eas-sync-storage
      bm-eas-SFCD8275S12N55GTMEQE5DA2K8
      bm-hps
      bm-eas-2P39U5C9I171RD6PK0I3K1ANB8
      bm-eas-android1415378088711
      bm-connector-outlook-3.1.5718 otlk:14.0.0.7166
      bm-connector-thunderbird-3.1.5718 tbird:14.0.0.7166
    */

    if (entry['origin'] != null) {
      var lowerOrigin = entry['origin'].toLowerCase();
      if (lowerOrigin.indexOf("hps") != -1) {
        entry['origin'] = 'BlueMind Web';
      } else if ((lowerOrigin == 'bm-eas') || (lowerOrigin.indexOf("bm-eas-router") != -1)
          || (lowerOrigin.indexOf("bm-eas-sync") != -1) || (lowerOrigin.indexOf("bm-eas-partnership") != -1)) {
        entry['origin'] = 'ActiveSync';
      } else if (lowerOrigin.indexOf("migration") != -1) {
        entry['origin'] = 'BlueMind 3.0';
      } else if (lowerOrigin.indexOf("bm-eas-") != -1) {
        var device = entry['origin'].substring(7);
        entry['origin'] = 'ActiveSync: ' + device;
      } else if ((lowerOrigin.indexOf("dav") != -1) && (lowerOrigin.indexOf("card") != -1)) {
        entry['origin'] = 'CardDAV';
      } else if ((lowerOrigin.indexOf("dav") != -1) && (lowerOrigin.indexOf("cal") != -1)) {
        entry['origin'] = 'CalDAV';
      } else if (lowerOrigin.indexOf("dav") != -1) {
        entry['origin'] = 'DAV';
      } else if ((lowerOrigin.indexOf("internal-system") != -1) || (lowerOrigin == 'system')) {
        entry['origin'] = 'System';
      } else if (lowerOrigin.indexOf("outlook") != -1) {
        var versionOffset = lowerOrigin.indexOf("otlk") + 5;
        // otlk:12.0.x = Outlook 2007, otlk:14.0.x = Outlook 2010; otlk:15.0.x=
        // Outlook 2013, otlk:16.0.x= Outlook 2016
        var versionString = lowerOrigin.substring(versionOffset);
        var version = versionString;
        if (versionString.indexOf("12.0") != -1) {
          version = "2007";
        } else if (versionString.indexOf("14.0") != -1) {
          version = "2010";
        } else if (versionString.indexOf("15.0") != -1) {
          version = "2013";
        } else if (versionString.indexOf("16.0") != -1) {
          version = "2016";
        }
        entry['origin'] = "Outlook: " + version;
      } else if (lowerOrigin.indexOf("thunderbird") != -1) {
        var versionOffset = lowerOrigin.indexOf("tbird") + 6;
        var version = entry['origin'].substring(versionOffset);
        entry['origin'] = "Thunderbird: " + version;
      }
    } 
    
    if (entry['origin'] == null || entry['origin'] == "null") {
      entry['origin'] = "";
    }
    
    return entry;
  });
  this.setContent(net.bluemind.history.templates.dialogHistory({
    history : entries
  }));
  this.setVisible(true);
}
