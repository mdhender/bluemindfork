/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

/** Draft statuses. */
export default Object.freeze({
    NEW: Symbol("new"),
    SAVING: Symbol("saving"),
    SAVED: Symbol("saved"),
    SAVE_ERROR: Symbol("saveError"),
    SENDING: Symbol("sending"),
    SENT: Symbol("sent"),
    SEND_ERROR: Symbol("sendError"),
    DELETING: Symbol("deleting"),
    DELETED: Symbol("deleted"),
    DELETE_ERROR: Symbol("deleteError")
});
