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

specialTabs.bmTabType = {
    __proto__: contentTabBaseType,
    name: "bmTab",
    perTabPanel: "vbox",
    lastBrowserId: 0,
    get loadingTabString() {
      delete this.loadingTabString;
      return (this.loadingTabString = document
        .getElementById("bundle_messenger")
        .getString("loadingTab"));
    },

    modes: {
      bmTab: {
        type: "bmTab",
        maxTabs: 10,
      },
    },

    /**
     * This is the internal function used by content tabs to open a new tab. To
     * open a contentTab, use specialTabs.openTab("contentTab", aArgs)
     *
     * @param aArgs The options that content tabs accept.
     * @param aArgs.contentPage A string that holds the URL that is to be opened
     * @param aArgs.openWindowInfo The opener window
     * @param aArgs.clickHandler The click handler for that content tab. See the
     *  "Content Tabs" article on MDC.
     * @param aArgs.onLoad A function that takes an Event and a DOMNode. It is
     *  called when the content page is done loading. The first argument is the
     *  load event, and the second argument is the xul:browser that holds the
     *  contentPage. You can access the inner tab's window object by accessing
     *  the second parameter's contentWindow property.
     */
    openTab(aTab, aArgs) {
      console.trace("OPEN TAB:", aTab, aArgs);
      specialTabs.contentTabType.openTab(aTab, aArgs);
      aTab.tabNode.setAttribute("bmApp", aArgs.bmApp);
    },
    tryCloseTab(aTab) {
      let docShell = aTab.browser.docShell;
      // If we have a docshell, a contentViewer, and it forbids us from closing
      // the tab, then we return false, which means, we can't close the tab. All
      // other cases return true.
      return !(
        docShell &&
        docShell.contentViewer &&
        !docShell.contentViewer.permitUnload()
      );
    },
    persistTab(aTab) {
      if (aTab.browser.currentURI.spec == "about:blank") {
        return null;
      }

      let onClick = aTab.clickHandler;

      return {
        tabURI: aTab.browser.currentURI.spec,
        clickHandler: onClick ? onClick : null,
        bmApp: aTab.tabNode.getAttribute("bmApp")
      };
    },
    restoreTab(aTabmail, aPersistedState) {
        delayOpener.open({
            bmApp: aPersistedState.bmApp,
            openInBackGround: true
        });
    }
};

function bmTabsDelayOpener() {
    this._apps = [];
    this._timer = null;
}

bmTabsDelayOpener.prototype = {
    open: function(aApp) {
        this._apps.push(aApp);
        if (!this._timer) {
            this._timer = Components.classes["@mozilla.org/timer;1"].createInstance(Components.interfaces.nsITimer);
            this._timer.init(this, 1000, Components.interfaces.nsITimer.TYPE_ONE_SHOT);
        }
    },
    observe: function(aSubject, aTopic) {
        if (aTopic == 'timer-callback') {
            gBMOverlay.openBmApps(this._apps);
            this._apps = [];
            this._timer = null;
        }
    }
};

var delayOpener = new bmTabsDelayOpener();

function bmContentTabInit() {
    console.log("bmContentTab loaded");
    let tabmail = document.getElementById("tabmail");
    tabmail.registerTabType(specialTabs.bmTabType);
}
