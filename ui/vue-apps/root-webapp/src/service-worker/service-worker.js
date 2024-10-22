import { clientsClaim } from "workbox-core";
import { registerRoute } from "workbox-routing";
import { extensions } from "@bluemind/extensions";
import logger from "@bluemind/logger";
import session from "@bluemind/session";

import BrowserData from "./BrowserData";
import DefaultRoutes from "./DefaultRoutes";
import { ApiRouteRegistry } from "./ApiProxyPlugin/ApiRouteRegistry";

clientsClaim();
self.skipWaiting();

self.importScripts("/webapp/service-worker-extensions");

const scripts = extensions.get("serviceworker.scripts", "script");
self.importScripts(...scripts.map(({ path }) => "/webapp/" + path));

//Api Client proxyfication
const handlers = extensions.get("serviceworker.handlers", "api-handler");
handlers.forEach(handler => ApiRouteRegistry.register(handler.class, handler.priority, handler.role));
ApiRouteRegistry.routes().forEach(route => registerRoute(route));

//Synchronisation mechanism should be handle here :
// SyncRegistry.register(...);
// SyncRegistry.syncServices.foreach(service => service.start());
// registerEventListener("SYNCHRONIZE", SyncRegistry.synchronize(...));

//And even Notification system to prevent multiple notifications for one event....

registerRoute(DefaultRoutes.STYLES);
registerRoute(DefaultRoutes.IMAGES);
registerRoute(DefaultRoutes.SCRIPTS);
registerRoute(DefaultRoutes.BLANK);

self.addEventListener("message", async ({ data }) => {
    switch (data.type) {
        case "RESET":
            await BrowserData.reset();
            break;
    }
});

session.addEventListener("refresh", async () => {
    try {
        await BrowserData.resetIfNeeded();
    } catch (e) {
        logger.error("[SW][BrowserData] Fail to reset browser data");
        logger.error(e);
    }
});
