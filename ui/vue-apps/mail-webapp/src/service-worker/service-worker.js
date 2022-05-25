import registerApiRoute, { apiRoutes } from "./workbox/registerApiRoute";
import registerSessionInfoRoute from "./workbox/registerSessionInfoRoute";
import registerPartRoute from "./workbox/registerPartRoute";

import { syncMailbox, syncMailFolders, syncMailFolder } from "./sync";
import Session from "./session";
import { logger } from "./logger";
import BrowserData from "./BrowserData";
import { extensions } from "@bluemind/extensions";
import { MailItemDB } from "./workbox/MailItemDB";

extensions.register("serviceworker.handlers", "mail-webapp", { "api-handler": { class: MailItemDB, priority: 1 } });

registerSessionInfoRoute();
registerPartRoute();
registerApiRoute(apiRoutes);

self.addEventListener("message", async ({ data }) => {
    switch (data.type) {
        case "INIT":
            await firstSynchronisation();

            break;
        case "SYNCHRONIZE":
            if (data.body.isHierarchy) {
                await synchronizeHierarchy(data);
            } else {
                await synchronizeFolder(data);
            }
            break;
        case "RESET":
            await BrowserData.reset(await Session.infos());
            break;
    }
});

const firstSynchronisation = async () => {
    logger.log("[SYNC][SW] Initialisation");
    const updatedFolderUids = await syncMailFolders();
    refreshFolder(updatedFolderUids);
};

const synchronizeFolder = async data => {
    logger.log(`[SYNC][SW] Folder synchronization: ${data.body.mailbox} in v${data.body.version}`);
    const updated = await syncMailFolder(data.body.mailbox, data.body.version);
    const updatedFolderUids = updated ? [data.body.mailbox] : [];
    refreshFolder(updatedFolderUids);
};

const synchronizeHierarchy = async data => {
    const { domain, userId } = await Session.infos();
    if (data.body.owner === userId) {
        logger.log(`[SYNC][SW] Hierarchy synchronization: ${data.body.owner} in v${data.body.version}`);
        await syncMailbox(domain, userId, data.body.version);
    }
};

const refreshFolder = folderUids => {
    self.clients.matchAll().then(clients => {
        clients.forEach(client => client.postMessage({ type: "refresh", folderUids }));
    });
};
