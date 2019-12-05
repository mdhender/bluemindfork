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

goog.provide("net.bluemind.deferredaction.reminder.DeferredActionScheduler");

goog.require("net.bluemind.deferredaction.service.DeferredActionService");
goog.require("net.bluemind.rrule.OccurrencesHelper");
goog.require("goog.log");
goog.require("goog.Promise");

var DELAY = 10 * 60 * 1000;
var logger = goog.log.getLogger("net.bluemind.deferredaction.reminder.DeferredActionScheduler");

var notify = browserNotify;

/**
 * @constructor
 */
net.bluemind.deferredaction.reminder.DeferredActionScheduler = function(ctx) {
    var deferredaction = ctx.service("deferredaction");
    var calendar = ctx.service("calendar");
    var userDateTimeFormater = createUserDateTimeFormater(
        ctx.helper("date").create.bind(ctx.helper("date")),
        ctx.helper("timezone").getDefaultTimeZone,
        ctx.helper("dateformat").formatter.datetime.format.bind(ctx.helper("dateformat").formatter.datetime)
    );
    checkDeferredActions(ctx, deferredaction, calendar, userDateTimeFormater)();
    window.setInterval(checkDeferredActions(ctx, deferredaction, calendar, userDateTimeFormater), DELAY);
};

function createUserDateTimeFormater(BmDateTimeCreator, getTimezone, formater) {
    return function(dtstart) {
        return formater(BmDateTimeCreator(dtstart, getTimezone()));
    };
}

function checkDeferredActions(ctx, deferredaction, calendar, userDateTimeFormater) {
    return function() {
        goog.log.info(logger, "Checking reminders…");
        deferredaction
            .getItems(Date.now() + DELAY)
            .then(deleteOverdues(deferredaction))
            .then(appendEvents(calendar))
            .then(scheduleNotifications(deferredaction, userDateTimeFormater))
            .then(calculateNextDeferredAction(ctx, deferredaction));
    };
}

function deleteOverdues(deferredaction) {
    return function(items) {
        goog.log.info(logger, "Deleting overdue reminders…");
        return items.filter(deleteOverdue(deferredaction));
    };
}

function deleteOverdue(deferredaction) {
    return function(item) {
        if (overdue(item)) {
            deferredaction.deleteItem(item);
        }
        return !overdue(item);
    };
}

function overdue(item) {
    return item.value["executionDate"] < Date.now();
}

function appendEvents(calendar) {
    return function(items) {
        goog.log.info(logger, "Getting related events…");
        return goog.Promise.all(items.map(promiseWithEvents(calendar)));
    };
}

function promiseWithEvents(calendar) {
    return function(item) {
        var uids = getUids(item.value["reference"]);
        var containerUid = uids.containerUid;
        var itemUid = uids.itemUid;
        return getEvent(calendar, containerUid, itemUid).then(appendEvent(item));
    };
}

function getUids(reference) {
    var uids = reference.split("#");
    return {
        containerUid: uids[0],
        itemUid: uids[1]
    };
}

function getEvent(calendar, containerUid, itemUid) {
    return calendar.getItem(containerUid, itemUid);
}

function appendEvent(item) {
    return function(event) {
        item.event = event;
        return item;
    };
}

function scheduleNotifications(deferredaction, userDateTimeFormater) {
    return function(items) {
        goog.log.info(logger, "Scheduling reminders notifications…");
        if (!("Notification" in window)) {
            // eslint-disable-next-line no-console
            console.log("This browser does not support notifications.");
            return items;
        }
        items.forEach(scheduleNotification(userDateTimeFormater, deferredaction));
        return items;
    };
}

function scheduleNotification(userDateTimeFormater, deferredaction) {
    return function(item) {
        var delay = item.value["executionDate"] - Date.now();
        var text = getNotificationText(userDateTimeFormater, item);
        setTimeout(function() {
            notify(text);
            deferredaction.deleteItem(item);
        }, delay);
    };
}

function getNotificationText(userDateTimeFormater, item) {
    var datetimestart = userDateTimeFormater(item.event.value["main"]["dtstart"]);
    var datetimeend = userDateTimeFormater(item.event.value["main"]["dtend"]);
    return item.event.name + "\n" + datetimestart + " → " + datetimeend;
}

function browserNotify(text) {
    goog.log.info(logger, "Notification for reminder: " + text);
    if (Notification.permission === "granted") {
        new Notification("Calendar", { body: text });
    }
    if (Notification.permission !== "denied") {
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

/**
 * Change implementation of notif, used by Thunderbird connector
 *
 * @export
 * @param {Function} new implementation, a function with a String param
 */
net.bluemind.deferredaction.reminder.DeferredActionScheduler.setNotificationImpl = function(fn) {
    goog.log.info(logger, "Changing notification implementation");
    notify = fn;
};

function calculateNextDeferredAction(ctx, deferredaction) {
    return function(items) {
        return items
            .filter(isrecurrent)
            .map(getNextDeferredAction(ctx))
            .forEach(createDeferredAction(deferredaction));
    };
}

function createDeferredAction(deferredaction) {
    return function(item) {
        deferredaction.createItem(item);
    };
}

function getNextDeferredAction(ctx) {
    return function(prevItem) {
        var occurrenceHelper = new net.bluemind.rrule.OccurrencesHelper();
        var nextOccurrenceDate = occurrenceHelper.getNextOccurrence(
            ctx,
            prevItem.event,
            prevItem.value["executionDate"]
        );
        var item = Object.assign({}, prevItem);
        item.value["executionDate"] =
            nextOccurrenceDate.getTime() + parseInt(prevItem.value["configuration"]["trigger"], 10) * 1000;
        return item;
    };
}

function isrecurrent(item) {
    return item.event.value["main"]["rrule"];
}
