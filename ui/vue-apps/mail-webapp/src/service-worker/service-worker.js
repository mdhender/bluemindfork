import { skipWaiting, clientsClaim } from "workbox-core";

import registerApiRoute, { apiRoutes } from "./workbox/registerApiRoute";
import registerCSSRoute from "./workbox/registerCSSRoute";
import registerScriptRoute from "./workbox/registerScriptRoute";
import registerPartRoute from "./workbox/registerPartRoute";

import { syncMailbox, syncMailFolders, syncMailFolder } from "./sync";
import { sessionInfos } from "./MailAPI";
import { logger } from "./logger";

clientsClaim();
skipWaiting();

registerPartRoute();
registerApiRoute(apiRoutes);
registerCSSRoute();
registerScriptRoute();

self.addEventListener("message", async ({ data }) => {
    if (data.type === "INIT_PERIODIC_SYNC") {
        logger.debug("Synchronization begins");
        await syncMailFolders();
        logger.debug("Synchronization ends");
    }
    if (data.type === "SYNC_CONTAINER") {
        logger.debug(`Synchronization of container ${data.body.mailbox} for ${data.body.version}`);
        await syncMailFolder(data.body.mailbox, data.body.version);
    }
    if (data.type === "SYNC_MAILBOX") {
        const { domain, userId } = await sessionInfos.getInstance();
        if (data.body.owner === userId) {
            logger.debug(`Synchronization of mailbox ${data.body.owner} for ${data.body.version}`);
            await syncMailbox(domain, userId, data.body.version);
        }
    }
});
