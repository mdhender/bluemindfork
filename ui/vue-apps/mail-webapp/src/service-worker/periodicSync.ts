import { maildb, SyncOptions } from "./MailDB";
import { userAtDomain, mailapi, sessionInfos, getDBName } from "./MailAPI";
import { MailItem } from "./entry";

export function registerPeriodicSync(fn: Function, interval = 60 * 1000, ...args: any[]) {
    fn(...args);
    return setInterval(fn, interval, ...args);
}

export async function syncMailFolders() {
    await syncMyMailbox();
    const folders = await (await maildb.getInstance(await getDBName())).getAllMailFolders();
    for (const folder of folders) {
        await syncMailFolder(folder.uid);
    }
}

export async function syncMailFolder(uid: string) {
    const syncOptions = await getSyncOptions(uid, "mail_item");
    const { created, updated, deleted, version } = await (await mailapi.getInstance()).mailItem.changeset(
        uid,
        syncOptions.version
    );
    if (version !== syncOptions.version) {
        const ids = created
            .reverse()
            .concat(updated.reverse())
            .map(({ id }) => id);
        const mailItems = await fetchMailItemsByChunks(ids, uid);
        (await maildb.getInstance(await getDBName())).reconciliate(
            { uid, items: mailItems, deletedIds: deleted.map(({ id }) => id) },
            { ...syncOptions, version }
        );
        (await maildb.getInstance(await getDBName())).updateSyncOptions({ ...syncOptions, version });
    }
}

export async function syncMyMailbox() {
    const { domain, userId } = await sessionInfos.getInstance();
    await syncMailbox(domain, userId);
}

async function syncMailbox(domain: string, userId: string) {
    const api = await mailapi.getInstance();
    const syncOptions = await getSyncOptions(userAtDomain({ userId, domain }), "mail_folder");
    const { created, updated, deleted, version } = await api.mailFolder.changeset(
        { domain, userId },
        syncOptions.version
    );
    if (version !== syncOptions.version) {
        const toBeUpdated = created.concat(updated);
        const mailFolders = (await api.mailFolder.fetch({ domain, userId })).filter(mailfolder =>
            toBeUpdated.includes(mailfolder.internalId)
        );
        await (await maildb.getInstance(await getDBName())).deleteMailFolders(deleted);
        await (await maildb.getInstance(await getDBName())).putMailFolders(mailFolders);
        await (await maildb.getInstance(await getDBName())).updateSyncOptions({ ...syncOptions, version });
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

async function getSyncOptions(uid: string, type: "mail_item" | "mail_folder") {
    const syncOptions = await (await maildb.getInstance(await getDBName())).getSyncOptions(uid);
    if (!syncOptions) {
        const syncOptions = createSyncOptions(uid, type);
        (await maildb.getInstance(await getDBName())).updateSyncOptions(syncOptions);
        return syncOptions;
    }
    return syncOptions;
}

function* chunk<T>(array: T[], chunk_size: number) {
    for (let i = 0; i < array.length; i += chunk_size) {
        yield array.slice(i, i + chunk_size);
    }
}
