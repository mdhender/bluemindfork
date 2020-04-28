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
import AlertTypes from "./AlertTypes";
import UUIDGenerator from "@bluemind/uuid";

/** Generic structure for an alert.  */
export default class Alert {
    constructor({ type, uid, code, message, key, props, renderer }) {
        this.type = type || AlertTypes.ERROR;
        this.uid = uid || UUIDGenerator.generate();
        this.code = code;
        this.message = message;
        this.key = key;
        this.props = props;
        this.renderer = renderer;
    }
}
