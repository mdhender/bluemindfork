/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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

Services.scriptloader.loadSubScript("chrome://bm/content/abList.js", window, "UTF-8");
Services.scriptloader.loadSubScript("chrome://bm/content/abCommon.js", window, "UTF-8");

function onLoad(isAddonActivation) {
    WL.injectElements(`
        <hbox id="bmInError" insertafter="ListDescriptionContainer" hidden="true">
            <label id="bmError" class="red" value="" />
        </hbox>
    `, [], true);

    WL.injectCSS("chrome://bm/content/skin/style.css");
}
