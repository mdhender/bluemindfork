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
        <!--Spaces (left panel)-->
        <button id="bm-spaces-open-calendar"
            class="spaces-toolbar-button"
            title="_MSG_bm.button.open.calendar.label__"
            tooltiptext="__MSG_bm.button.open.calendar.tooltiptext__"
            insertafter="chatButton"
            oncommand="gBMOverlay.openBmApp('/cal', false);"/>
        <button id="bm-spaces-open-todolist"
            class="spaces-toolbar-button"
            title="__MSG_bm.button.open.todolist.label__"
            tooltiptext="__MSG_bm.button.open.todolist.tooltiptext__"
            insertafter="bm-spaces-open-calendar"
            oncommand="gBMOverlay.openBmApp('/task', false);"/>
        <button id="bm-spaces-sync"
            class="spaces-toolbar-button"
            title="__MSG_bm.button.sync.label__"
            tooltiptext="__MSG_bm.button.sync.tooltiptext__"
            insertafter="bm-spaces-open-todolist"
            oncommand="gBMOverlay.doSync();" />
    `, [], true);

    WL.injectCSS("chrome://bm/content/skin/style.css");
    WL.injectCSS("chrome://messenger/skin/shared/grid-layout.css");

    if (window.gMessageDisplay) {
        // TB < 115 inject bandals in main window
        WL.injectElements(`
            <!--Message view-->
            <hbox id="bm-ics-bandal"
                    collapsed="true"
                    insertbefore="messagepanewrapper"
                    class="bm-bandal">
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
                    insertbefore="messagepanewrapper"
                    class="bm-bandal">
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
    } else {
        // TB 115 inject in loaded message browser
        window.addEventListener("MsgsLoaded", msgsListener);
    }

    window.gBMOverlay.init();
    window.bmContentTabInit();
}

function onUnload(deactivatedWhileWindowOpen) {
    let win = window?.gTabmail?.tabInfo[0]?.chromeBrowser?.contentWindow?.messageBrowser?.contentWindow;
    if (win) {
      // TB 115 remove injected in message browser
      console.trace("onUnload");
      bmUnloadFromWindow(win);
    }
    window.gBMOverlay.onremove();
}

function msgsListener () {
    let win = window?.gTabmail?.tabInfo[0]?.chromeBrowser?.contentWindow?.messageBrowser?.contentWindow;
    console.trace("inject in message browser", win);
    if (!win) return;

    bmInjectCSS("chrome://bm/content/skin/style.css", win);
    bmInjectCSS("chrome://messenger/skin/shared/grid-layout.css", win);
    
    bmInjectXUL(`
        <!--Message view-->
        <hbox id="bm-ics-bandal"
                collapsed="true"
                insertafter="msgHeaderView"
                class="bm-bandal">
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
                insertafter="msgHeaderView"
                class="bm-bandal">
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
    `, [], true, win);

    win.gBMIcsBandal = window.gBMIcsBandal;
    if (window.gBMIcsBandal.onLoad()) {
        console.trace("gBMIcsBandal loaded");
        window.removeEventListener("MsgsLoaded", msgsListener);
    }
}

let injectedID = "bmBandal";

function bmInjectCSS(cssFile, win) {
  let element;
  let v = parseInt(Services.appinfo.version.split(".").shift());

  // using createElementNS in TB78 delays the insert process and hides any security violation errors
  if (v > 68) {
    element = win.document.createElement("link");
  } else {
    let ns = win.document.documentElement.lookupNamespaceURI("html");
    element = win.document.createElementNS(ns, "link");
  }

  element.setAttribute("wlapi_autoinjected", injectedID);
  element.setAttribute("rel", "stylesheet");
  element.setAttribute("href", cssFile);
  return win.document.documentElement.appendChild(element);
};

