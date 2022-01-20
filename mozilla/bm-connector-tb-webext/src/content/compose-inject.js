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

/* Main window overlay */

Services.scriptloader.loadSubScript("chrome://bm/content/compose.js", window, "UTF-8");
Services.scriptloader.loadSubScript("chrome://bm/content/fileProvider/remoteChooser.js", window, "UTF-8");

function onLoad(activatedWhileWindowOpen) {
    WL.injectElements(`
        <command id="cmd_attachBm" oncommand="attachFilesFromHosting();"/>
        <menuitem id="button-attachPopup_BlueMind"
                appendto="button-attachPopup"
                insertbefore="button-attachPopup_attachPageItem"
                label="__MSG_bm.compose.attach.link__"
                accesskey="B"
                command="cmd_attachBm"
                hidden="true"/>
        <vbox id="bmSignature"
                collapsed="true"
                flex="0"
                insertbefore="compose-notification-bottom">
            <hbox>
                <label id="bm-header-signature" class="msgNotificationBarText"
                        value="__MSG_bm.signature.disclamer__"/>
                <spacer flex="1"/>
                <label id="bm-toggle-signature" class="text-link"
                                onclick="gBMCompose.togglePreview();"
                                value="__MSG_bm.signature.hide__"/>
            </hbox>
            <browser id="bm-browser-signature" context="mailContext"
                    flex="1" name="bm-browser-signature"
                    height="200"
                    disablesecurity="true"
                    disablehistory="true"
                    type="content"
                    autofind="false"
                    onclick="return false;"
                    src="about:blank"
                    collapsed="false"
                    remote="false" />
        </vbox>
    `, [], true);

    WL.injectCSS("chrome://bm/content/skin/style.css");

    window.BmInitCompose();
}