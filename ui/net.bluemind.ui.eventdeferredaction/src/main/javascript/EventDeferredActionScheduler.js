/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
 *
 * @format
 */

goog.provide("net.bluemind.ui.eventdeferredaction.DeferredActionScheduler");

goog.require("net.bluemind.deferredaction.service.DeferredActionService");
goog.require("net.bluemind.rrule.OccurrencesHelper");
goog.require("goog.log");
goog.require("goog.Promise");
goog.require("net.bluemind.mvp.UID");

var DELAY = 60 * 1000;
var logger = goog.log.getLogger("net.bluemind.ui.eventdeferredaction.DeferredActionScheduler");

var notify = browserNotify;

/**
 * @constructor
 */
net.bluemind.ui.eventdeferredaction.DeferredActionScheduler = function(ctx) {
    var deferredaction = ctx.service("deferredaction");
    var dateHelperCreate = ctx.helper("date").create.bind(ctx.helper("date"));
    var userDateTimeFormater = createUserDateTimeFormater(
        ctx.helper("date").toBMDateTime.bind(ctx.helper("date")),
        dateHelperCreate,
        ctx.helper("timezone").getDefaultTimeZone,
        ctx.helper("dateformat").format.bind(ctx.helper("dateformat"))
    );
    checkDeferredActions(deferredaction, userDateTimeFormater, dateHelperCreate)();
    window.setInterval(checkDeferredActions(deferredaction, userDateTimeFormater, dateHelperCreate), DELAY);
};

function createUserDateTimeFormater(toBMDateTime, dateTimeCreator, getTimezone, formater) {
    return function(timestamp) {
        var bmDateTime = toBMDateTime(new net.bluemind.date.DateTime(timestamp));
        return formater(dateTimeCreator(bmDateTime, getTimezone()));
    };
}

function checkDeferredActions(deferredaction, userDateTimeFormater, dateHelperCreate) {
    return function() {
        deferredaction
            .getItemsByDate(Date.now() + DELAY)
            .then(deleteOverdues(deferredaction, dateHelperCreate))
            .then(scheduleNotifications(deferredaction, userDateTimeFormater, dateHelperCreate));
    };
}

function deleteOverdues(deferredaction, dateHelperCreate) {
    return function(items) {
        return items.filter(deleteOverdue(deferredaction, dateHelperCreate));
    };
}

function deleteOverdue(deferredaction, dateHelperCreate) {
    return function(item) {
        if (overdue(item, dateHelperCreate)) {
            try {
                createNext(deferredaction, dateHelperCreate, item);
            } catch(e) {
                goog.log.error("Failed to create next event reminder.", e);
            } finally {
                deferredaction.deleteItem(item);
            }
        }
        return !overdue(item, dateHelperCreate);
    };
}

function createNext(deferredaction, dateHelperCreate, item) {
    if (isrecurrent(item)) {
        var nextItem = getNextDeferredAction(prevItem, dateHelperCreate);
        deferredaction.getItem(nextItem["uid"]).then(function(item) {
            if (!item) {
                return deferredaction.createItem(nextItem);
            }
        });
    }
}

function startOfEvent(item, dateHelperCreate) {
    var trigger = parseInt(item["value"]["configuration"]["trigger"], 10) * 1000;
    return dateHelperCreate(parseInt(item["value"]["executionDate"], 10) - trigger);
}

function endOfEvent(item, dateHelperCreate) {
    var dtstart = dateHelperCreate(JSON.parse(item["value"]["configuration"]["dtstart"]));
    var dtend = dateHelperCreate(JSON.parse(item["value"]["configuration"]["dtend"]));
    var duration = dtend.getTime() - dtstart.getTime();
    return startOfEvent(item, dateHelperCreate) + duration;
}

function overdue(item, dateHelperCreate) {
    return endOfEvent(item, dateHelperCreate) < Date.now();
}

function scheduleNotifications(deferredaction, userDateTimeFormater, dateHelperCreate) {
    return function(items) {
        if (!("Notification" in window)) {
            // eslint-disable-next-line no-console
            console.log("This browser does not support notifications.");
            return items;
        }
        items.forEach(scheduleNotification(userDateTimeFormater, deferredaction, dateHelperCreate));
        return items;
    };
}

function scheduleNotification(userDateTimeFormater, deferredaction, dateHelperCreate) {
    return function(item) {
        var delay = item["value"]["executionDate"] - Date.now();
        var text = getNotificationText(userDateTimeFormater, item, dateHelperCreate);
        setTimeout(function() {
            notify(text);
            try {
                createNext(deferredaction, dateHelperCreate, item);
            } catch(e) {
                goog.log.error("Failed to create next event reminder.", e);
            } finally {
                deferredaction.deleteItem(item);
            }
        }, delay);
    };
}

function getNotificationText(userDateTimeFormater, item, dateHelperCreate) {
    var location = item["value"]["configuration"]["location"]
        ? " (" + item["value"]["configuration"]["location"] + ")"
        : "";
    var dtstart = startOfEvent(item, dateHelperCreate);
    return item["value"]["configuration"]["summary"] + location + " - " + userDateTimeFormater(dtstart.getTime());
}

function browserNotify(text) {
    goog.log.info(logger, "Notification for reminder: " + text);
    if (Notification.permission === "granted") {
        new Notification("Calendar", { body: text });
    } else if (Notification.permission !== "denied") {
        Notification.requestPermission(function(permission) {
            if (!("permission" in Notification)) {
                Notification.permission = permission;
            }
            if (Notification.permission === "granted") {
                new Notification("Calendar", { body: text });
            }
        });
    }
}

function getNextDeferredAction(prevItem, dateHelperCreate) {
    var item = { "value": Object.assign({}, prevItem["value"]) };
    item["value"]["executionDate"] = getNextExecutionTime(prevItem, dateHelperCreate);
    item["name"] = "EVENT-" + new Date(item["value"]["executionDate"]);
    item["uid"] = item["value"]["reference"] + "-main-" + item["value"]["executionDate"];
    return item;
}

function getNextExecutionTime(prevItem, dateHelperCreate) {
    var dtstart = dateHelperCreate(JSON.parse(prevItem["value"]["configuration"]["dtstart"]));
    if (!(dtstart instanceof goog.date.DateTime)) {
        dtstart = new net.bluemind.date.DateTime(dtstart);
    }

    var rruleObject = JSON.parse(prevItem["value"]["configuration"]["rrule"]);
    var exdates = JSON.parse(prevItem["value"]["configuration"]["exdates"]);
    var untilDate = dateHelperCreate(rruleObject["until"], dtstart.getTimeZone());
    var trigger = parseInt(prevItem["value"]["configuration"]["trigger"], 10) * 1000;
    var from = dateHelperCreate(parseInt(prevItem["value"]["executionDate"], 10) - trigger);
    var nextExecutionDate = new net.bluemind.rrule.OccurrencesHelper().findNextOccurrenceDate(
        dtstart,
        rruleObject,
        exdates,
        untilDate,
        from
    );
    return nextExecutionDate.getTime() + trigger;
}

function isrecurrent(item) {
    return Boolean(item["value"]["configuration"]["rrule"]);
}

/**
 * Change implementation of notif, used by Thunderbird connector
 *
 * @export
 * @param {Function} new implementation, a function with a String param
 */
net.bluemind.ui.eventdeferredaction.DeferredActionScheduler.setNotificationImpl = function(fn) {
    goog.log.info(logger, "Changing notification implementation");
    notify = fn;
};
