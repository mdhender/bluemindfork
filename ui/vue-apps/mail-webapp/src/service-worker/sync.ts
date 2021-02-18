import { maildb, SyncOptions } from "./MailDB";
import { userAtDomain, mailapi, sessionInfos, getDBName, clearSessions } from "./MailAPI";
import { MailItem } from "./entry";
import pLimit from "p-limit";
import { Limit } from "p-limit";
import { logger } from "./logger";

let limits: { [uid: string]: Limit } = {};

export async function syncMailFolders() {
    clearSessions();
    await syncMyMailbox();
    const folders = await (await maildb.getInstance(await getDBName())).getAllMailFolders();
    for (const folder of folders) {
        await syncMailFolder(folder.uid);
    }
}

export async function syncMailFolder(uid: string, pushedVersion?: number): Promise<boolean> {
    return await limit(uid, async () => {
        const syncOptions = await getSyncOptions(uid, "mail_item");
        return pushedVersion && pushedVersion <= syncOptions.version
            ? false
            : syncMailFolderToVersion(uid, syncOptions);
    });
}

async function syncMailFolderToVersion(uid: string, syncOptions: SyncOptions): Promise<boolean> {
    try {
        const api = await mailapi.getInstance();
        const { created, updated, deleted, version } = await api.mailItem.changeset(uid, syncOptions.version);
        if (version !== syncOptions.version) {
            const ids = created
                .reverse()
                .concat(updated.reverse())
                .map(({ id }) => id);
            const mailItems = await fetchMailItemsByChunks(ids, uid);
            const db = await maildb.getInstance(await getDBName());
            await db.reconciliate(
                { uid, items: mailItems, deletedIds: deleted.map(({ id }) => id) },
                { ...syncOptions, version }
            );
            await db.updateSyncOptions({ ...syncOptions, version });
            return true;
        }
    } catch (error) {
        logger.error("error while syncing changet", error);
    }
    return false;
}

export async function syncMyMailbox(): Promise<boolean> {
    const { domain, userId } = await sessionInfos.getInstance();
    return await syncMailbox(domain, userId);
}

export async function syncMailbox(domain: string, userId: string, pushedVersion?: number): Promise<boolean> {
    return await limit(userId + domain, async () => {
        const syncOptions = await getSyncOptions(userAtDomain({ userId, domain }), "mail_folder");
        return pushedVersion && pushedVersion <= syncOptions.version
            ? false
            : syncMailboxToVersion(domain, userId, syncOptions);
    });
}

export async function syncMailboxToVersion(domain: string, userId: string, syncOptions: SyncOptions): Promise<boolean> {
    const api = await mailapi.getInstance();
    const { created, updated, deleted, version } = await api.mailFolder.changeset(
        { domain, userId },
        syncOptions.version
    );
    if (version !== syncOptions.version) {
        const toBeUpdated = created.concat(updated);
        const api = await mailapi.getInstance();
        const mailFolders = (await api.mailFolder.fetch({ domain, userId })).filter(mailfolder =>
            toBeUpdated.includes(mailfolder.internalId)
        );
        const db = await maildb.getInstance(await getDBName());
        await db.deleteMailFolders(deleted);
        await db.putMailFolders(mailFolders);
        await db.updateSyncOptions({ ...syncOptions, version });
        return true;
    }
    return false;
}

async function fetchMailItemsByChunks(ids: number[], uid: string): Promise<MailItem[]> {
    const chunks = [...chunk(ids, 200)];
    const responses = await Promise.all(
        chunks.map(async chunk => (await mailapi.getInstance()).mailItem.fetch(uid, chunk))
    );
    const result: MailItem[] = [];
    return result.concat(...responses);
}

export async function limit<T>(uid: string, fn: () => Promise<T>): Promise<T> {
    if (!(uid in limits)) {
        limits[uid] = pLimit(1);
    }
    return await limits[uid](fn);
}

function createSyncOptions(uid: string, type: "mail_item" | "mail_folder"): SyncOptions {
    return {
        uid,
        version: 0,
        type
    };
}

async function getSyncOptions(uid: string, type: "mail_item" | "mail_folder"): Promise<SyncOptions> {
    const db = await maildb.getInstance(await getDBName());
    const syncOptions = await db.getSyncOptions(uid);
    if (!syncOptions) {
        const syncOptions = createSyncOptions(uid, type);
        await db.updateSyncOptions(syncOptions);
        return syncOptions;
    }
    return syncOptions;
}

function* chunk<T>(array: T[], chunk_size: number) {
    for (let i = 0; i < array.length; i += chunk_size) {
        yield array.slice(i, i + chunk_size);
    }
}
