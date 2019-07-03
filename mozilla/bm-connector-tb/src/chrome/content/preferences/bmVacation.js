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

var gBMVacation = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject
                    .getLogger("gBMVaction: "),
    _login: {},
    _pwd: {},
    _srv: {},
    _authKey: null,
    _user: null,
    _vacation: null,
    init: function() {
        let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]
                  .getService(Components.interfaces.mozIJSSubScriptLoader);
        loader.loadSubScript("chrome://bm/content/core2/client/MailboxesClient.js");

        if (!bmUtils.getSettings(this._login, this._pwd, this._srv, true)) {
            this._logger.error("cannot login");
            return;
        }
        let result = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
        let self = this;
        result.then(function(logged) {
            self._authKey = logged.authKey;
            self._user = logged.authUser;
            let client = new MailboxesClient(self._srv.value, self._authKey, self._user.domainUid);
            return client.getMailboxFilter(self._user.uid);
        }).then(function(mailFilter) {
            self._fill(mailFilter.vacation);
        }).catch(function(err) {
            self._logger.error(err);
            self._showError(err);
        });
    },
    _fill: function(aVacation) {
        this._vacation = aVacation;
        let radios = document.getElementById("radios-vacation");
        if (aVacation.enabled) {
            radios.selectedIndex = 1;
            this.vacEnabled(true);
        } else {
            radios.selectedIndex = 0;
            this.vacEnabled(false);
        }

        let subject = document.getElementById("text-subject");
        subject.value = aVacation.subject;
        let message = document.getElementById("text-message");
        message.value = aVacation.text;

        let start = document.getElementById("date-start");
        if (aVacation.start) {
            let dstart = new Date(aVacation.start.iso8601);
            //for strange raison datetimepicker.dateValue setter does not work here
            start.value = this._dateForDatePicker(dstart);
        }
        let end = document.getElementById("date-end");
        let checkEnd = document.getElementById("check-end");
        if (aVacation.end) {
            checkEnd.checked = true;
            let dend = new Date(aVacation.end.iso8601);
            end.value = this._dateForDatePicker(dend);
        } else {
            checkEnd.checked = false;
        }
    },
    _dateForDatePicker: function(aDate) {
        let month = aDate.getMonth();
        month = (month < 9) ? month = "0" + ++month : month + 1;
        let date = aDate.getDate();
        if (date < 10)
            date = "0" + date;
        return aDate.getFullYear() + "-" + month + "-" + date;
    },
    _showError: function(aErr) {
        let errorCode = (aErr instanceof BMError) ? aErr.message : "errors.UNKWNOWN_ERROR";
        let errorMessage = bmUtils.getLocalizedString(errorCode);
        let prefNotificationBar = document.getElementById("errorNotificationBar");
        prefNotificationBar.appendNotification(errorMessage,
                                errorCode,
                                "",
                                prefNotificationBar.PRIORITY_CRITICAL_LOW,
                                [],
                                null);
    },
    onAccept: function() {
        let prefNotificationBar = document.getElementById("errorNotificationBar");
        prefNotificationBar.removeAllNotifications();

        let radios = document.getElementById("radios-vacation");
        this._logger.debug("radios-vacation:" + radios.selectedIndex == 1);
        let enabled = radios.selectedIndex == 1;
       
        let vac;
        let toUpdate = false;
        if (enabled) {
            let subject = document.getElementById("text-subject");
            let message = document.getElementById("text-message");
            let start = document.getElementById("date-start");
            let dstart = start.dateValue;
            let endCheck = document.getElementById("check-end");
            let end = null;
            let dend = null;
            if (endCheck.checked) {
                end = document.getElementById("date-end");
                dend = end.dateValue;
            }
            if (!message.value) {
                this._showError(new BMError("errors.vacation.message"));
                return false;
            }
            if (dend && dend.getTime() < dstart.getTime()) {
                this._showError(new BMError("errors.vacation.end"));
                return false;
            }
            vac = {};
            vac.subject = subject.value;
            vac.text = message.value;
            vac.enabled = true;
            vac.start = this._dateToBmDate(dstart);
            vac.end = dend ? this._dateToBmDate(dend) : null;

            if (!this._vacation.enabled ||
                this._vacation.subject != vac.subject ||
                this._vacation.text != vac.text ||
                !this._sameBmDates(this._vacation.start, vac.start) ||
                !this._sameBmDates(this._vacation.end, vac.end)) {
                toUpdate = true;
            }
        } else {
            if (this._vacation.enabled) {
                vac = this._vacation;
                vac.enabled = false;
                toUpdate = true;
            }
        }

        if (toUpdate) {
            let result = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
            let self = this;
            let client;
            result.then(function(logged) {
                self._authKey = logged.authKey;
                self._user = logged.authUser;
                client = new MailboxesClient(self._srv.value, self._authKey, self._user.domainUid);
                return client.getMailboxFilter(self._user.uid);
            }).then(function(mailFilter) {
                mailFilter.vacation = vac;
                return client.setMailboxFilter(self._user.uid, mailFilter);
            }).catch(function(err) {
                self._logger.error(err);
                self._showError(err);
            });
        }
    },
    _dateToBmDate: function(aDate) {
        let bmDate = {};
        bmDate.precision = "DateTime";
        bmDate.timezone = "UTC";
        bmDate.iso8601 = aDate.toISOString();
        return bmDate;
    },
    _sameBmDates: function(d1, d2) {
        if (d1 != null) {
            if (d2 == null) return false;
            return d1.iso8601 == d2.iso8601;
        } else {
            if (d2 == null) return true;
            return this._sameBmDates(d2, d1);
        }
    },
    vacEnabled: function(aEnabled) {
        let disable = !aEnabled;
        document.getElementById("label-start").disabled = disable;
        document.getElementById("date-start").disabled = disable;
        document.getElementById("check-end").disabled = disable;
        if (disable) {
            document.getElementById("date-end").disabled = true;
        }
        document.getElementById("label-subject").disabled = disable;
        document.getElementById("text-subject").disabled = disable;
        document.getElementById("label-message").disabled = disable;
        document.getElementById("text-message").disabled = disable;
    }
};
