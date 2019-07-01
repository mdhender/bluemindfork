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

goog.provide("net.bluemind.calendar.Messages");

net.bluemind.calendar.Messages.decomposeMessage = function(msg) {
  if (typeof msg === 'object') {
    return msg['message'];
  }
  return msg;
}

net.bluemind.calendar.Messages.errorLoadingCalendars = function(msg) {
  /** @meaning calendar.calendars.loadingError */
  var MSG_ERROR = goog.getMsg('Error during calendars loading: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.errorLoading = function(msg) {
  /** @meaning generic.loadingError */
  var MSG_ERROR = goog.getMsg('Error during loading: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.errorUpdateSettings = function(msg) {
  /** @meaning calendar.settings.updateError */
  var MSG_ERROR = goog.getMsg('Error during update calendar settings: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.errorCreate = function(msg) {
  /** @meaning generic.createError */
  var MSG_ERROR = goog.getMsg('Error during creation: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.successCreate = function() {
  /** @meaning calendar.create.success */
  var MSG_ERROR = goog.getMsg('Event saved');
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.errorUpdate = function(msg) {
  /** @meaning generic.updateError */
  var MSG_ERROR = goog.getMsg('Error during update: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.successUpdate = function() {
  /** @meaning calendar.update.success */
  var MSG_ERROR = goog.getMsg('Event saved');
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.errorDelete = function(msg) {
  /** @meaning generic.deleteError */
  var MSG_ERROR = goog.getMsg('Error during delete: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.successDelete = function() {
  /** @meaning calendar.delete.success */
  var MSG_ERROR = goog.getMsg('Event deleted');
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.errorMove = function(msg) {
  /** @meaning calendar.move.error */
  var MSG_ERROR = goog.getMsg('Error during move: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.successMove = function() {
  /** @meaning calendar.move.success */
  var MSG_ERROR = goog.getMsg('Event moved');
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.errorCopy = function(msg) {
  /** @meaning calendar.copy.error */
  var MSG_ERROR = goog.getMsg('Error during copy: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.successCopy = function() {
  /** @meaning calendar.copy.success */
  var MSG_ERROR = goog.getMsg('Event copied');
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.errorViewCreate = function(msg) {
  /** @meaning generic.createError */
  var MSG_ERROR = goog.getMsg('Error during creation: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.successViewCreate = function() {
  /** @meaning calendar.view.create.success */
  var MSG_ERROR = goog.getMsg('View created');
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.errorViewUpdate = function(msg) {
  /** @meaning calendar.view.udpate.error */
  var MSG_ERROR = goog.getMsg('Error during update: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.successViewUpdate = function() {
  /** @meaning calendar.view.update.success */
  var MSG_ERROR = goog.getMsg('View udpated');
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.errorViewDelete = function(msg) {
  /** @meaning generic.deleteError */
  var MSG_ERROR = goog.getMsg('Error during delete: {$message}', {
    message : net.bluemind.calendar.Messages.decomposeMessage(msg)
  });
  return MSG_ERROR;
}

net.bluemind.calendar.Messages.successViewDelete = function() {
  /** @meaning calendar.view.delete.success */
  var MSG_ERROR = goog.getMsg('View deleted');
  return MSG_ERROR;
}