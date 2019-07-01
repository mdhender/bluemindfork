/* BEGIN LICENSE
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

goog.provide("net.bluemind.task.Messages");

net.bluemind.task.Messages.errorLoadingLists = function(msg) {
  /** @meaning task.todolists.loadingError */
  var MSG_ERROR = goog.getMsg('Error during Todo lists loading: {$message}', {
    message : msg
  });
  return MSG_ERROR;
}

net.bluemind.task.Messages.errorLoading = function(msg) {
  /** @meaning generic.loadingError */
  var MSG_ERROR = goog.getMsg('Error during loading: {$message}', {
    message : msg
  });
  return MSG_ERROR;
}

net.bluemind.task.Messages.errorCreate = function(msg) {
  /** @meaning generic.createError */
  var MSG_ERROR = goog.getMsg('Error during creation: {$message}', {
    message : msg
  });
  return MSG_ERROR;
}

net.bluemind.task.Messages.successCreate = function() {
  /** @meaning task.create.success */
  var MSG_ERROR = goog.getMsg('Task created');
  return MSG_ERROR;
}

net.bluemind.task.Messages.errorUpdate = function(msg) {
  /** @meaning generic.updateError */
  var MSG_ERROR = goog.getMsg('Error during update: {$message}', {
    message : msg
  });
  return MSG_ERROR;
}

net.bluemind.task.Messages.successUpdate = function() {
  /** @meaning task.update.success */
  var MSG_ERROR = goog.getMsg('Task updated');
  return MSG_ERROR;
}

net.bluemind.task.Messages.errorDelete = function(msg) {
  /** @meaning generic.deleteError */
  var MSG_ERROR = goog.getMsg('Error during delete: {$message}', {
    message : msg
  });
  return MSG_ERROR;
}

net.bluemind.task.Messages.successDelete = function() {
  /** @meaning task.delete.success */
  var MSG_ERROR = goog.getMsg('Task deleted');
  return MSG_ERROR;
}

net.bluemind.task.Messages.errorMove = function(msg) {
  /** @meaning task.move.error */
  var MSG_ERROR = goog.getMsg('Error during move: {$message}', {
    message : msg
  });
  return MSG_ERROR;
}

net.bluemind.task.Messages.successMove = function() {
  /** @meaning task.move.success */
  var MSG_ERROR = goog.getMsg('Task moved');
  return MSG_ERROR;
}

net.bluemind.task.Messages.errorCopy = function(msg) {
  /** @meaning task.copy.error */
  var MSG_ERROR = goog.getMsg('Error during copy: {$message}', {
    message : msg
  });
  return MSG_ERROR;
}

net.bluemind.task.Messages.successCopy = function() {
  /** @meaning task.move.success */
  var MSG_ERROR = goog.getMsg('Task copied');
  return MSG_ERROR;
}
