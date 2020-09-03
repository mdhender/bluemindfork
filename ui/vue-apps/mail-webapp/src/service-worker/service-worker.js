import { precacheAndRoute } from "workbox-precaching";

import registerApiRoute from "./workbox/registerApiRoute";
import registerCSSRoute from "./workbox/registerCSSRoute";
import registerImageRoute from "./workbox/registerImageRoute";
import registerScriptRoute from "./workbox/registerScriptRoute";

import { registerPeriodicSync } from "./periodicSync";

precacheAndRoute(self.__WB_MANIFEST);

registerApiRoute();
registerCSSRoute();
registerImageRoute();
registerScriptRoute();

registerPeriodicSync();
