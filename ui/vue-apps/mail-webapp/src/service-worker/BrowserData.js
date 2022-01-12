import { deleteDB } from "idb";
import { logger } from "./logger";
import Session from "./session";
export default {
    async resetIfNeeded({ mailboxCopyGuid }) {
        if (await areBrowserDataDeprecated(mailboxCopyGuid)) {
            await this.reset();
        }
    },

    async reset() {
        broadcast("RESET", { status: "START" });
        try {
            // Cache API
            logger.log(`[SW][BrowserData] Resetting caches.`);
            const cacheNames = await caches.keys();
            await Promise.all(cacheNames.map(name => deleteCache(name)));
            // IndexedDB

            logger.log(`[SW][BrowserData] Resetting databases.`);
            const databases = await indexedDB.databases();
            await Promise.all(databases.map(database => deleteDatabase(database)));
        } catch (e) {
            broadcast("RESET", { status: "ERROR" });
        }
        // (await Session.environment()).initialize();
        broadcast("RESET", { status: "SUCCESS" });
    }
};

async function areBrowserDataDeprecated(remote) {
    const local = await (await Session.environment()).getMailboxCopyGuid();
    if (local === undefined) {
        logger.log(`[SW][BrowserData] Browser copy uid initialized (${remote}).`);
        await (await Session.environment()).setMailboxCopyGuid(remote);
        return false;
    }
    logger.log(`[SW][BrowserData] Browser copy uid reset needed (${local} != ${remote}).`);
    return local !== remote;
}

function broadcast(type, data) {
    self.clients.matchAll().then(clients => {
        clients.forEach(client => client.postMessage({ type: type, ...data }));
    });
}

async function deleteCache(name) {
    logger.log(`[SW][BrowserData] Start reseting cache ${name}.`);
    await caches.delete(name);
    logger.log(`[SW][BrowserData] Cache ${name} reseted.`);
}

async function deleteDatabase({ name }) {
    logger.log(`[SW][BrowserData] Start deleting databe ${name}.`);
    await deleteDB(name);
    logger.log(`[SW][BrowserData] Database ${name} deleted.`);
}
