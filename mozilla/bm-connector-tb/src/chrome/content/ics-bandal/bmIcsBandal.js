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
Components.utils.import("resource://bm/core2/BMAuthService.jsm");
Components.utils.import("resource://gre/modules/Services.jsm");

var gBMIcsBandal = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMIcsBandal: "),
    onLoad: function() {
        gBMIcsBandal._logger.debug("onLoad");
        gMessageListeners.push(gBMIcsBandal);
        //extend tbHideMessageHeaderPane to remove bandal on folder change
        gBMIcsBandal.tbHideMessageHeaderPane = HideMessageHeaderPane;
        HideMessageHeaderPane = function ltnHideMessageHeaderPane() {
            gBMIcsBandal.hideBandal();
            gBMIcsBandal.tbHideMessageHeaderPane.apply(null, arguments);
        };
    },
    hideBandal: function() {
        let bandal = document.getElementById("bm-ics-bandal");
        bandal.collapsed = true;
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
    onEndHeaders: function() {
        let dispMessage = gMessageDisplay.displayedMessage;
        let f = dispMessage.folder;
        if (!f) return;
        gBMIcsBandal._logger.debug("f.URI:" + f.URI);
        let folderPart = "\"" + f.URI.split("/")[3] + "/\"";
        let imapFolder;
        try {
            imapFolder = f.QueryInterface(Components.interfaces.nsIMsgImapMailFolder);
        } catch(e) {
            this._logger.debug("cannot query interface to IMAP folder: " + f.URI + ", " + e);
            return;
        }
        let inComServer = imapFolder.imapIncomingServer;
        let otherLogin = null;
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
                gBMIcsBandal._logger.debug("x-bm-event:" + uids);
                let rsvpEventUids = gBMIcsBandal._getRsvpEventUids(uids);
                let resourceId = aMimeMsg.headers["x-bm-resourcebooking"];
                if (rsvpEventUids && rsvpEventUids.length > 0) {
                    gBMIcsBandal._logger.info("rsvpEventUids:" + rsvpEventUids);
                    gBMIcsBandal._logger.info("resourceId:" + resourceId);
                    let msg = {};
                    msg.bmRsvpEventUids = rsvpEventUids;
                    msg.bmResourceId = resourceId;
                    msg.bmOtherLogin = otherLogin;
                    gBMIcsBandal.onBmIcsMail(msg);
                }
            }
        }, true, {saneBodySize: true, partsOnDemand: true});
    },
    _getRsvpEventUids: function(aHeaders) {
        if (!aHeaders) return null;
        let ret = [];
        for (let i = 0; i < aHeaders.length; i++) {
            //8ce82499-8af9-497a-a74c-ce6902fab97a; recurid="2016-12-09T12:30:00.000+01:00"; rsvp="true"
            let h = aHeaders[i].split("; rsvp=");
            let uid = h[0];
            if (h.length == 2) {
                let r = h[1];
                let rsvp = r.substring(1, r.length -1);//"true"
                if (rsvp == "true" && ret.indexOf(uid) == -1) {
                    ret.push(uid);
                }
            }
        }
        return ret;
    },
    onEndAttachments: function() {},
    onBeforeShowHeaderPane: function() {},
    onBmIcsMail: function(aParam) {
        gBMIcsBandal._logger.info("onBmIcsMail");
        let msg = aParam;
        this._rsvpEventUids = msg.bmRsvpEventUids;
        if (msg.bmResourceId) {
            this._attendeeType = "Resource";
            this._ressourceId = msg.bmResourceId;
        } else {
            this._attendeeType = "Individual";
        }
        
        if (msg.bmOtherLogin != null) {
            this._otherUserLogin = msg.bmOtherLogin;
            this.getOtherUser(msg.bmOtherLogin);
        } else {
            this.getEventItemUids();
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
    getEventItemUids: function() {
        if (bmUtils.getSettings(this._login, this._pwd, this._srv, false)) {
            this._auth(function() {
                this._getEventItemUids();
            }.bind(this));
        } else {
            this._logger.debug("not enouth settings");
        }
    },
    _auth: function(aCallback) {
        let auth = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
        let self = this;
        auth.then(function(logged) {
            self._authKey = logged.authKey;
            self._user = logged.authUser;
            if (!self._userUid) {
                self._userUid = logged.authUser.uid;
            }
            aCallback();
        }).catch(function(err) {
            self._logger.error(err);
            self._showError(err);
        });
    },
    _showError: function(aErr) {
        let errorCode = (aErr instanceof BMError) ? aErr.message : "errors.UNKWNOWN_ERROR";
        let errorMessage = bmUtils.getLocalizedString(errorCode);
        let msgNotificationBar = document.getElementById("msgNotificationBar");
        msgNotificationBar.appendNotification(errorMessage,
                                errorCode,
                                "",
                                msgNotificationBar.PRIORITY_CRITICAL_LOW,
                                [],
                                null);
    },
    _getEventItemUids: function() {
        if (this._ressourceId) {
            this._containerUid = "calendar:" + this._ressourceId;
            this._attendeeDir = "bm://" + this._user.domainUid + "/resources/" + this._ressourceId;
        } else {
            this._containerUid = "calendar:Default:" + this._userUid;
            this._attendeeDir = "bm://" + this._user.domainUid + "/users/" + this._userUid;
        }
        this._events = [];
        for (let rsvpEventUid of this._rsvpEventUids) {
            //8ce82499-8af9-497a-a74c-ce6902fab97a; recurid="2016-12-09T12:30:00.000+01:00"
            let u = rsvpEventUid.split("; recurid=");
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
            this._events.push(event);
        }
        if (this._events.length > 0) {
            this._getEventFromItemUid(this._events[0]);
        }
    },
    _getEventFromItemUid: function(aEvent) {
        let cal = new CalendarClient(this._srv.value, this._authKey, this._containerUid);
        let result = cal.getByIcsUid(aEvent.eventUid);
        let self = this;
        result.then(function(items) {
            let vevent;
            for (let itemValue of items) {
                let series = itemValue.value;
                if (aEvent.recurid) {
                    for(let occ of series.occurrences) {
                        if (occ.recurid.iso8601 == aEvent.recurid) {
                            vevent = occ;
                            break;
                        }
                    }
                } else {
                    vevent = series.main;
                }
            }
            if (!vevent) {
                throw new BMError("errors.UNKWNOWN_ERROR");
            }
            let state;
            for (let attendee of vevent.attendees) {
                self._logger.debug("attendee dir:" + attendee.dir + " type:" + attendee.cutype);
                if (attendee.dir == self._attendeeDir) {
                    state = attendee.partStatus;
                }
            }
            self._logger.info("state:" + state);
            self._fillBandal(vevent);
            self.changeBandalLinksOnclick(state);
            
            let bandal = document.getElementById("bm-ics-bandal");
            bandal.collapsed = false;
        }).catch(function(err) {
            self._logger.error(err);
            self._showError(err);
        });
    },
    getOtherUser: function(aLogin) {
        this._logger.info("Get other user infos: " + aLogin);
        if (bmUtils.getSettings(this._login, this._pwd, this._srv, false)) {
            this._auth(function() {
                this._getOtherUser(aLogin);
            }.bind(this));
        } else {
            this._logger.debug("not enouth settings");
        }
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
        res.then(function(dirEntries) {
            if (dirEntries.total == 0) {
                this._logger.debug("user: " + query.emailFilter + " not found");
                return;
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
            } else {
                if (!otherCal.writable) {
                    self._logger.info("calendar is not writable: " + aLogin);
                    let partRow = document.getElementById("bm-ics-bandal-partRow");
                    partRow.setAttribute("hidden" , "true");
                }
                self._otherUserDisplayName = dirEntry.displayName;
                self._userUid = dirEntry.entryUid;
                self._getEventItemUids();
            } 
        }).catch(function(err) {
            self._logger.error(err);
            self._showError(err);
        });
    },
    _fillBandal: function(aEvent) {
        let imipBar = document.getElementById("imip-bar");
        if (imipBar) {
            this._logger.debug("hide lightning imip bar");
            imipBar.collapsed = true;
        }
        let start = new Date(aEvent.dtstart.iso8601);
        let end = new Date(aEvent.dtend.iso8601);
        let dwhen = "";
        if (aEvent.dtstart.precision == "Date") {
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
        let title = document.getElementById("bm-ics-bandal-title");
        let when = document.getElementById("bm-ics-bandal-when");
        let where = document.getElementById("bm-ics-bandal-where");
        title.setAttribute("value", aEvent.summary);
        when.setAttribute("value",  dwhen);
        where.setAttribute("value", aEvent.location != null ? aEvent.location : "");
    },
    changeParticipation: function(aState, aAttDir) {
        this._logger.info("changeParticipation(" + aState + "," + aAttDir + ")");
        this._auth(function(){
            this._setParticipation(aState, aAttDir);
        }.bind(this));
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
            self._showError(err);
        });
    }
}

gBMIcsBandal.init();
if (document.getElementById("msgHeaderView") && document.getElementById("msgHeaderView")._loaded) {
    gBMIcsBandal.onLoad();
} else {
    window.addEventListener("messagepane-loaded", gBMIcsBandal.onLoad, true);
}