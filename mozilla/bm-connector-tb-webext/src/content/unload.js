/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

var { bmService } = ChromeUtils.import("chrome://bm/content/modules/bmService.jsm");

console.log("Unload");

bmService.reset();
bmService.monitor.stopListening();
bmService.onShutdown();

let rootURI = "chrome://bm/";
for (let module of Components.utils.loadedModules) {
    if (module.startsWith(rootURI)) {
        console.log("unload:" + module);
        Components.utils.unload(module);
    }
}
