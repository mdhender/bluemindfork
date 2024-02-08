/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");
var { BMAuthService } = ChromeUtils.import("chrome://bm/content/modules/core2/BMAuthService.jsm");

function getWindow() {
    // TB 115 : message window is inside is own browser
    return window?.gTabmail?.tabInfo[0]?.chromeBrowser?.contentWindow?.messageBrowser?.contentWindow ?? window;
}

var gBMIcsBandal = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMIcsBandal: "),
    onLoad: function() {
        gBMIcsBandal._logger.debug("onLoad");
        
        console.trace("gBMIcsBandal onLoad");

        let win = getWindow();
        if (!win || !win.gMessageListeners) return false;
        win.gMessageListeners.push(gBMIcsBandal);
        
        return true;
    },
    hideBandal: function() {
        let win = getWindow();
        let bandal = win.document.getElementById("bm-ics-bandal");
        bandal.collapsed = true;
        let counter = win.document.getElementById("bm-counter-bandal");
        counter.collapsed = true;
    },
    changeBandalLinksOnclick: function(aState) {
        let win = getWindow();
        let part = win.document.getElementById("bm-ics-bandal-participation");
        let ofDisplayName = win.document.getElementById("bm-ics-bandal-participation-of");
        if (this._otherUserDisplayName != null) {
            part.setAttribute("value", bmUtils.getLocalizedString("icsbandal.participationOf"));
            ofDisplayName.setAttribute("value", this._otherUserDisplayName);
            ofDisplayName.setAttribute("hidden", "false");
        } else {
            part.setAttribute("value", bmUtils.getLocalizedString("icsbandal.participation"));
            ofDisplayName.setAttribute("value", "");
            ofDisplayName.setAttribute("hidden", "true");
        }
        let accept = win.document.getElementById("bm-ics-bandal-accept");
        let tentat = win.document.getElementById("bm-ics-bandal-tentative");
        let declin = win.document.getElementById("bm-ics-bandal-decline");
        let cpParam = ",'" + this._attendeeDir + "'";
        if (aState == "Accepted") {
            accept.setAttribute("class", "highlight");
            accept.setAttribute("onclick", "");
            tentat.setAttribute("class", "text-link");
            tentat.setAttribute("onclick", "gBMIcsBandal.changeParticipation('Tentative'" + cpParam + ");");
            declin.setAttribute("class", "text-link");
            declin.setAttribute("onclick", "gBMIcsBandal.changeParticipation('Declined'" + cpParam + ");");
        } else if (aState == "Tentative") {
            accept.setAttribute("class", "text-link");
            accept.setAttribute("onclick", "gBMIcsBandal.changeParticipation('Accepted'" + cpParam + ");");
            tentat.setAttribute("class", "highlight");
            tentat.setAttribute("onclick", "");
            declin.setAttribute("class", "text-link");
            declin.setAttribute("onclick", "gBMIcsBandal.changeParticipation('Declined'" + cpParam + ");");
        } else if (aState == "Declined") {
            accept.setAttribute("class", "text-link");
            accept.setAttribute("onclick", "gBMIcsBandal.changeParticipation('Accepted'" + cpParam + ");");
            tentat.setAttribute("class", "text-link");
            tentat.setAttribute("onclick", "gBMIcsBandal.changeParticipation('Tentative'" + cpParam + ");");
            declin.setAttribute("class", "highlight");
            declin.setAttribute("onclick", "");
        } else {
            accept.setAttribute("class", "text-link");
            accept.setAttribute("onclick", "gBMIcsBandal.changeParticipation('Accepted'" + cpParam + ");");
            tentat.setAttribute("class", "text-link");
            tentat.setAttribute("onclick", "gBMIcsBandal.changeParticipation('Tentative'" + cpParam + ");");
            declin.setAttribute("class", "text-link");
            declin.setAttribute("onclick", "gBMIcsBandal.changeParticipation('Declined'" + cpParam + ");");
        }
    },
    onStartHeaders: function() {
        gBMIcsBandal.hideBandal();
        this._containerUid = null;
        this._ressourceId = null;
        this._attendeeDir = null;
        this._otherUserDisplayName = null;
        this._eventItemUids = [];
        this._bandalKind = null;
        this._clearPartBandal();
        this._clearCounterBandal();
    },
    _clearPartBandal: function() {
        let win = getWindow();
        let partRow = win.document.getElementById("bm-ics-bandal-partRow");
        partRow.setAttribute("hidden" , "false");
        let accept = win.document.getElementById("bm-ics-bandal-accept");
        accept.setAttribute("class", "text-link");
        let tentative = win.document.getElementById("bm-ics-bandal-tentative");
        tentative.setAttribute("class", "text-link");
        let decline = win.document.getElementById("bm-ics-bandal-decline");
        decline.setAttribute("class", "text-link");
        let title = win.document.getElementById("bm-ics-bandal-title");
        let when = win.document.getElementById("bm-ics-bandal-when");
        let where = win.document.getElementById("bm-ics-bandal-where");
        title.setAttribute("value", "");
        when.setAttribute("value", "");
        where.setAttribute("value", "");
    },
    _clearCounterBandal: function() {
        let win = getWindow();
        let counterRow = win.document.getElementById("bm-counter-bandal-decisionRow");
        counterRow.setAttribute("hidden" , "false");
        let accept = win.document.getElementById("bm-counter-bandal-accept");
        accept.setAttribute("class", "text-link");
        let decline = win.document.getElementById("bm-counter-bandal-decline");
        decline.setAttribute("class", "text-link");
        let title = win.document.getElementById("bm-counter-bandal-title");
        let original = win.document.getElementById("bm-counter-bandal-original");
        let where = win.document.getElementById("bm-counter-bandal-where");
        let proposed = win.document.getElementById("bm-counter-bandal-proposed");
        title.setAttribute("value", "");
        original.setAttribute("value", "");
        where.setAttribute("value", "");
        proposed.setAttribute("value", "");
    },
    onEndHeaders: function() {
        let dispMessage;
        if (window.gMessageDisplay) {
            dispMessage = window.gMessageDisplay.displayedMessage;
        } else {
            dispMessage = window.gTabmail.currentAboutMessage.gMessage;
        }
        if (!dispMessage) return;
        let f = dispMessage.folder;
        if (!f) return;
        gBMIcsBandal._logger.debug("f.URI:" + f.URI);
        let imapFolder = null;
        try {
            imapFolder = f.QueryInterface(Components.interfaces.nsIMsgImapMailFolder);
        } catch(e) {
            this._logger.debug("cannot query interface to IMAP folder: " + f.URI + ", " + e);
        }
        if (!imapFolder) {
            return;
        }
        gBMIcsBandal._checkInBmMailAccount(imapFolder);

        let tags = dispMessage.getStringProperty("keywords") || "";
        if (tags.indexOf("bmeventreadonly") != -1) {
            gBMIcsBandal._logger.info("readonly meeting request managed by a delegate");
            return;
        }

        MsgHdrToMimeMessage(dispMessage, null, function(aMsgHdr, aMimeMsg) {
            if (aMimeMsg) {
                let uids = aMimeMsg.headers["x-bm-event"];
                let cancel = aMimeMsg.headers["x-bm-event-canceled"];
                let resourceId = aMimeMsg.headers["x-bm-resourcebooking"];
                let counter = aMimeMsg.headers["x-bm-event-countered"];
                let calendarUid = aMimeMsg.headers["x-bm-calendar"];
                let private = aMimeMsg.headers["x-bm-event-private"];
                gBMIcsBandal._logger.debug("x-bm-event:" + uids);
                gBMIcsBandal._logger.debug("x-bm-event-canceled:" + cancel);
                gBMIcsBandal._logger.debug("x-bm-resourcebooking:" + resourceId);
                gBMIcsBandal._logger.debug("x-bm-event-countered:" + counter);
                gBMIcsBandal._logger.debug("x-bm-calendar:" + calendarUid);
                gBMIcsBandal._logger.debug("x-bm-event-private:" + private);
                if (uids || cancel || resourceId || counter) {
                    gBMIcsBandal._hideLightingImipBar();
                }
                if (uids) {
                    gBMIcsBandal._getRsvpEvent(uids);
                    if (gBMIcsBandal._event) {
                        let msg = {};
                        msg.bmResourceId = resourceId;
                        msg.bmCalendar = calendarUid;
                        msg.bmPrivate = private;
                        msg.bmBandalKind = "PART";
                        gBMIcsBandal.onBmIcsMail(msg);
                    }
                } else if (counter) {
                    gBMIcsBandal._getCounterEvent(counter);
                    if (gBMIcsBandal._event) {
                        let msg = {};
                        msg.bmResourceId = resourceId;
                        msg.bmCalendar = calendarUid;
                        msg.bmBandalKind = "COUNTER";
                        gBMIcsBandal.onBmIcsMail(msg);
                    }
                }
            }
        }, true, {saneBodySize: true, partsOnDemand: true});
    },
    _bmAccountUser: null,
    _checkInBmMailAccount: function(folder) {
        if (!this._bmAccountUser) {
            let user = {};
            if (bmUtils.getSettings(user, {}, {}, false) && user.value.indexOf(folder.username) != -1) {
                this._bmAccountUser = folder.username;
                this._logger.debug("BM msg account user is:" + folder.username);
            }
        }
        if (this._bmAccountUser && this._bmAccountUser == folder.username) {
            this._logger.debug("In BM msg account");
            this._hideLightingImipBar();
        }
    },
    _hideLightingImipBar: function() {
        let win = getWindow();
        window.setTimeout(function() {
            let imipBar = win.document.getElementById("imip-bar");
            if (imipBar) {
                gBMIcsBandal._logger.debug("hide lightning imip bar");
                imipBar.collapsed = true;
            }
        }, 100);
    },
    _parseHeaderValues: function(aHeader) {
        let value, attributes = {};
        aHeader.split("; ").forEach((attribute) => {
            let properties = attribute.split('=');
            if (properties.length === 1) {
                value = attribute; 
            } else {
                let propValue = properties[1]; 
                attributes[properties[0]] = propValue?.substring(1, propValue.length -1);
            }
        });
        return {value, attributes};
    },
    _getRsvpEvent: function(aHeaders) {
        this._event = null;
        let header = aHeaders[0].trim();
        //44fed5ac-3fd8-45af-adf5-b7517a7d7c02; rsvp="true"; std="1704893400000"; major="true"; end="1704897000000"; seq="0"
        let {value, attributes} = this._parseHeaderValues(header);
        if (attributes.rsvp === "true") {
            let event = {
                eventUid: value,
                recurid: attributes.recurid
            }
            this._event = event;
            this._logger.debug("Invite:" + event.eventUid + ", recurid:" + event.recurid);
        }
    },
    _getCounterEvent: function(aHeaders) {
        this._event = null;
        let header = aHeaders[0].trim();
        //d7b9e8fc-da66-4c54-a789-de827496ec44; originator="david@bm.lan"; recurid="2016-12-09T12:30:00.000+01:00"
        let {value, attributes} = this._parseHeaderValues(header);
        let event = {
            eventUid: value,
            recurid: attributes.recurid,
            orignator: attributes.originator
        }
        this._event = event;
        this._logger.debug("Counter:" + event.eventUid + ", originator:" + event.originator + ", recurid:" + event.recurid);
    },
    onEndAttachments: function() {},
    onBeforeShowHeaderPane: function() {},
    onBmIcsMail: function(msg) {;
        this._logger.info("onBmIcsMail: " + msg.bmBandalKind);
        this._bandalKind = msg.bmBandalKind;
        if (msg.bmResourceId) {
            this._attendeeType = "Resource";
            this._ressourceId = msg.bmResourceId;
        } else {
            this._attendeeType = "Individual";
        }
        
        if (bmUtils.getSettings(this._login, this._pwd, this._srv, false)) {
            let self = this;
            self._auth().then(function() {
                if (msg.bmCalendar && self._containerUid != msg.bmCalendar) {
                    return self._getOtherCalendar(msg.bmCalendar);
                }
                if (msg.bmPrivate) {
                    return self._checkPrivateEventNotSentToDelegates();
                }
                return;
            }).then(function() {
                return self._getSeriesAndEvent(self._event);
            }).then(function(seriesAndEvent) {
                if (seriesAndEvent == null) {
                    self._logger.debug("series not found");
                    if (self._bandalKind == "PART") {
                        self._showNotification(new BMError("errors.imip.part-event-not-found"), "WARNING");
                    } else {
                        self._showNotification(new BMError("errors.imip.counter-event-not-found"), "INFO");
                    }
                    return;
                }
                if (self._bandalKind == "PART") {
                    self._participationBandal(seriesAndEvent);
                } else {
                    self._counterBandal(seriesAndEvent, self._event);
                }
            }).catch(function(err) {
                self._logger.error(err);
                self._showNotification(err, "ERROR");
            });
        } else {
            this._logger.debug("not enouth settings");
        }
    },
    init: function() {
        let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]
                .getService(Components.interfaces.mozIJSSubScriptLoader);
        loader.loadSubScript("chrome://bm/content/core2/client/AuthenticationClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/CalendarClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/ContainersClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/DirectoryClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/UserClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/MailboxesClient.js");
        this._logger.debug("init");
    },
    _onError: function(aError) {
        this._logger.error(aError);
        this._finalCallback();
    },
    _login: {},
    _pwd: {},
    _srv: {},
    _authKey: null,
    _user: null,
    _containerUid: null,
    _ressourceId: null,
    _attendeeDir: null,
    _auth: function() {
        let auth = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
        let self = this;
        return auth.then(function(logged) {
            self._authKey = logged.authKey;
            self._user = logged.authUser;
            if (!self._containerUid) {
                //init for logged user, will be overwritten if for another user
                if (self._ressourceId) {
                    self._containerUid = "calendar:" + self._ressourceId;
                    self._attendeeDir = "bm://" + self._user.domainUid + "/resources/" + self._ressourceId;
                } else {
                    self._containerUid = "calendar:Default:" + logged.authUser.uid;
                    self._attendeeDir = "bm://" + self._user.domainUid + "/users/" + logged.authUser.uid;
                }
            }
            return Promise.resolve();
        });
    },
    _checkPrivateEventNotSentToDelegates: function() {
        let client = new MailboxesClient(this._srv.value, this._authKey, this._user.domainUid);
        let self = this;
        let result = client.getMailboxDelegationRule(this._user.uid);
        return result.then(function(rule) {
            if (rule?.delegateUids.length) {
                self._showNotification(new BMError("errors.imip.warn-private-event-with-delegates"), "WARNING");
            }
        });
    },
    _showNotification: function(aErr, aPriority) {
        let errorCode = (aErr instanceof BMError) ? aErr.message : "errors.UNKWNOWN_ERROR";
        let errorMessage = bmUtils.getLocalizedString(errorCode);
        let msgNotificationBar = document.getElementById("msgNotificationBar");
        if (!msgNotificationBar) {
            //TB >= 78
            msgNotificationBar = getWindow().gMessageNotificationBar.msgNotificationBar;
        }
        let priority;
        switch (aPriority) {
            case "INFO":
                priority = msgNotificationBar.PRIORITY_INFO_LOW;
                break;
            case "WARNING":
                priority = msgNotificationBar.PRIORITY_WARNING_LOW;
            case "ERROR":
            default:
                priority = msgNotificationBar.PRIORITY_ERROR_LOW;
                break;
        }
        if (!window.gMessageDisplay) {
            //TB 115
            msgNotificationBar.appendNotification(errorCode, {
                priority: priority,
                label: errorMessage
            }, []);
        } else {
            msgNotificationBar.appendNotification(errorMessage,
                                errorCode,
                                "",
                                priority,
                                [],
                                null);
        }
    },
    _getSeriesAndEvent: function(aEvent) {
        let cal = new CalendarClient(this._srv.value, this._authKey, this._containerUid);
        let result = cal.getByIcsUid(aEvent.eventUid);
        return result.then(function(items) {
            let itemUid = null;
            let vevent = null;
            let vseries = null;
            for (let itemValue of items) {
                let series = itemValue.value;
                if (aEvent.recurid) {
                    for(let occ of series.occurrences) {
                        if (occ.recurid.iso8601 == aEvent.recurid) {
                            vevent = occ;
                            vseries = series;
                            itemUid = itemValue.uid;
                            break;
                        }
                    }
                } else {
                    itemUid = itemValue.uid;
                    vevent = series.main;
                    vseries = series;
                }
            }
            if (vevent) {
                return {
                    itemUid: itemUid,
                    series: vseries,
                    vevent: vevent 
                };
            }
            //not found
            return null;
        });
    },
    _participationBandal: function(seriesAndEvent) {
        let state;
        for (let attendee of seriesAndEvent.vevent.attendees) {
            this._logger.debug("attendee dir:" + attendee.dir + " type:" + attendee.cutype);
            if (attendee.dir == this._attendeeDir) {
                state = attendee.partStatus;
            }
        }

        this._logger.info("state:" + state);
        this._fillPartBandal(seriesAndEvent.vevent);
        this.changeBandalLinksOnclick(state);
        
        let win = getWindow();
        let bandal = win.document.getElementById("bm-ics-bandal");
        bandal.collapsed = false;
    },
    _getOtherCalendar: function(aCalendarUid) {
        let cc = new ContainersClient(this._srv.value, this._authKey);
        let self = this;
        let res =  cc.get(aCalendarUid);
        return res.then(function(otherCal) {
            if (otherCal == null) {
                self._logger.info("other calendar[" + aCalendarUid + "] not found or not readable");
                return Promise.reject();
            } else {
                if (!otherCal.writable) {
                    let win = getWindow();
                    self._logger.info("other calendar[" + aCalendarUid + "] is not writable");
                    let partRow = win.document.getElementById("bm-ics-bandal-partRow");
                    partRow.setAttribute("hidden" , "true");
                    let counterRow = win.document.getElementById("bm-counter-bandal-decisionRow");
                    counterRow.setAttribute("hidden" , "true");
                }
            }
            let dir = new DirectoryClient(self._srv.value, self._authKey, self._user.domainUid);
            return dir.findByEntryUid(otherCal.owner);
        }).then(function(dirEntry) {
            self._otherUserDisplayName = dirEntry.displayName;
            self._userUid = dirEntry.entryUid;
            self._containerUid = aCalendarUid;
            self._attendeeDir = "bm://" + dirEntry.path;
            return Promise.resolve();
        });
    },
    _fillPartBandal: function(aEvent) {
        let win = getWindow();
        let title = win.document.getElementById("bm-ics-bandal-title");
        let when = win.document.getElementById("bm-ics-bandal-when");
        let where = win.document.getElementById("bm-ics-bandal-where");
        title.setAttribute("value", aEvent.summary);
        when.setAttribute("value",  this._dateString(aEvent.dtstart, aEvent.dtend));
        where.setAttribute("value", aEvent.location != null ? aEvent.location : "");
    },
    _dateString: function(dtstart, dtend) {
        let start = new Date(dtstart.iso8601);
        let end = new Date(dtend.iso8601);
        let dwhen = "";
        if (dtstart.precision == "Date") {
            let dstart = start.toLocaleDateString();
            let dend = end.toLocaleDateString();
            if (dstart == dend) {
                dwhen = dstart;
            } else {
                dwhen = dstart + " - " + dend;
            }
        } else {
            dwhen = start.toLocaleString() + " - " + end.toLocaleString();
        }
        return dwhen;
    },
    changeParticipation: function(aState, aAttDir) {
        this._logger.info("changeParticipation(" + aState + "," + aAttDir + ")");
        let self = this;
        self._auth().then(function() {
            self._setParticipation(aState, aAttDir);
        }).catch(function(err) {
            self._logger.error(err);
            self._showNotification(err, "ERROR");
        });
    },
    _setParticipation: function(aState, aAttDir) {
        let cal = new CalendarClient(this._srv.value, this._authKey, this._containerUid);
        let result = cal.getByIcsUid(this._event.eventUid);
        let self = this;
        result.then(function(items) {
            let changes = {
                modify:[]
            };
            for (let itemValue of items) {
                let series = itemValue.value;
                let sendNotif = false;
                let partChanged = false;
                let vevent;
                if (!self._event.recurid && series.main) {
                    vevent = series.main;
                } else {
                    for(let occ of series.occurrences) {
                        if (occ.recurid.iso8601 == self._event.recurid) {
                            vevent = occ;
                            break;
                        }
                    }
                }
                if (vevent) {
                    for (let attendee of vevent.attendees) {
                        self._logger.debug("attendee dir:" + attendee.dir + " type:" + attendee.cutype);
                        if (attendee.dir == aAttDir) {
                            attendee.partStatus = aState;
                            partChanged = true;
                        }
                    }
                    if (vevent.organizer.dir != aAttDir) {
                        sendNotif = true;
                    }
                }
                if (partChanged) {
                    changes.modify.push({
                        uid: itemValue.uid,
                        value: series,
                        sendNotification: sendNotif
                    });
                }
            }
            return cal.updates(changes);
        }).then(function() {
            self.changeBandalLinksOnclick(aState);
        }).catch(function(err) {
            self._logger.error(err);
            self._showNotification(err, "ERROR");
        });
    },
    _counterBandal: function(seriesAndEvent, event) {
        let counter = this._getCounter(seriesAndEvent, event);
        if (counter == null) {
            this._showNotification(new BMError("errors.imip.counter-counter-not-found"), "INFO");
            return;
        }
        this._fillCounterBandal(seriesAndEvent.vevent, counter);
        
        let win = getWindow();
        let bandal = win.document.getElementById("bm-counter-bandal");
        bandal.collapsed = false;
    },
    _getCounter: function(seriesAndEvent, event) {
        for (let c of seriesAndEvent.series.counters) {
            if (c.originator.email == event.originator) {
                if ((event.recurid == null && c.counter.recurid == null)
                    || (event.recurid != null && c.counter.recurid != null && c.counter.recurid.iso8601 == event.recurid)) {
                    return c;
                }
            }
        }
        return null;
    },
    _fillCounterBandal: function(aEvent, counter) {
        let win = getWindow();
        let title = win.document.getElementById("bm-counter-bandal-title");
        let original = win.document.getElementById("bm-counter-bandal-original");
        let where = win.document.getElementById("bm-ics-bandal-where");
        let proposed = win.document.getElementById("bm-counter-bandal-proposed");
        title.setAttribute("value", aEvent.summary);
        original.setAttribute("value",  this._dateString(aEvent.dtstart, aEvent.dtend));
        where.setAttribute("value", aEvent.location != null ? aEvent.location : "");
        proposed.setAttribute("value", this._dateString(counter.counter.dtstart, counter.counter.dtend));

        let decision = win.document.getElementById("bm-counter-bandal-decision");
        if (this._otherUserDisplayName != null) {
            decision.setAttribute("value", bmUtils.getLocalizedString("counterbandal.acceptFor") + " " + this._otherUserDisplayName);
        } else {
            decision.setAttribute("value", bmUtils.getLocalizedString("counterbandal.accept"));
        }

        let accept = win.document.getElementById("bm-counter-bandal-accept");
        let decline = win.document.getElementById("bm-counter-bandal-decline");
        accept.setAttribute("onclick", "gBMIcsBandal.acceptCounter(true)");
        decline.setAttribute("onclick", "gBMIcsBandal.acceptCounter(false)"); 
    },
    acceptCounter: function(accepted) {
        this._logger.info("acceptCounter(" + accepted + ")");
        let event = this._event;
        let self = this;
        self._auth().then(function() {
            return self._getSeriesAndEvent(event);
        }).then(function(seriesAndEvent) {
            if (seriesAndEvent == null) {
                return Promise.reject(new BMError("errors.imip.counter-event-not-found"));
            }
            let series = seriesAndEvent.series;
            let vevent = seriesAndEvent.vevent;
            let counter = self._getCounter(seriesAndEvent, event);
            if (counter == null) {
                return Promise.reject(new BMError("errors.imip.counter-event-not-found"));
            }
            if (accepted) {
                series.counters = [];
                vevent.dtstart = counter.counter.dtstart;
                vevent.dtend = counter.counter.dtend;
                for (let attendee of vevent.attendees) {
                    attendee.partStatus = "NeedsAction";
                    attendee.rsvp = true;
                }
                vevent.sequence++;
            } else {
                let i = 0;
                for (let c of seriesAndEvent.series.counters) {
                    if (c.originator.email == event.originator) {
                        if ((event.recurid == null && c.counter.recurid == null)
                            || (event.recurid != null && c.counter.recurid != null && c.counter.recurid.iso8601 == event.recurid)) {
                            series.counters.splice(i, 1);
                            break;
                        }
                    }
                    i++;
                }
            }

            let cal = new CalendarClient(self._srv.value, self._authKey, self._containerUid);
            let changes = {
                modify:[]
            };
            changes.modify.push({
                uid: seriesAndEvent.itemUid,
                value: series,
                sendNotification: true
            });
            return cal.updates(changes);
        }).then(function() {
            let win = getWindow();
            let accept = win.document.getElementById("bm-counter-bandal-accept");
            let decline = win.document.getElementById("bm-counter-bandal-decline");
            accept.removeAttribute("onclick");
            decline.removeAttribute("onclick");
            if (accepted) {
                accept.setAttribute("class", "highlight");
            } else {
                decline.setAttribute("class", "highlight");
            }
        }).catch(function(err) {
            self._logger.error(err);
            self._showNotification(err, "ERROR");
        });
    },
    onUnload: function() {
        let win = getWindow();
        for (let i = 0; i < win.gMessageListeners.length; i++) {
            if (win.gMessageListeners[i] === gBMIcsBandal) {
                gBMIcsBandal._logger.info("remove message listener");
                win.gMessageListeners.splice(i, 1);
                break;
            }
        }
    }
}

gBMIcsBandal.init();
if (window.gMessageDisplay) {
    // TB < 115
    if (document.getElementById("msgHeaderView") && document.getElementById("msgHeaderView").loaded) {
        gBMIcsBandal.onLoad();
    } else {
        window.addEventListener("messagepane-loaded", gBMIcsBandal.onLoad, true);
    }
}