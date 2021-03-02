import { skipWaiting, clientsClaim } from "workbox-core";

import registerApiRoute, { apiRoutes } from "./workbox/registerApiRoute";
import registerCSSRoute from "./workbox/registerCSSRoute";
import registerScriptRoute from "./workbox/registerScriptRoute";
import registerPartRoute from "./workbox/registerPartRoute";

import { syncMailbox, syncMailFolders, syncMailFolder } from "./sync";
import Session from "./session";
import { logger } from "./logger";

clientsClaim();
skipWaiting();

registerPartRoute();
registerApiRoute(apiRoutes);
registerCSSRoute();
registerScriptRoute();

self.addEventListener("message", async ({ data, ports }) => {
    if (data.type === "INIT") {
        await firstSynchronisation(ports[0]);
    } else if (data.type === "SYNCHRONIZE") {
        if (data.body.isHierarchy) {
            await synchronizeHierarchy(data);
        } else {
            await synchronizeFolder(data, ports[0]);
        }
    }
});

const firstSynchronisation = async port => {
    logger.log("[SYNC][SW] Initialisation");
    const updatedFolderUid = await syncMailFolders();
    if (port) {
        port.postMessage(updatedFolderUid);
    }
};

const synchronizeFolder = async (data, port) => {
    logger.log(`[SYNC][SW] Folder synchronization: ${data.body.mailbox} in v${data.body.version}`);
    const updated = await syncMailFolder(data.body.mailbox, data.body.version);
    if (port) {
        port.postMessage(updated);
    }
};

const synchronizeHierarchy = async data => {
    const { domain, userId } = await Session.infos();
    if (data.body.owner === userId) {
        logger.log(`[SYNC][SW] Hierarchy synchronization: ${data.body.owner} in v${data.body.version}`);
        await syncMailbox(domain, userId, data.body.version);
    }
};