function bmInjectXUL(xulString, dtdFiles = [], debug = false, win) {

  function checkElements(stringOfIDs) {
    let arrayOfIDs = stringOfIDs.split(",").map((e) => e.trim());
    for (let id of arrayOfIDs) {
      let element = win.document.getElementById(id);
      if (element) {
        return element;
      }
    }
    return null;
  }

  function localize(entity) {
    let msg = entity.slice("__MSG_".length, -2);
    return WL.extension.localeData.localizeMessage(msg);
  }

  function injectChildren(elements, container) {
    if (debug) console.log(elements);

    for (let i = 0; i < elements.length; i++) {

      if (
        elements[i].hasAttribute("insertafter") &&
        checkElements(elements[i].getAttribute("insertafter"))
      ) {
        let insertAfterElement = checkElements(
          elements[i].getAttribute("insertafter")
        );

        if (debug)
          console.log(
            elements[i].tagName +
            "#" +
            elements[i].id +
            ": insertafter " +
            insertAfterElement.id
          );
        if (
          debug &&
          elements[i].id &&
          win.document.getElementById(elements[i].id)
        ) {
          console.error(
            "The id <" +
            elements[i].id +
            "> of the injected element already exists in the document!"
          );
        }
        elements[i].setAttribute("wlapi_autoinjected", injectedID);
        insertAfterElement.parentNode.insertBefore(
          elements[i],
          insertAfterElement.nextSibling
        );
      } else if (
        elements[i].hasAttribute("insertbefore") &&
        checkElements(elements[i].getAttribute("insertbefore"))
      ) {
        let insertBeforeElement = checkElements(
          elements[i].getAttribute("insertbefore")
        );

        if (debug)
          console.log(
            elements[i].tagName +
            "#" +
            elements[i].id +
            ": insertbefore " +
            insertBeforeElement.id
          );
        if (
          debug &&
          elements[i].id &&
          win.document.getElementById(elements[i].id)
        ) {
          console.error(
            "The id <" +
            elements[i].id +
            "> of the injected element already exists in the document!"
          );
        }
        elements[i].setAttribute("wlapi_autoinjected", injectedID);
        insertBeforeElement.parentNode.insertBefore(
          elements[i],
          insertBeforeElement
        );
      } else if (
        elements[i].id &&
        win.document.getElementById(elements[i].id)
      ) {
        // existing container match, dive into recursivly
        if (debug)
          console.log(
            elements[i].tagName +
            "#" +
            elements[i].id +
            " is an existing container, injecting into " +
            elements[i].id
          );
        injectChildren(
          Array.from(elements[i].children),
          win.document.getElementById(elements[i].id)
        );
      } else {
        // append element to the current container
        if (debug)
          console.log(
            elements[i].tagName +
            "#" +
            elements[i].id +
            ": append to " +
            container.id
          );
        elements[i].setAttribute("wlapi_autoinjected", injectedID);
        container.appendChild(elements[i]);
      }
    }
  }

  if (debug) console.log("Injecting into root document:");
  let localizedXulString = xulString.replace(
    /__MSG_(.*?)__/g,
    localize
  );
  injectChildren(
    Array.from(
      win.MozXULElement.parseXULToFragment(
        localizedXulString,
        dtdFiles
      ).children
    ),
    win.document.documentElement
  );

};

function bmUnloadFromWindow(window) {
  // Unload any contained browser elements.
  let elements = [];
  elements = elements.concat(...window.document.getElementsByTagName("browser"));
  elements = elements.concat(...window.document.getElementsByTagName("xul:browser"));
  for (let element of elements) {
    if (element.contentWindow) {
      bmUnloadFromWindow(element.contentWindow);
    }
  }

  // Remove all auto injected objects
  elements = Array.from(
    window.document.querySelectorAll(
      '[wlapi_autoinjected="' + injectedID + '"]'
    )
  );
  for (let element of elements) {
    element.remove();
  }

  // Remove add-on scope, if it exists
  if (window.hasOwnProperty(injectedID)) {
    delete window[injectedID];
  }
}