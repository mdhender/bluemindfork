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
    var userDateTimeFormater = createUserDateTimeFormater(
        ctx.helper("date").create.bind(ctx.helper("date")),
        ctx.helper("timezone").getDefaultTimeZone,
        ctx.helper("dateformat").formatter.datetime.format.bind(ctx.helper("dateformat").formatter.datetime)
    );
    checkDeferredActions(deferredaction, userDateTimeFormater)();
    window.setInterval(checkDeferredActions(deferredaction, userDateTimeFormater), DELAY);
};

function createUserDateTimeFormater(BmDateTimeCreator, getTimezone, formater) {
    return function(dtstart) {
        return formater(BmDateTimeCreator(dtstart, getTimezone()));
    };
}

function checkDeferredActions(deferredaction, userDateTimeFormater) {
    return function() {
        goog.log.info(logger, "Checking reminders…");
        deferredaction
            .getItems(Date.now() + DELAY)
            .then(deleteOverdues(deferredaction))
            .then(scheduleNotifications(deferredaction, userDateTimeFormater))
            .then(createAndSaveNextDeferredAction(deferredaction));
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
    return (
        item.value["configuration"]["summary"] +
        " (" +
        item.value["configuration"]["location"] +
        ") - " +
        userDateTimeFormater(item.value["configuration"]["dtstart"])
    );
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

function createAndSaveNextDeferredAction(deferredaction) {
    return function(items) {
        return items
            .filter(isrecurrent)
            .map(createNextDeferredAction)
            .forEach(saveItem(deferredaction));
    };
}

function saveItem(deferredaction) {
    return function(item) {
        deferredaction.createItem(item);
    };
}

function createNextDeferredAction(prevItem) {
    var item = Object.assign({}, prevItem);
    item.value["executionDate"] = prevItem.value["configuration"]["nextExecutionDate"];
    return item;
}

function isrecurrent(item) {
    return item.value["configuration"]["nextExecutionDate"];
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
