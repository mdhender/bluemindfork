import { deleteDB } from "idb";
import session from "@bluemind/session";
import { logger } from "./logger";
import db from "./EnvironmentDB";
declare const self: ServiceWorkerGlobalScope;

export default {
    async resetIfNeeded() {
        if (await areBrowserDataDeprecated(await session.mailboxCopyGuid)) {
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
            const databaseNames = await listDatabases();
            await Promise.all(databaseNames.map(name => deleteDatabase(name)));

            broadcast("RESET", { status: "SUCCESS" });
        } catch (e) {
            logger.error("[BrowserData] fail to reset local data.", e);
            broadcast("RESET", { status: "ERROR" });
        }
    }
};

async function listDatabases() {
    try {
        const databases = await indexedDB.databases();
        return databases.map(({ name }) => name as string);
    } catch {
        // remove catch once Firefox will support indexedDB.databases()
        // https://developer.mozilla.org/en-US/docs/Web/API/IDBFactory/databases#browser_compatibility
        // https://bugzilla.mozilla.org/show_bug.cgi?id=934640
        const mailboxFullPath = `user.${await session.userId}@${(await session.domain).replaceAll(".", "_")}`;
        const newWebmailDbName = `${mailboxFullPath}:webapp/mail`;
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
            newWebmailDbName,
            `${await session.userId}:smime:body`,
            `${await session.userId}:smime:pki`
        ];
    }
}

async function areBrowserDataDeprecated(remote: string) {
    const local = await db.getMailboxCopyGuid();
    if (local === undefined) {
        logger.log(`[SW][BrowserData] Browser copy uid initialized (${remote}).`);
        await db.setMailboxCopyGuid(remote);
        return false;
    }
    return local !== remote;
}

function broadcast(type: string, data: Record<string, string>) {
    self.clients.matchAll().then(clients => {
        clients.forEach(client => client.postMessage({ type: type, ...data }));
    });
}

async function deleteCache(name: string) {
    logger.log(`[SW][BrowserData] Start reseting cache ${name}.`);
    await caches.delete(name);
    logger.log(`[SW][BrowserData] Cache ${name} reseted.`);
}

async function deleteDatabase(name: string) {
    logger.log(`[SW][BrowserData] Start deleting database ${name}.`);
    await deleteDB(name);
    logger.log(`[SW][BrowserData] Database ${name} deleted.`);
}
