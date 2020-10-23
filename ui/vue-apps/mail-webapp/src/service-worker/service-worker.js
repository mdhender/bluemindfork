import { skipWaiting } from "workbox-core";

import registerApiRoute from "./workbox/registerApiRoute";
import registerCSSRoute from "./workbox/registerCSSRoute";
import registerImageRoute from "./workbox/registerImageRoute";
import registerScriptRoute from "./workbox/registerScriptRoute";

import { registerPeriodicSync } from "./periodicSync";

skipWaiting();

registerApiRoute();
registerCSSRoute();
registerImageRoute();
registerScriptRoute();

self.addEventListener("message", event => {
    if (event.data.type === "INIT_PERIODIC_SYNC") {
        registerPeriodicSync();
    }
});
