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

var gBMIcsBandal = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMIcsBandal: "),
    onLoad: function() {
        gBMIcsBandal._logger.debug("onLoad");
        gMessageListeners.push(gBMIcsBandal);
        //extend tbHideMessageHeaderPane to remove bandal on folder change
        gBMIcsBandal.tbHideMessageHeaderPaneOriginal = HideMessageHeaderPane;
        gBMIcsBandal.tbHideMessageHeaderPane = HideMessageHeaderPane;
        HideMessageHeaderPane = function ltnHideMessageHeaderPane() {
            gBMIcsBandal.hideBandal();
            gBMIcsBandal.tbHideMessageHeaderPane.apply(null, arguments);
        };
    },
    hideBandal: function() {
        let bandal = document.getElementById("bm-ics-bandal");
        bandal.collapsed = true;
        let counter = document.getElementById("bm-counter-bandal");
        counter.collapsed = true;
    },
    changeBandalLinksOnclick: function(aState) {
        let part = document.getElementById("bm-ics-bandal-participation");
        if (this._otherUserLogin != null) {
            part.setAttribute("value", bmUtils.getLocalizedString("icsbandal.participationOf") + " " + this._otherUserDisplayName);
        } else {
            part.setAttribute("value", bmUtils.getLocalizedString("icsbandal.participation"));
        }
        let accept = document.getElementById("bm-ics-bandal-accept");
        let tentat = document.getElementById("bm-ics-bandal-tentative");
        let declin = document.getElementById("bm-ics-bandal-decline");
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
        this._ressourceId = null;
        this._attendeeType = null;
        this._attendeeDir = null;
        this._otherUserLogin = null;
        this._otherUserDisplayName = null;
        this._otherUserId = null;
        this._eventItemUids = [];
        this._bandalKind = null;
        this._clearPartBandal();
        this._clearCounterBandal();
    },
    _clearPartBandal: function() {
        let partRow = document.getElementById("bm-ics-bandal-partRow");
        partRow.setAttribute("hidden" , "false");
        let accept = document.getElementById("bm-ics-bandal-accept");
        accept.setAttribute("class", "text-link");
        let tentative = document.getElementById("bm-ics-bandal-tentative");
        tentative.setAttribute("class", "text-link");
        let decline = document.getElementById("bm-ics-bandal-decline");
        decline.setAttribute("class", "text-link");
        let title = document.getElementById("bm-ics-bandal-title");
        let when = document.getElementById("bm-ics-bandal-when");
        let where = document.getElementById("bm-ics-bandal-where");
        title.setAttribute("value", "");
        when.setAttribute("value", "");
        where.setAttribute("value", "");
    },
    _clearCounterBandal: function() {
        let counterRow = document.getElementById("bm-counter-bandal-decisionRow");
        counterRow.setAttribute("hidden" , "false");
        let accept = document.getElementById("bm-counter-bandal-accept");
        accept.setAttribute("class", "text-link");
        let decline = document.getElementById("bm-counter-bandal-decline");
        decline.setAttribute("class", "text-link");
        let title = document.getElementById("bm-counter-bandal-title");
        let original = document.getElementById("bm-counter-bandal-original");
        let where = document.getElementById("bm-counter-bandal-where");
        let proposed = document.getElementById("bm-counter-bandal-proposed");
        title.setAttribute("value", "");
        original.setAttribute("value", "");
        where.setAttribute("value", "");
        proposed.setAttribute("value", "");
    },
    onEndHeaders: function() {
        let dispMessage = gMessageDisplay.displayedMessage;
        if (!dispMessage) return;
        let f = dispMessage.folder;
        if (!f) return;
        gBMIcsBandal._logger.debug("f.URI:" + f.URI);
        let imapFolder = null;
        let otherLogin = null;
        try {
            imapFolder = f.QueryInterface(Components.interfaces.nsIMsgImapMailFolder);
        } catch(e) {
            this._logger.debug("cannot query interface to IMAP folder: " + f.URI + ", " + e);
        }
        if (!imapFolder) {
            return;
        }
        gBMIcsBandal._checkInBmMailAccount(imapFolder);

        let folderPart = "\"" + f.URI.split("/")[3] + "/\"";
        let inComServer = imapFolder.imapIncomingServer;
        if (folderPart == inComServer.publicNamespace) {
            gBMIcsBandal._logger.debug("in a shared public folder -> do not display ics bandal");
            return;
        } else {
            if (folderPart == inComServer.otherUsersNamespace) {
                //imap://nico%40test.lan@edge.test.lan/Autres%20utilisateurs/mehdi
                otherLogin = f.folderURL.split("/")[4];
                gBMIcsBandal._logger.debug("in a shared folder of user: " + otherLogin);
            }
        }

        MsgHdrToMimeMessage(dispMessage, null, function(aMsgHdr, aMimeMsg) {
            if (aMimeMsg) {
                let uids = aMimeMsg.headers["x-bm-event"];
                let cancel = aMimeMsg.headers["x-bm-canceled"];
                let resourceId = aMimeMsg.headers["x-bm-resourcebooking"];
                let counter = aMimeMsg.headers["x-bm-event-countered"];
                gBMIcsBandal._logger.debug("x-bm-event:" + uids);
                gBMIcsBandal._logger.debug("x-bm-canceled:" + cancel);
                gBMIcsBandal._logger.debug("x-bm-resourcebooking:" + resourceId);
                gBMIcsBandal._logger.debug("x-bm-event-countered:" + counter);
                if (uids || cancel || resourceId || counter) {
                    gBMIcsBandal._hideLightingImipBar();
                }
                if (uids) {
                    gBMIcsBandal._getRsvpEvents(uids);
                    if (gBMIcsBandal._events.length > 0) {
                        let msg = {};
                        msg.bmResourceId = resourceId;
                        msg.bmOtherLogin = otherLogin;
                        msg.bmBandalKind = "PART";
                        gBMIcsBandal.onBmIcsMail(msg);
                    }
                } else if (counter) {
                    gBMIcsBandal._getCounterEvent(counter);
                    if (gBMIcsBandal._events.length > 0) {
                        let msg = {};
                        msg.bmResourceId = resourceId;
                        msg.bmOtherLogin = otherLogin;
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
        window.setTimeout(function() {
            let imipBar = document.getElementById("imip-bar");
            if (imipBar) {
                gBMIcsBandal._logger.debug("hide lightning imip bar");
                imipBar.collapsed = true;
            }
        }, 100);
    },
    _getRsvpEvents: function(aHeaders) {
        this._events = [];
        let added = [];
        for (let i = 0; i < aHeaders.length; i++) {
            //8ce82499-8af9-497a-a74c-ce6902fab97a; recurid="2016-12-09T12:30:00.000+01:00"; rsvp="true"
            let h = aHeaders[i].split("; rsvp=");
            let uidAndRecurId = h[0];
            if (h.length == 2) {
                let r = h[1];
                let rsvp = r.substring(1, r.length -1);//"true"
                if (rsvp == "true" && added.indexOf(uidAndRecurId) == -1) {
                    //8ce82499-8af9-497a-a74c-ce6902fab97a; recurid="2016-12-09T12:30:00.000+01:00"
                    let u = uidAndRecurId.split("; recurid=");
                    let uid = u[0];
                    let recurId = null;
                    if (u.length == 2) {
                        let r = u[1];
                        recurId = r.substring(1, r.length -1);
                    }
                    let event = {
                        eventUid: uid,
                        recurid: recurId
                    };
                    added.push(uid);
                    this._events.push(event);
                    this._logger.debug("Invite:" + event.eventUid + ", recurid:" + event.recurid);
                }
            }
        }
    },
    _getCounterEvent: function(aHeaders) {
        this._events = [];
        for (let i = 0; i < aHeaders.length; i++) {
            //d7b9e8fc-da66-4c54-a789-de827496ec44; originator="david@bm.lan"; recurid="2016-12-09T12:30:00.000+01:00"
            let h = aHeaders[i].split("; recurid=");
            let uidAndOriginator = h[0].split("; originator=");
            let uid = uidAndOriginator[0];
            let orignator = uidAndOriginator[1].substring(1, uidAndOriginator[1].length -1);//"david@bm.lan"
            let recurId = null;
            if (h.length == 2) {
                recurId = h[1].substring(1, h[1].length -1);//"2016-12-09T12:30:00.000+01:00"
            }
            let event = {
                eventUid: uid,
                recurid: recurId,
                originator: orignator
            };
            this._events.push(event);
            this._logger.debug("Counter:" + event.eventUid + ", originator:" + event.originator + ", recurid:" + event.recurid);
        }
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
                if (msg.bmOtherLogin != null) {
                    self._otherUserLogin = msg.bmOtherLogin;
                    return self._getOtherUser(msg.bmOtherLogin);
                } else {
                    return;
                }
            }).then(function() {
                return self._getSeriesAndEvent(self._events[0]);
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
                    self._counterBandal(seriesAndEvent, self._events[0]);
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
    _userUid: null,
    _containerUid: null,
    _auth: function() {
        let auth = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
        let self = this;
        return auth.then(function(logged) {
            self._authKey = logged.authKey;
            self._user = logged.authUser;
            if (!self._userUid) {
                self._userUid = logged.authUser.uid;
            }
            if (self._ressourceId) {
                self._containerUid = "calendar:" + self._ressourceId;
                self._attendeeDir = "bm://" + self._user.domainUid + "/resources/" + self._ressourceId;
            } else {
                self._containerUid = "calendar:Default:" + self._userUid;
                self._attendeeDir = "bm://" + self._user.domainUid + "/users/" + self._userUid;
            }
            return Promise.resolve();
        });
    },
    _showNotification: function(aErr, aPriority) {
        let errorCode = (aErr instanceof BMError) ? aErr.message : "errors.UNKWNOWN_ERROR";
        let errorMessage = bmUtils.getLocalizedString(errorCode);
        let msgNotificationBar = document.getElementById("msgNotificationBar");
        if (!msgNotificationBar) {
            //TB >= 78
            msgNotificationBar = gMessageNotificationBar.msgNotificationBar;
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
        msgNotificationBar.appendNotification(errorMessage,
                            errorCode,
                            "",
                            priority,
                            [],
                            null);
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
        
        let bandal = document.getElementById("bm-ics-bandal");
        bandal.collapsed = false;
    },
    _getOtherUser: function(aLogin) {
        let dir = new DirectoryClient(this._srv.value, this._authKey, this._user.domainUid);
        let self = this;
        let query = {
            emailFilter: aLogin + "@" + this._user.domainUid,
            kindsFilter: ["USER"]
        }
        let res = dir.search(query);
        let dirEntry;
        return res.then(function(dirEntries) {
            if (dirEntries.total == 0) {
                this._logger.debug("user: " + query.emailFilter + " not found");
                return Promise.reject();
            } else {
                return dirEntries.values[0].value;
            }
        }).then(function(aDirEntry) {
            dirEntry = aDirEntry; 
            self._userUid = dirEntry.entryUid;
            let cc = new ContainersClient(self._srv.value, self._authKey);
            return cc.all({type: "calendar"});
        }).then(function(calendars) {
            let otherCal = null;
            for (let c of calendars) {
                if (c.defaultContainer && c.owner == self._userUid) {
                    otherCal = c;
                }
            }
            if (otherCal == null) {
                self._logger.info("no readable calendar found for user: " + aLogin);
                return Promise.reject();
            } else {
                if (!otherCal.writable) {
                    self._logger.info("calendar is not writable: " + aLogin);
                    let partRow = document.getElementById("bm-ics-bandal-partRow");
                    partRow.setAttribute("hidden" , "true");
                    let counterRow = document.getElementById("bm-counter-bandal-decisionRow");
                    counterRow.setAttribute("hidden" , "true");
                }
                self._otherUserDisplayName = dirEntry.displayName;
                self._userUid = dirEntry.entryUid;
                return Promise.resolve();
            } 
        });
    },
    _fillPartBandal: function(aEvent) {
        let title = document.getElementById("bm-ics-bandal-title");
        let when = document.getElementById("bm-ics-bandal-when");
        let where = document.getElementById("bm-ics-bandal-where");
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
        let result = cal.getByIcsUid(this._events[0].eventUid);
        let self = this;
        result.then(function(items) {
            let changes = {
                modify:[]
            };
            for (let itemValue of items) {
                let series = itemValue.value;
                let sendNotif = false;
                let partChanged = false;
                for (let event of self._events) {
                    let vevent;
                    if (!event.recurid && series.main) {
                        vevent = series.main;
                    } else {
                        for(let occ of series.occurrences) {
                            if (occ.recurid.iso8601 == event.recurid) {
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
            //TODO counter already accepted or declined
            return;
        }
        this._fillCounterBandal(seriesAndEvent.vevent, counter);
        
        let bandal = document.getElementById("bm-counter-bandal");
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
        let title = document.getElementById("bm-counter-bandal-title");
        let original = document.getElementById("bm-counter-bandal-original");
        let where = document.getElementById("bm-ics-bandal-where");
        let proposed = document.getElementById("bm-counter-bandal-proposed");
        title.setAttribute("value", aEvent.summary);
        original.setAttribute("value",  this._dateString(aEvent.dtstart, aEvent.dtend));
        where.setAttribute("value", aEvent.location != null ? aEvent.location : "");
        proposed.setAttribute("value", this._dateString(counter.counter.dtstart, counter.counter.dtend));

        let decision = document.getElementById("bm-counter-bandal-decision");
        if (this._otherUserLogin != null) {
            decision.setAttribute("value", bmUtils.getLocalizedString("counterbandal.acceptFor") + " " + this._otherUserDisplayName);
        } else {
            decision.setAttribute("value", bmUtils.getLocalizedString("counterbandal.accept"));
        }

        let accept = document.getElementById("bm-counter-bandal-accept");
        let decline = document.getElementById("bm-counter-bandal-decline");
        accept.setAttribute("onclick", "gBMIcsBandal.acceptCounter(true)");
        decline.setAttribute("onclick", "gBMIcsBandal.acceptCounter(false)"); 
    },
    acceptCounter: function(accepted) {
        this._logger.info("acceptCounter(" + accepted + ")");
        let event = this._events[0];
        let self = this;
        self._auth().then(function() {
            return self._getSeriesAndEvent(event);
        }).then(function(seriesAndEvent) {
            if (seriesAndEvent == null) {
                // TODO: event not found
                return Promise.reject();
            }
            let series = seriesAndEvent.series;
            let vevent = seriesAndEvent.vevent;
            let counter = self._getCounter(seriesAndEvent, event);
            if (counter == null) {
                // TODO: counter not found
                return Promise.reject();
            }
            let sendNotif = false;
            if (accepted) {
                sendNotif = true;
                series.counters = [];
                vevent.dtstart = counter.counter.dtstart;
                vevent.dtend = counter.counter.dtend;
                for (let attendee of vevent.attendees) {
                    attendee.partStatus = "NeedsAction";
                    attendee.rsvp = true;
                }
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
                sendNotification: sendNotif
            });
            return cal.updates(changes);
        }).then(function() {
            let accept = document.getElementById("bm-counter-bandal-accept");
            let decline = document.getElementById("bm-counter-bandal-decline");
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
        for (let i = 0; i < gMessageListeners.length; i++) {
            if (gMessageListeners[i] === gBMIcsBandal) {
                gBMIcsBandal._logger.info("remove message listener");
                gMessageListeners.splice(i, 1);
                break;
            }
        }
        HideMessageHeaderPane = gBMIcsBandal.tbHideMessageHeaderPaneOriginal;
    }
}

gBMIcsBandal.init();
if (document.getElementById("msgHeaderView") && document.getElementById("msgHeaderView").loaded) {
    gBMIcsBandal.onLoad();
} else {
    window.addEventListener("messagepane-loaded", gBMIcsBandal.onLoad, true);
}