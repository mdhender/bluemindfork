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

Services.scriptloader.loadSubScript("chrome://bm/content/abCard.js", window, "UTF-8");

function onLoad(activatedWhileWindowOpen) {
    WL.injectElements(`
        <tab id="debugTabButton" label="Debug" accesskey="D" insertafter="photoTabButton"/>
        
        <vbox id="abBmDebugTab" insertafter="abPhotoTab">
            <button id="bmShowDebug" label="Show all props" oncommand="ShowProps();"/>
            <html:textarea id="bmProps" readonly="true" rows="12"/>
        </vbox>
        
        <vbox id="editcard">
            <hbox id="bmAlertLocalCard" hidden="true">
                <label id="bmAlert" class="red" value="__MSG_bm.card.alert.local__" />
                <label id="bmOfList" value="" />
            </hbox>
            <hbox id="bmInError" hidden="true">
                <label id="bmError" class="red" value="" />
            </hbox>
        </vbox>
    `, [], true);

    WL.injectCSS("chrome://bm/content/skin/style.css");
}
