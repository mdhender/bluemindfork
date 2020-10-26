import { MailDB, SyncOptions } from "./MailDB";
import { createFolderId, mailapi, sessionInfos } from "./MailAPI";
import { MailItem } from "./entry";
import { logger } from "./logger";

declare const self: ServiceWorkerGlobalScope;
const db = new MailDB();

export function registerPeriodicSync(fn: Function, interval = 60 * 1000, ...args: any[]) {
    fn(...args);
    return setInterval(fn, interval, ...args);
}

export async function syncMailFolders() {
    await syncMyMailbox();
    const folders = await db.getAllMailFolders();
    for (const folder of folders) {
        await syncMailFolder(folder.uid, folder.value.fullName);
    }
}

export async function syncMailFolder(uid: string, fullName?: string) {
    const syncOptions = await getSyncOptions(uid);
    const { created, updated, deleted, version } = await (await mailapi.getInstance()).mailItem.changeset(
        uid,
        syncOptions.version
    );
    if (version !== syncOptions.version) {
        if (fullName) {
            showNotification(`Synchronization of the "${fullName}" folder.`);
        }
        logger.log("version updated");
        const ids = created
            .reverse()
            .concat(updated.reverse())
            .map(({ id }) => id);
        const mailItems = await fetchMailItemsByChunks(ids, uid);
        db.reconciliate(
            { uid, items: mailItems, deletedIds: deleted.map(({ id }) => id) },
            { ...syncOptions, version }
        );
        db.updateSyncOptions({ ...syncOptions, version });
    } else {
        logger.log("up to date");
    }
}

export async function syncMyMailbox() {
    const { domain, userId } = await sessionInfos.getInstance();
    await syncMailbox(domain, userId);
}

async function syncMailbox(domain: string, userId: string, login?: string) {
    const api = await mailapi.getInstance();
    const syncOptions = await getSyncOptions(createFolderId({ userId, domain }));
    const { version } = await api.mailFolder.changeset({ domain, userId }, syncOptions.version);
    if (version !== syncOptions.version) {
        if (login) {
            showNotification(`Synchronization of the email box "${login}".`);
        }
        logger.log("version updated");
        const mailFolders = await api.mailFolder.fetch({ domain, userId });
        db.putMailFolders(mailFolders);
        db.updateSyncOptions({ ...syncOptions, version });
    } else {
        logger.log("up to date");
    }
}

async function fetchMailItemsByChunks(ids: number[], uid: string) {
    const chunks = [...chunk(ids, 200)];
    const responses = await Promise.all(
        chunks.map(async chunk => (await mailapi.getInstance()).mailItem.fetch(uid, chunk))
    );
    const result: MailItem[] = [];
    return result.concat(...responses);
}

function createSyncOptions(uid: string, type: "mail_item" | "mail_folder"): SyncOptions {
    return {
        uid,
        version: 0,
        type
    };
}

async function getSyncOptions(uid: string) {
    const syncOptions = await db.getSyncOptions(uid);
    if (!syncOptions) {
        const syncOptions = createSyncOptions(uid, "mail_folder");
        db.updateSyncOptions(syncOptions);
        return syncOptions;
    }
    return syncOptions;
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
