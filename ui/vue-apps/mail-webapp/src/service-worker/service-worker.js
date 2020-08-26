import { precacheAndRoute } from "workbox-precaching";

import registerApiRoute from "./workbox/registerApiRoute";
import registerCSSRoute from "./workbox/registerCSSRoute";
import registerImageRoute from "./workbox/registerImageRoute";
import registerScriptRoute from "./workbox/registerScriptRoute";

import { initSync, periodicSync } from "./sync";

precacheAndRoute(self.__WB_MANIFEST);
registerApiRoute();
registerCSSRoute();
registerImageRoute();
registerScriptRoute();

initSync().then(() => periodicSync(["INBOX"]));
