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

/**
 * @fileoverview Provides schema for container item persistence
 */
goog.provide("net.bluemind.deferredaction.persistence.schema");

net.bluemind.deferredaction.persistence.schema = {
    resetTags: ['4.1.47208'],
    stores: [
        {
            name: "item",
            keyPath: "id",
            type: "TEXT",
            indexes: [
                {
                    name: "container",
                    keyPath: "container"
                },
                {
                    name: "container, uid",
                    keyPath: ["container", "uid"]
                },
                {
                    name: "container, order, uid",
                    keyPath: ["container", "order", "uid"]
                },
                {
                    name: "value.executionDate",
                    keyPath: "value.executionDate"
                },
                {
                    name: "value.reference",
                    keyPath: "value.reference"
                }
            ]
        },
        {
            name: "changes",
            keyPath: "uid",
            type: "TEXT",
            indexes: [
                {
                    name: "container",
                    keyPath: "container",
                    type: "TEXT"
                }
            ]
        },
        {
            name: "container",
            keyPath: "uid",
            type: "TEXT"
        },
        {
            name: "last_sync",
            keyPath: "container",
            type: "TEXT"
        },
        {
            name: "configuration",
            keyPath: "property"
        },
        {
            name: "csettings",
            keyPath: "uid",
            type: "TEXT"
        }
    ]
};
