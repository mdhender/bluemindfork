import { SyncOptions, SyncOptionsType } from "./MailDB";
import { MailFolder, MailItem, OwnerSubscription } from "./entry";
import pLimit from "p-limit";
import { Limit } from "p-limit";
import { logger } from "./logger";
import Session from "./session";

let limits: { [uid: string]: Limit } = {};

export async function syncMailFolders(): Promise<string[]> {
    Session.clear();
    const updatedOwnerSubscription = await syncOwnerSubscriptions();
    const session = await Session.instance();
    const mailboxSubscriptions = await session.db.getOwnerSubscriptions("mailboxacl");
    const userMailboxOfflineSync = mailboxSubscriptions.find(
        subscription => subscription.value.owner === session.infos.userId
    );
    let updatedFolderUids: string[] = [];
    if (userMailboxOfflineSync?.value.offlineSync) {
        logger.log("[SYNC][SW] user mailbox is offline synced");
        updatedFolderUids = await syncMyMailbox();
    } else if (updatedOwnerSubscription.find(subscription => subscription.uid === userMailboxOfflineSync?.uid)) {
        logger.log("[SYNC][SW] user mailbox is not offline synced, remove sync");
        await unsyncMyMailbox();
    } else {
        logger.log("[SYNC][SW] user mailbox is not offline synced");
    }
    return updatedFolderUids;
}

export async function syncOwnerSubscriptions(): Promise<OwnerSubscription[]> {
    const session = await Session.instance();
    const { domain, userId } = session.infos;
    const syncOptions = await getOrCreateSyncOptions(`${userId}@${domain}.subscriptions`, "owner_subscriptions");
    const { created, updated, deleted, version } = await session.api.ownerSubscriptions.changeset(
        {
            domain,
            userId
        },
        syncOptions.version
    );
    let versionUpdated = version !== syncOptions.version;
    if (versionUpdated) {
        const ids = created.concat(updated);
        const updatedOwnerSubscriptions = await session.api.ownerSubscriptions.mget({ domain, userId }, ids);
        await session.db.deleteOwnerSubscriptions(deleted);
        await session.db.putOwnerSubscriptions(updatedOwnerSubscriptions);
        await session.db.updateSyncOptions({ ...syncOptions, version });
        return updatedOwnerSubscriptions;
    }
    return [];
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
        const { created, updated, deleted, version } = await session.api.mailItem.filteredChangeset(
            uid,
            syncOptions.version
        );
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
        logger.error("[SW][MailFolder] error while syncing changeset", error);
        return false;
    }
}

export async function syncMyMailbox(): Promise<string[]> {
    const session = await Session.instance();
    const { domain, userId } = session.infos;
    const updatedMailFolders = await syncMailbox(domain, userId);
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
    const mailboxRoot = `user.${userId}`;
    const { created, updated, deleted, version } = await session.api.mailFolder.changeset(
        { domain, mailboxRoot },
        syncOptions.version
    );
    if (version !== syncOptions.version) {
        const toBeUpdated = created.concat(updated);
        const mailFolders = await session.api.mailFolder.mget({ domain, mailboxRoot }, toBeUpdated);
        await session.db.deleteMailFolders(mailboxRoot, deleted);
        await session.db.putMailFolders(mailboxRoot, mailFolders);
        await session.db.updateSyncOptions({ ...syncOptions, version });
        return mailFolders;
    }
    return [];
}

export async function unsyncMyMailbox() {
    const session = await Session.instance();
    await session.db.deleteSyncOptions(session.userAtDomain);
    const mailFolders = await session.db.getAllMailFolders(`user.${session.infos.userId}`);
    mailFolders.forEach(mailFolder => session.db.deleteSyncOptions(mailFolder.uid));
}

async function fetchMailItemsByChunks(ids: number[], uid: string): Promise<MailItem[]> {
    const chunks = [...chunk(ids, 200)];
    const responses = await Promise.all(chunks.map(async chunk => (await Session.api()).mailItem.mget(uid, chunk)));
    const result: MailItem[] = [];
    return result.concat(...responses);
}

export async function limit<T>(uid: string, fn: () => Promise<T>): Promise<T> {
    if (!(uid in limits)) {
        limits[uid] = pLimit(1);
    }
    return await limits[uid](fn);
}

function createSyncOptions(uid: string, type: SyncOptionsType): SyncOptions {
    return {
        uid,
        version: 0,
        type
    };
}

async function getOrCreateSyncOptions(uid: string, type: SyncOptionsType): Promise<SyncOptions> {
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
