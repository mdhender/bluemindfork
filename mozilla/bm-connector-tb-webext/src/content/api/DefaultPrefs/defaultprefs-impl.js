/* eslint-disable object-shorthand */

var { ExtensionCommon } = ChromeUtils.import("resource://gre/modules/ExtensionCommon.jsm");
var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");

var DefaultPrefsApi = class extends ExtensionCommon.ExtensionAPI {
  getAPI(context) {
    return {
      DefaultPrefsApi: {
        setExtensionDefaultPrefs: async function () {

          function setDefaultPref(name, value) {
            let branch = Services.prefs.getDefaultBranch("");
            if (typeof value == "boolean") {
              branch.setBoolPref(name, value);
            } else if (typeof value == "string") {
              branch.setStringPref(name, value);
            } else if (typeof value == "number") {
              branch.setIntPref(name, value);
            }
          }

          // Default preferences
          setDefaultPref("extensions.bm.server", "https://xxxx");
          /* Log level
           BMLogger.TRACE = -1;
           BMLogger.DEBUG = 0;
           BMLogger.INFO = 1;
           BMLogger.ERROR = 2;
          */
          setDefaultPref("extensions.bm.log.level", 1);
          setDefaultPref("extensions.bm.log.debug", false);
          /* Auto sync every x min */
          setDefaultPref("extensions.bm.refresh.delay", 5);
          /* Open BlueMind in tab ?*/
          setDefaultPref("extensions.bm.openInTab", true);
          /* Workaround DHE keys less than 1023-bit are no longer accepted
           * until server JDK updated to JAVA 8 and Tigase to 7.0
           */
          setDefaultPref("security.ssl3.dhe_rsa_aes_128_sha", false);
          setDefaultPref("security.ssl3.dhe_rsa_aes_256_sha", false);
          setDefaultPref("general.useragent.compatMode.firefox", true);
          /* Prevent Collected contacts ab to become hidden */
          setDefaultPref("ldap_2.servers.history.position", 2);
          /* Remote file chooser can close his window */
          setDefaultPref("dom.allow_scripts_to_close_windows", true);
          /* Debug/Dev Prefs */
          setDefaultPref("extensions.bm.dev.resetOnStart", false);

        }
      }
    }
  }
};
