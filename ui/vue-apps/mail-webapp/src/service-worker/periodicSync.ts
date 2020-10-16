import { MailDB, SyncOptions } from "./MailDB";
import { MailAPI, getSessionInfos } from "./MailAPI";
import { MailItem } from "./entry";
import { logger } from "./logger";

declare const self: ServiceWorkerGlobalScope;
const db = new MailDB();

function createSyncOptions(uid: string, fullName: string): SyncOptions {
    return {
        fullName,
        uid,
        version: 0,
        minInterval: 60 * 1000,
        type: "mail_item"
    };
}

export async function registerPeriodicSync() {
    try {
        const { sid, domain, userId } = await getSessionInfos();
        const mailapi = new MailAPI({ sid });
        const folders = await mailapi.fetchMailFolders(domain, userId);
        const foldersSyncOptions = folders.map(folder => createSyncOptions(folder.uid, folder.value.fullName));

        for (const syncOptions of foldersSyncOptions) {
            await db.updateSyncOptions(syncOptions);
            interval(sync, syncOptions.minInterval, mailapi, syncOptions.uid, true);
        }
    } catch (error) {
        logger.error("Oops!", { error });
    }
}

function interval(fn: Function, timeout?: number, ...args: any[]) {
    fn(...args);
    return setInterval(fn, timeout, ...args);
}

export async function sync(mailapi: MailAPI, uid: string, scheduled?: boolean) {
    const syncOptions = (await db.getSyncOptions(uid)) || createSyncOptions(uid, "unknown");
    if (scheduled) {
        logger.log(
            `[${syncOptions.fullName}] Checking, next check in ${syncOptions.minInterval / 1000} seconds…`,
            syncOptions
        );
    }
    const { created, updated, deleted, version } = await fetchChanges(mailapi, syncOptions);
    const outofdate = version !== syncOptions.version;
    if (!outofdate) {
        return;
    }
    const time = Date.now();
    const ids = created
        .reverse()
        .concat(updated.reverse())
        .map(({ id }) => id);
    const mailItems = await fetchMailItems(mailapi, ids, uid);
    if (mailItems.length > 0) {
        showNotification(
            `[${syncOptions.fullName}] Fetch ${mailItems.length} mails in ${(Date.now() - time) / 1000} seconds.`
        );
    }
    db.reconciliate({ uid, items: mailItems, deletedIds: deleted.map(({ id }) => id) }, { ...syncOptions, version });
    if (deleted.length > 0) {
        showNotification(
            `[${syncOptions.fullName}] Delete ${deleted.length} mails in ${(Date.now() - time) / 1000} seconds.`
        );
    }
    showNotification(`[${syncOptions.fullName}] Database updated.`);
}

async function fetchMailItems(mailapi: MailAPI, ids: number[], uid: string) {
    const chunks = [...chunk(ids, 200)];
    const responses = await Promise.all(chunks.map(chunk => mailapi.fetchMailItems(uid, chunk)));
    const result: MailItem[] = [];
    return result.concat(...responses);
}

function showNotification(message: string) {
    self.registration.showNotification("BMail Sync", {
        body: message
    });
}

function* chunk<T>(array: T[], chunk_size: number) {
    for (let i = 0; i < array.length; i += chunk_size) {
        yield array.slice(i, i + chunk_size);
    }
}

async function fetchChanges(mailapi: MailAPI, syncOptions: SyncOptions) {
    return mailapi.fetchChangeset(syncOptions.uid, syncOptions.version);
}
