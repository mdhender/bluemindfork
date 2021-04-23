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

Services.scriptloader.loadSubScript("chrome://bm/content/messenger.js", window, "UTF-8");
Services.scriptloader.loadSubScript("chrome://bm/content/bmIcsBandal.js", window, "UTF-8");
Services.scriptloader.loadSubScript("chrome://bm/content/bmContentTab.js", window, "UTF-8");

function onLoad(activatedWhileWindowOpen) {
    WL.injectElements(`
        <!--Status bar-->
        <hbox id="status-bar">
            <hbox id="bm-status" class="statusbarpanel">
                <label id="bm-sync-status"
                    class="statusbarpanel"
                    value="" />
            </hbox>
        </hbox>
        <!--Mail toolbox-->
        <toolbarbutton id="bm-button-open-calendar"
                        class="toolbarbutton-1"
                        label="__MSG_bm.button.open.calendar.label__"
                        tooltiptext="__MSG_bm.button.open.calendar.tooltiptext__"
                        oncommand="gBMOverlay.openBmApp('/cal', false);"
                        insertbefore="gloda-search"
                        appendto="mail-bar3"/>
        <toolbarbutton id="bm-button-open-todolist"
                        class="toolbarbutton-1"
                        label="__MSG_bm.button.open.todolist.label__"
                        tooltiptext="__MSG_bm.button.open.todolist.tooltiptext__"
                        oncommand="gBMOverlay.openBmApp('/task', false);"
                        insertbefore="gloda-search"
                        appendto="mail-bar3"/>
        <toolbarbutton id="bm-button-sync"
                        class="toolbarbutton-1"
                        label="__MSG_bm.button.sync.label__"
                        tooltiptext="__MSG_bm.button.sync.tooltiptext__"
                        oncommand="gBMOverlay.doSync();"
                        insertbefore="gloda-search"
                        appendto="mail-bar3"/>
        <!--Tasks menu-->
        <menupopup id="taskPopup">
            <menu id="menu-task-bm" label="BlueMind"
                    accesskey="B"
                    class="menu-iconic">
                <menupopup>
                <menuitem id="bm-button-sync-menu"
                            label="__MSG_bm.button.sync.label__"
                            accesskey="S"
                            oncommand="gBMOverlay.doSync();"/>
                <menuitem id="bm-button-open-calendar-menu"
                            label="__MSG_bm.button.open.calendar.label__"
                            accesskey="C"
                            oncommand="gBMOverlay.openBmApp('/cal', false);"/>
                <menuitem id="bm-button-open-todolist-menu"
                            label="__MSG_bm.button.open.todolist.label__"
                            accesskey="T"
                            oncommand="gBMOverlay.openBmApp('/task', false);"/>
                <menuseparator/>
                <menuitem id="bm-button-open-settings-menu"
                            label="__MSG_bm.button.open.settings.label__"
                            accesskey="B"
                            oncommand="gBMOverlay.openBmApp('/settings', false);"/>
                <menuitem id="bm-button-open-preferences-menu"
                            label="__MSG_bm.button.open.preferences.label__"
                            accesskey="c"
                            oncommand="gBMOverlay.openPreferences();"/>
                </menupopup>
            </menu>
        </menupopup>
        <!--Main app menu-->
        <toolbarseparator id="menup-app-seperator-bm"
                            appendto="appMenu-mainViewItems"
                            insertbefore="appmenu-quit" />
        <toolbarbutton id="menu-app-bm"
                        class="subviewbutton subviewbutton-iconic subviewbutton-nav"
                        label="BlueMind"
                        closemenu="none"
                        oncommand="PanelUI.showSubView('menu-app-bm-View', this)"
                        insertbefore="menup-app-seperator-bm" />
        <panelview id="menu-app-bm-View"
                    title="BlueMind"
                    class="PanelUI-subView">
            <vbox class="panel-subview-body">
                <toolbarbutton id="bm-button-open-calendar"
                            class="subviewbutton subviewbutton-iconic"
                            label="__MSG_bm.button.open.calendar.label__"
                            accesskey="C"
                            tooltiptext="__MSG_bm.button.open.calendar.tooltiptext__"
                            oncommand="gBMOverlay.openBmApp('/cal', false);" />
                <toolbarbutton id="bm-button-open-todolist"
                            class="subviewbutton subviewbutton-iconic"
                            label="__MSG_bm.button.open.todolist.label__"
                            accesskey="T"
                            tooltiptext="__MSG_bm.button.open.todolist.tooltiptext__"
                            oncommand="gBMOverlay.openBmApp('/task', false);" />
                <toolbarbutton id="bm-button-sync"
                            class="subviewbutton subviewbutton-iconic"
                            label="__MSG_bm.button.sync.label__"
                            accesskey="S"
                            tooltiptext="__MSG_bm.button.sync.tooltiptext__"
                            oncommand="gBMOverlay.doSync();" />
                <toolbarseparator />
                <toolbarbutton id="bm-button-open-settings-menu"
                            label="__MSG_bm.button.open.settings.label__"
                            class="subviewbutton subviewbutton-iconic"
                            accesskey="B"
                            oncommand="gBMOverlay.openBmApp('/settings', false);"/>
                <toolbarbutton id="bm-button-open-preferences-menu"
                            class="subviewbutton subviewbutton-iconic"
                            label="__MSG_bm.button.open.preferences.label__"
                            accesskey="c"
                            oncommand="gBMOverlay.openPreferences();"/>
            </vbox>
        </panelview>
        <!--Message view-->
        <hbox id="bm-ics-bandal"
                collapsed="true"
                insertbefore="messagepanewrapper">
            <description class="msgNotificationBarText" value="__MSG_bm.icsbandal.description__"/>
            <div class="grid-two-column">
                <div>
                    <label value="__MSG_bm.icsbandal.title__"/>
                </div>
                <div>
                    <label id="bm-ics-bandal-title" value=""/>
                </div>
                <div>
                    <label value="__MSG_bm.icsbandal.when__"/>
                </div>
                <div>
                    <label id="bm-ics-bandal-when" value=""/>
                </div>
                <div>
                    <label value="__MSG_bm.icsbandal.where__"/>
                </div>
                <div>
                    <label id="bm-ics-bandal-where" value=""/>
                </div>
                <div id="bm-ics-bandal-partRow">
                    <label id="bm-ics-bandal-participation" value=""/>
                </div>
                <div>
                    <hbox>
                        <label id="bm-ics-bandal-accept" class="text-link"
                                onclick="" value="__MSG_bm.icsbandal.accept__"/>
                        <label value="-"/>
                        <label id="bm-ics-bandal-tentative" class="text-link"
                                onclick="" value="__MSG_bm.icsbandal.tentative__"/>
                        <label value="-"/>
                        <label id="bm-ics-bandal-decline" class="text-link"
                                onclick="" value="__MSG_bm.icsbandal.decline__"/>
                    </hbox>
                </div>
            </div>
        </hbox>
        <hbox id="bm-counter-bandal"
                collapsed="true"
                insertbefore="messagepanewrapper">
            <description class="msgNotificationBarText" value="__MSG_bm.counter.description__"/>
            <div class="grid-two-column">
                <div>
                    <label value="__MSG_bm.counter.title__"/>
                </div>
                <div>
                    <label id="bm-counter-bandal-title" value=""/>
                </div>
                <div>
                    <label value="__MSG_bm.counter.original__"/>
                </div>
                <div>
                    <label id="bm-counter-bandal-original" value=""/>
                </div>
                <div>
                    <label value="__MSG_bm.counter.proposed__"/>
                </div>
                <div>
                    <label id="bm-counter-bandal-proposed" value=""/>
                </div>
                <div>
                    <label value="__MSG_bm.counter.where__"/>
                </div>
                <div>
                    <label id="bm-counter-bandal-where" value=""/>
                </div>
                <div id="bm-counter-bandal-decisionRow">
                    <label id="bm-counter-bandal-decision" value=""/>
                </div>
                <div>
                    <hbox>
                        <label id="bm-counter-bandal-accept" class="text-link"
                                onclick="" value="__MSG_bm.counter.accept__"/>
                        <label value="-"/>
                        <label id="bm-counter-bandal-decline" class="text-link"
                                onclick="" value="__MSG_bm.counter.decline__"/>
                    </hbox>
                </div>
            </div>
        </hbox>
    `, [], true);

    WL.injectCSS("chrome://bm/content/skin/style.css");
    WL.injectCSS("chrome://messenger/skin/shared/grid-layout.css");

    window.gBMOverlay.init();
    window.bmContentTabInit();
}

function onUnload(deactivatedWhileWindowOpen) {
    window.gBMOverlay.onremove();
}