import { extensions } from "@bluemind/extensions";
import session from "@bluemind/session";

import registerPartRoute from "./routes/registerPartRoute";

import { syncMailbox, syncMailFolders, syncMailFolder } from "./sync";
import logger from "@bluemind/logger";
import MailboxItemsDBProxy from "./proxies/MailboxItemsDBProxy";
import MailboxItemsCacheProxy from "./proxies/MailboxItemsCacheProxy";
import MailboxFoldersDBProxy from "./proxies/MailboxFoldersDBProxy";
import OwnerSubscriptionsDBProxy from "./proxies/OwnerSubscriptionsDBProxy";

extensions.register("serviceworker.handlers", "net.bluemind.webapp.mail.js", {
    "api-handler": { class: MailboxItemsDBProxy, priority: 128 }
});
extensions.register("serviceworker.handlers", "net.bluemind.webapp.mail.js", {
    "api-handler": { class: MailboxItemsCacheProxy, priority: 129 }
});
extensions.register("serviceworker.handlers", "net.bluemind.webapp.mail.js", {
    "api-handler": { class: MailboxFoldersDBProxy, priority: 128 }
});
extensions.register("serviceworker.handlers", "net.bluemind.webapp.mail.js", {
    "api-handler": { class: OwnerSubscriptionsDBProxy, priority: 128 }
});
registerPartRoute();

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
    if (data.body.owner === (await session.userId)) {
        logger.log(`[SYNC][SW] Hierarchy synchronization: ${data.body.owner} in v${data.body.version}`);
        await syncMailbox(await session.domain, await session.userId, data.body.version);
    }
};

const refreshFolder = folderUids => {
    self.clients.matchAll().then(clients => {
        clients.forEach(client => client.postMessage({ type: "refresh", folderUids }));
    });
};
