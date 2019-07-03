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

Components.utils.import("resource://bm/bmUtils.jsm");
Components.utils.import("resource://bm/bmService.jsm");
Components.utils.import("resource://bm/core2/BMAuthService.jsm");

var gBMCalPrefs = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject
                    .getLogger("gBMCalPrefs: "),
    _login: {},
    _pwd: {},
    _srv: {},
    _authKey: null,
    _user: null,
    _calendarSharing: null,
    _calendarSub: null,
    init: function() {
        let timeZones = document.getElementById("timezone");
        for (let timeZone of TZ) {
            timeZones.appendItem(timeZone, timeZone);
        }
        let workHoursStart = document.getElementById("work_hours_start");
        let workHoursEnd = document.getElementById("work_hours_end");
        for (let i=0; i<24; i++) {
            let h = "";
            if (i < 10) h += "0";
            h += i;
            let h00 = h + ":00";
            let h30 = h + ":30";
            workHoursStart.appendItem(h00, i);
            workHoursStart.appendItem(h30, i + ".5");
            workHoursEnd.appendItem(h00, i);
            workHoursEnd.appendItem(h30, i + ".5");
        }
        
        let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]
            .getService(Components.interfaces.mozIJSSubScriptLoader);
        loader.loadSubScript("chrome://bm/content/core2/client/AuthenticationClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/UserSettingsClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/ContainersClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/ContainerManagementClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/UserClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/DirectoryClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/UserSubscriptionClient.js");
        loader.loadSubScript("chrome://bm/content/preferences/bmCalendarPreferencesCalendars.js");
        loader.loadSubScript("chrome://bm/content/preferences/bmCalendarPreferencesShare.js");
        loader.loadSubScript("chrome://bm/content/preferences/bmCalendarPreferencesSubscribe.js");


        if (!bmUtils.getSettings(this._login, this._pwd, this._srv, true)) {
          this._logger.error("cannot login");
        }
        
        this._calendarCRUD = gBMCalPrefsCalendars;
        this._calendarCRUD.init();
        
        this._calendarSharing = gBMCalPrefsShare;
        this._calendarSharing.init();

        this._calendarSub = gBMCalPrefsSubscribe;
        this._calendarSub.init();

        this._auth();
    },
    _auth: function() {
      let result = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
      let self = this;
      result.then(function(logged) {
          self._authKey = logged.authKey;
          self._user = logged.authUser;
          let client = new UserSettingsClient(self._srv.value, self._authKey, self._user.domainUid);
          return client.get(self._user.uid);
      }).then(function(settings) {
          return self._onGetSettings(settings);
      }).then(function() {
        let client = new ContainersClient(self._srv.value, self._authKey);
        let query = {"owner" : self._user.uid, "type" : "calendar"};
        return client.all(query);
      }).then(function(containers) {
        let selectCalendar = document.getElementById('selectCalendar');
        for (let cal of containers) {
          selectCalendar.appendItem(cal.name, cal.uid);
        }
      }).then(function() {
        document.getElementById('selectCalendar').value = "calendar:Default:" + self._user.uid;
        self._calendarSharing.calendarSelect("calendar:Default:" + self._user.uid);
      }).catch(function(err) {
        self._logger.error(err);
        self._showError(err);
      });
    },
    _showError: function(aErr) {
        let errorCode = (aErr instanceof BMError) ? aErr.message : "errors.UNKWNOWN_ERROR";
        let errorMessage = bmUtils.getLocalizedString(errorCode);
        let prefNotificationBar = document.getElementById("prefNotificationBar");
        prefNotificationBar.appendNotification(errorMessage,
                                errorCode,
                                "",
                                prefNotificationBar.PRIORITY_CRITICAL_LOW,
                                [],
                                null);
    },
    _onGetSettings: function(settings) {
        this.userSettings = settings;
        document.getElementById("timezone").value = settings["timezone"];
        document.getElementById("date").value = settings["date"];
        document.getElementById("timeformat").value = settings["timeformat"];
        document.getElementById("day_weekstart").value = settings["day_weekstart"];
        document.getElementById("defaultview").value = settings["defaultview"];
        document.getElementById("show_declined_events").value = settings["show_declined_events"];
        document.getElementById("showweekends").value = settings["showweekends"];
        let workDays = settings["working_days"].split(",");
        for (let wDay of workDays) {
            if (wDay) {
                document.getElementById("working_days:" + wDay).checked = true;
            }
        }
        document.getElementById("work_hours_start").value = settings["work_hours_start"];
        document.getElementById("work_hours_end").value = settings["work_hours_end"];
    },
    onAccept: function() {

      let settings = this.userSettings;
      settings["timezone"] = document.getElementById("timezone").value;
      settings["date"] = document.getElementById("date").value;
      settings["timeformat"] = document.getElementById("timeformat").value;
      settings["day_weekstart"] = document.getElementById("day_weekstart").value;
      settings["defaultview"] = document.getElementById("defaultview").value;
      settings["show_declined_events"] = document.getElementById("show_declined_events").value;
      settings["showweekends"] = document.getElementById("showweekends").value;
      let days = ["mon", "tue", "wed", "thu", "fri", "sat", "sun"];
      let wDays = [];
      for (let day of days) {
          if (document.getElementById("working_days:" + day).checked) {
              wDays.push(day);
          }
      }
      settings["working_days"] = wDays.toString();
      settings["work_hours_start"] = document.getElementById("work_hours_start").value;
      settings["work_hours_end"] = document.getElementById("work_hours_end").value;
      

      let subsChanged = this._calendarSub.hasChanged();
      let subs = this._calendarSub.getSubscription();
      let unsubs = this._calendarSub.getUnsubscription();
      let selectedCalendar = this._calendarSharing.getSelected();
      let acls = this._calendarSharing.getValues();

      let calsChanged = this._calendarCRUD.hasChanged();
      let cals = this._calendarCRUD.getCalendars();
      let added = this._calendarCRUD.getAdded();
      let removed = this._calendarCRUD.getRemoved();
      let updated = this._calendarCRUD.getUpdated();
      
      let result = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
      let self = this;
      result.then(function(logged) {
          self._authKey = logged.authKey;
          self._user = logged.authUser;
          let client = new UserSettingsClient(self._srv.value, logged.authKey, self._user.domainUid);
          return client.set(self._user.uid, settings);
      }, function(aRejectReason) {
          self._logger.error(aRejectReason);
      }).then(function() {
        // calendars removed
        if (calsChanged) {
            let client = new ContainersClient(self._srv.value, self._authKey);
            let promises = removed.keys().map(function(rmed) {
                client.delete_(rmed);
            }, self);
            return Promise.all(promises);
        }
      }).then(function() {
        // calendars created
        if (calsChanged) {
            let client = new ContainersClient(self._srv.value, self._authKey);
            let sub = new UserSubscriptionClient(self._srv.value, self._authKey, self._user.domainUid);
            let mgmt;
            let promises = added.keys().map(function(id) {
                let newCal = cals.get(id);
                let descriptor = {
                    defaultContainer: false,
                    domainUid: self._user.domainUid,
                    owner: self._user.uid,
                    name: newCal.name,
                    type: "calendar",
                    uid: id,
                    writable: true
                };
                let ret = client.create(id, descriptor);
                ret.then(function() {
                    mgmt = new ContainerManagementClient(self._srv.value, self._authKey, id);
                    return mgmt.setAccessControlList([{subject: self._user.uid, verb: "All"}]);
                }).then(function(id) {
                    return sub.subscribe(self._user.uid, [{containerUid: id, offlineSync: true}]);
                }).catch(function(err) {
                    self._logger.error(err);
                });
            }, self);
            return Promise.all(promises);
        }
      }).then(function() {
        // calendars name changed
        if (updated) {
            let client = new ContainersClient(self._srv.value, self._authKey);
            let promises = updated.map(function(up) {
                client.update(up.uid, {defaultContainer: false, name: up.name});
            }, self);
            return Promise.all(promises);
        }
      }).then(function() {
        // sharing
        if (selectedCalendar != null) {
            let client = new ContainerManagementClient(self._srv.value, self._authKey, selectedCalendar);
            client.setAccessControlList(acls);
            return;
        }
      }).then(function() {
        if (subsChanged) {
          let client = new UserSubscriptionClient(self._srv.value, self._authKey, self._user.domainUid);
          client.subscribe(self._user.uid, subs.values());
        }
        return;
      }).then(function() {
        if (subsChanged) {
          let client = new UserSubscriptionClient(self._srv.value, self._authKey, self._user.domainUid);
          client.unsubscribe(self._user.uid, unsubs.keys()); 
        }
        return;
      }).catch(function(err) {
          self._logger.error(err);
          self._showError(err);
      });

  }
};
