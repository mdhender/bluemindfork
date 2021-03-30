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

goog.provide("net.bluemind.contact.Messages");


net.bluemind.contact.Messages.errorLoadingBooks = function(msg) {
  /** @meaning contact.addressbooks.loadingError */
  var MSG_ERROR = goog.getMsg('Error during addressbooks loading: {$message}', { message : msg });
  return MSG_ERROR;
}

net.bluemind.contact.Messages.errorLoading = function(msg) {
  /** @meaning generic.loadingError */
  var MSG_ERROR = goog.getMsg('Error during loading: {$message}', { message : msg });
  return MSG_ERROR;
}

net.bluemind.contact.Messages.errorCreate = function(msg) {
  /** @meaning generic.createError */
  var MSG_ERROR = goog.getMsg('Error during creation: {$message}', { message : msg });
  return MSG_ERROR;
}

net.bluemind.contact.Messages.createErrorMaxItemCount = function() {
  /** @meaning generic.createErrorMaxItemCount */
  var MSG_ERROR = goog.getMsg('Maximum number of contacts exceeded');
  return MSG_ERROR;
}

net.bluemind.contact.Messages.successCreate = function() {
  /** @meaning contact.create.success */
  var MSG_ERROR = goog.getMsg('Contact created');
  return MSG_ERROR;
}

net.bluemind.contact.Messages.errorUpdate = function(msg) {
  /** @meaning generic.updateError */
  var MSG_ERROR = goog.getMsg('Error during update: {$message}', { message : msg });
  return MSG_ERROR;
}

net.bluemind.contact.Messages.successUpdate = function() {
  /** @meaning contact.update.success */
  var MSG_ERROR = goog.getMsg('Contact updated');
  return MSG_ERROR;
}

net.bluemind.contact.Messages.errorDelete = function(msg) {
  /** @meaning generic.deleteError */
  var MSG_ERROR = goog.getMsg('Error during delete: {$message}', { message : msg });
  return MSG_ERROR;
}


net.bluemind.contact.Messages.successDelete = function() {
  /** @meaning contact.delete.success */
  var MSG_ERROR = goog.getMsg('Contact deleted');
  return MSG_ERROR;
}


net.bluemind.contact.Messages.errorMove = function(msg) {
  /** @meaning contact.move.error */
  var MSG_ERROR = goog.getMsg('Error during move: {$message}', { message : msg });
  return MSG_ERROR;
}


net.bluemind.contact.Messages.successMove = function() {
  /** @meaning contact.move.success */
  var MSG_ERROR = goog.getMsg('Contact moved');
  return MSG_ERROR;
}


net.bluemind.contact.Messages.errorCopy = function(msg) {
  /** @meaning contact.copy.error */
  var MSG_ERROR = goog.getMsg('Error during copy: {$message}', { message : msg });
  return MSG_ERROR;
}


net.bluemind.contact.Messages.successCopy = function() {
  /** @meaning contact.copy.success */
  var MSG_ERROR = goog.getMsg('Contact copied');
  return MSG_ERROR;
}


net.bluemind.contact.Messages.infoExternalContact = function() {
  /** @meaning contact.info.external */
  var MSG_ERROR = goog.getMsg('This contact belongs to a different addressbook. Contact data will not get refreshed automatically');
  return MSG_ERROR;
}

net.bluemind.contact.Messages.errorNotAccessible = function() {
  /** @meaning vcard.member.notAccessible */
  var MSG_ERROR = goog.getMsg('This member has been deleted. Validate and save this contact if you want to remove deleted members');
  return MSG_ERROR;
}

