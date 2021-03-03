import { SyncOptions } from "./MailDB";
import { MailFolder, MailItem } from "./entry";
import pLimit from "p-limit";
import { Limit } from "p-limit";
import { logger } from "./logger";
import Session from "./session";

let limits: { [uid: string]: Limit } = {};

export async function syncMailFolders(): Promise<string[]> {
    Session.clear();
    const session = await Session.instance();
    const updatedMailFolders = await syncMyMailbox();
    return await Promise.all(
        updatedMailFolders
            .map(async folder => await markFolderSyncAsPending(session, folder.uid))
            .slice(0, 100)
            .map(async folderUidPromise => {
                const folderUid = await folderUidPromise;
                await syncMailFolder(folderUid);
                return folderUid;
            })
    );
}

export async function syncMailFolder(uid: string, pushedVersion?: number): Promise<boolean> {
    return await limit(uid, async () => {
        const syncOptions = await getOrCreateSyncOptions(uid, "mail_item");
        return pushedVersion && pushedVersion <= syncOptions.version
            ? false
            : syncMailFolderToVersion(uid, syncOptions);
    });
}

async function syncMailFolderToVersion(uid: string, syncOptions: SyncOptions): Promise<boolean> {
    try {
        const session = await Session.instance();
        const { created, updated, deleted, version } = await session.api.mailItem.changeset(uid, syncOptions.version);
        let versionUpdated = version !== syncOptions.version;
        if (versionUpdated) {
            const ids = created
                .reverse()
                .concat(updated.reverse())
                .map(({ id }) => id);
            const mailItems = await fetchMailItemsByChunks(ids, uid);
            await session.db.reconciliate(
                { uid, items: mailItems, deletedIds: deleted.map(({ id }) => id) },
                { ...syncOptions, version }
            );
        }
        await session.db.updateSyncOptions({ ...syncOptions, version, pending: false });
        return versionUpdated;
    } catch (error) {
        logger.error("[SW][MailFolder] error while syncing changet", error);
        return false;
    }
}

export async function syncMyMailbox(): Promise<MailFolder[]> {
    const { domain, userId } = await Session.infos();
    return await syncMailbox(domain, userId);
}

export async function syncMailbox(domain: string, userId: string, pushedVersion?: number): Promise<MailFolder[]> {
    return await limit(userId + domain, async () => {
        const syncOptions = await getOrCreateSyncOptions(await Session.userAtDomain(), "mail_folder");
        return pushedVersion && pushedVersion <= syncOptions.version
            ? []
            : syncMailboxToVersion(domain, userId, syncOptions);
    });
}

export async function syncMailboxToVersion(
    domain: string,
    userId: string,
    syncOptions: SyncOptions
): Promise<MailFolder[]> {
    const session = await Session.instance();
    const { created, updated, deleted, version } = await session.api.mailFolder.changeset(
        { domain, userId },
        syncOptions.version
    );
    if (version !== syncOptions.version) {
        const toBeUpdated = created.concat(updated);
        const allMailFolders = await session.api.mailFolder.fetch({ domain, userId });
        const mailFoldersByInternalId = Object.fromEntries(
            allMailFolders.map(mailFolder => [mailFolder.internalId, mailFolder])
        );
        const mailFolders = toBeUpdated.map(updatedFolder => mailFoldersByInternalId[updatedFolder]);
        await session.db.deleteMailFolders(deleted);
        await session.db.putMailFolders(mailFolders);
        await session.db.updateSyncOptions({ ...syncOptions, version });
        return mailFolders;
    }
    return [];
}

async function fetchMailItemsByChunks(ids: number[], uid: string): Promise<MailItem[]> {
    const chunks = [...chunk(ids, 200)];
    const responses = await Promise.all(chunks.map(async chunk => (await Session.api()).mailItem.fetch(uid, chunk)));
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

async function getOrCreateSyncOptions(uid: string, type: "mail_item" | "mail_folder"): Promise<SyncOptions> {
    const db = await Session.db();
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

const markFolderSyncAsPending = async (session: Session, folderUid: string): Promise<string> => {
    const db = session.db;
    const syncOptions = await getOrCreateSyncOptions(folderUid, "mail_item");
    await db.updateSyncOptions({ ...syncOptions, pending: true });
    return folderUid;
};
