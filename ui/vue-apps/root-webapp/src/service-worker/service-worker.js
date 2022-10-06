import { clientsClaim } from "workbox-core";
import { registerRoute } from "workbox-routing";
import { extensions } from "@bluemind/extensions";
import DefaultRoutes from "./DefaultRoutes";
import { ApiRouteRegistry } from "./ClientProxy/ApiRouteRegistry";

clientsClaim();
self.skipWaiting();

self.importScripts("/webapp/service-worker-extensions");

const scripts = extensions.get("serviceworker.scripts", "script");
self.importScripts(scripts.map(({ path }) => path).join(","));

//Api Client proxyfication
const handlers = extensions.get("serviceworker.handlers", "api-handler");
handlers.forEach(handler => ApiRouteRegistry.register(handler.class, handler.priority));
ApiRouteRegistry.routes().forEach(route => registerRoute(route));

//Synchronisation mechanism should be handle here :
// SyncRegistry.register(...);
// SyncRegistry.syncServices.foreach(service => service.start());
// registerEventListener("SYNCHRONIZE", SyncRegistry.synchronize(...));

//And even Notification system to prevent multiple notifications for one event....

registerRoute(DefaultRoutes.STYLES);
registerRoute(DefaultRoutes.IMAGES);
registerRoute(DefaultRoutes.SCRIPTS);
