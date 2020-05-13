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

/** @fileoverview Vevent event type */

goog.provide('net.bluemind.calendar.vevent.EventType');

/**
 * @enum {string}
 */
net.bluemind.calendar.vevent.EventType = {
  REFRESH : goog.events.getUniqueId('refresh'),
  CHANGE : goog.events.getUniqueId('change'),
  SAVE : goog.events.getUniqueId('save'),
  SEND : goog.events.getUniqueId('send'),
  BACK : goog.events.getUniqueId('back'),
  CANCEL : goog.events.getUniqueId('cancel'),
  REMOVE : goog.events.getUniqueId('remove'),
  DETAILS : goog.events.getUniqueId('details'),
  DUPLICATE : goog.events.getUniqueId('duplicate'),
  PART : goog.events.getUniqueId('participation')

};