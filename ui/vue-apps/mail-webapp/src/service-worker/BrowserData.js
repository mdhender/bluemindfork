import { deleteDB } from "idb";
import { logger } from "./logger";
import Session from "./session";
export default {
    async resetIfNeeded(userSession) {
        if (await areBrowserDataDeprecated(userSession.mailboxCopyGuid)) {
            await this.reset(userSession);
        }
    },

    async reset(userSession) {
        broadcast("RESET", { status: "START" });
        try {
            // Cache API
            logger.log(`[SW][BrowserData] Resetting caches.`);
            const cacheNames = await caches.keys();
            await Promise.all(cacheNames.map(name => deleteCache(name)));

            // IndexedDB
            logger.log(`[SW][BrowserData] Resetting databases.`);
            const databaseNames = await listDatabases(userSession);
            await Promise.all(databaseNames.map(name => deleteDatabase(name)));

            broadcast("RESET", { status: "SUCCESS" });
        } catch (e) {
            logger.error("[BrowserData] fail to reset local data.", e);
            broadcast("RESET", { status: "ERROR" });
        }
    }
};

async function listDatabases(userSession) {
    try {
        const databases = await indexedDB.databases();
        return databases.map(({ name }) => name);
    } catch {
        // remove catch once Firefox will support indexedDB.databases()
        // https://developer.mozilla.org/en-US/docs/Web/API/IDBFactory/databases#browser_compatibility
        // https://bugzilla.mozilla.org/show_bug.cgi?id=934640
        const newWebmailDbName = `user.${userSession.userId}@${userSession.domain.replace(".", "_")}:webapp/mail`;
        return [
            "capabilities",
            "context",
            "environment",
            "tag",
            "folder",
            "contact",
            "calendarview",
            "calendar",
            "todolist",
            "auth",
            "deferredaction",
            newWebmailDbName
        ];
    }
}

async function areBrowserDataDeprecated(remote) {
    const local = await (await Session.environment()).getMailboxCopyGuid();
    if (local === undefined) {
        logger.log(`[SW][BrowserData] Browser copy uid initialized (${remote}).`);
        await (await Session.environment()).setMailboxCopyGuid(remote);
        return false;
    }
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

async function deleteDatabase(name) {
    logger.log(`[SW][BrowserData] Start deleting databe ${name}.`);
    await deleteDB(name);
    logger.log(`[SW][BrowserData] Database ${name} deleted.`);
}
