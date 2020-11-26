import { skipWaiting } from "workbox-core";

import registerApiRoute from "./workbox/registerApiRoute";
import registerCSSRoute from "./workbox/registerCSSRoute";
import registerImageRoute from "./workbox/registerImageRoute";
import registerScriptRoute from "./workbox/registerScriptRoute";
import registerPartRoute from "./workbox/registerPartRoute";

import { logger } from "./logger";
import { registerPeriodicSync, syncMailFolders, syncMyMailbox } from "./periodicSync";
import { mailapi, sessionInfos } from "./MailAPI";

skipWaiting();

registerPartRoute();
registerApiRoute();
registerCSSRoute();
registerImageRoute();
registerScriptRoute();

self.addEventListener("message", event => {
    if (event.data.type === "INIT_PERIODIC_SYNC") {
        const interval = registerPeriodicSync(() => {
            syncMailFolders().catch(e => {
                logger.error(`Sync stopped due to following exception : `, e);
                clearInterval(interval);
                mailapi.clear();
                sessionInfos.clear();
            });
        });
        logger.log(`Synchronization registered with the interval id ${interval}.`);
        logger.log(
            `To stop the synchronization, use "navigator.serviceWorker.controller.postMessage({type:"STOP_PERIODIC_SYNC", payload: { interval: ${interval} }})".`
        );
    }
    if (event.data.type === "STOP_PERIODIC_SYNC") {
        const interval = event.data.payload?.interval;
        if (interval) {
            logger.log(`Sync stopped.`);
            clearInterval(interval);
        } else {
            logger.error("Need an interval in the payload object.");
            logger.warn(
                `To stop the synchronization, use "navigator.serviceWorker.controller.postMessage({type:"STOP_PERIODIC_SYNC", payload: { interval: ${interval} }})".`
            );
        }
    }
    if (event.data.type === "SYNC_MY_MAILBOX") {
        syncMyMailbox();
    }
    if (event.data.type === "SYNC_MY_FOLDERS") {
        syncMailFolders();
    }
});
