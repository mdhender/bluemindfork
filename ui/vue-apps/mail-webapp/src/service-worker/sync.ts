import { maildb, SyncOptions } from "./MailDB";
import { userAtDomain, mailapi, sessionInfos, getDBName } from "./MailAPI";
import { MailItem } from "./entry";
import pLimit from "p-limit";
import { Limit } from "p-limit";

let limits: { [uid: string]: Limit } = {};

export async function syncMailFolders() {
    await syncMyMailbox();
    const folders = await (await maildb.getInstance(await getDBName())).getAllMailFolders();
    for (const folder of folders) {
        await syncMailFolder(folder.uid);
    }
}

export async function syncMailFolder(uid: string, pushedVersion?: number) {
    await limit(uid, async () => {
        const syncOptions = await getSyncOptions(uid, "mail_item");
        return pushedVersion && pushedVersion <= syncOptions.version
            ? Promise.resolve()
            : syncMailFolderToVersion(uid, syncOptions);
    });
}

async function syncMailFolderToVersion(uid: string, syncOptions: SyncOptions) {
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
    }
}

export async function syncMyMailbox() {
    const { domain, userId } = await sessionInfos.getInstance();
    await syncMailbox(domain, userId);
}

export async function syncMailbox(domain: string, userId: string, pushedVersion?: number) {
    await limit(userId + domain, async () => {
        const syncOptions = await getSyncOptions(userAtDomain({ userId, domain }), "mail_folder");
        return pushedVersion && pushedVersion <= syncOptions.version
            ? Promise.resolve()
            : syncMailboxToVersion(domain, userId, syncOptions);
    });
}

export async function syncMailboxToVersion(domain: string, userId: string, syncOptions: SyncOptions) {
    const api = await mailapi.getInstance();
    const { created, updated, deleted, version } = await api.mailFolder.changeset(
        { domain, userId },
        syncOptions.version
    );
    if (version !== syncOptions.version) {
        const toBeUpdated = created.concat(updated);
        const mailFolders = (await api.mailFolder.fetch({ domain, userId })).filter(mailfolder =>
            toBeUpdated.includes(mailfolder.internalId)
        );
        const db = await maildb.getInstance(await getDBName());
        await db.deleteMailFolders(deleted);
        await db.putMailFolders(mailFolders);
        await db.updateSyncOptions({ ...syncOptions, version });
    }
}

export async function limit(uid: string, fn: () => Promise<void>) {
    if (!(uid in limits)) {
        limits[uid] = pLimit(1);
    }
    await limits[uid](fn);
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
