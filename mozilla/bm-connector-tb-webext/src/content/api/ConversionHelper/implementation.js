/* eslint-disable object-shorthand */

// Get various parts of the WebExtension framework that we need.
var { ExtensionCommon } = ChromeUtils.import("resource://gre/modules/ExtensionCommon.jsm");
var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");

var ConversionHelper = class extends ExtensionCommon.ExtensionAPI {
  getAPI(context) {
    // To be notified of the extension going away, call callOnClose with any object that has a
    // close function, such as this one.
    context.callOnClose(this);

    this.pathToConversionJSM = null;
    this.pathToOverlayJSM = null;
    this.pathToUnloadScript = null;
    this.chromeHandle = null;
    this.OM = null;

    const aomStartup = Cc["@mozilla.org/addons/addon-manager-startup;1"].getService(Ci.amIAddonManagerStartup);

    let that = this;
    
    return {
      ConversionHelper: {

        registerChromeUrl: async function(chromeData) {
          const manifestURI = Services.io.newURI(
            "manifest.json",
            null,
            context.extension.rootURI
          );
          that.chromeHandle = aomStartup.registerChrome(manifestURI, chromeData);          
        },

        registerApiFolder: async function(aPath) {
          // get the final path to ConversionHelper.JSM
          that.pathToConversionJSM = aPath.startsWith("chrome://") 
            ? aPath + "ConversionHelper.jsm"
            : context.extension.rootURI.resolve(aPath + "ConversionHelper.jsm");
          // try to load the JSM and set the extension context
          try {
            let JSM = ChromeUtils.import(that.pathToConversionJSM);
            JSM.ConversionHelper.context = context;
          } catch (e) {
            console.log("Failed to load <" + that.pathToConversionJSM + ">");
            Components.utils.reportError(e);
          }
        }
        
      }
    };
  }
  
  close() {
    console.log("ConversionHelper for " + this.extension.id + " closed!");
  }

  onShutdown(isAppShutdown) {
    if (isAppShutdown) return;

    // Unload the JSM we imported above. This will cause Thunderbird to forget about the JSM, and
    // load it afresh next time `import` is called. (If you don't call `unload`, Thunderbird will
    // remember this version of the module and continue to use it, even if your extension receives
    // an update.) You should *always* unload JSMs provided by your extension.
    Cu.unload(this.pathToConversionJSM);
    
  }
};
