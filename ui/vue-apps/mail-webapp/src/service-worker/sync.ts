import pLimit from "p-limit";
import { Limit } from "p-limit";
import {
    ContainerChangeset,
    ContainerSubscriptionModel,
    ItemFlag,
    ItemFlagFilter,
    ItemValue,
    ItemVersion,
    OwnerSubscriptionsClient
} from "@bluemind/core.container.api";
import { MailboxFolder, MailboxFoldersClient, MailboxItem, MailboxItemsClient } from "@bluemind/backend.mail.api";
import session from "@bluemind/session";
import { default as db, SyncOptions, SyncOptionsType } from "./MailDB";
import logger from "@bluemind/logger";

const limits: { [uid: string]: Limit } = {};
const SYNC_FILTER: ItemFlagFilter = { must: [], mustNot: [ItemFlag.Deleted] };

export async function syncMailFolders(): Promise<string[]> {
    session.revalidate();
    const updatedOwnerSubscription = await syncOwnerSubscriptions();
    const subscriptions = await db.getOwnerSubscriptions("mailboxacl");
    const userId = await session.userId;
    const userMailbox = subscriptions.find(subscription => subscription.value.owner === userId);
    let updatedFolderUids: string[] = [];
    if (userMailbox?.value.offlineSync) {
        logger.log("[SYNC][SW] user mailbox is offline synced");
        updatedFolderUids = await syncMyMailbox();
    } else if (updatedOwnerSubscription.find(subscription => subscription.uid === userMailbox?.uid)) {
        logger.log("[SYNC][SW] user mailbox is not offline synced, remove sync");
        await unsyncMyMailbox();
    } else {
        logger.log("[SYNC][SW] user mailbox is not offline synced");
    }
    return updatedFolderUids;
}

export async function syncOwnerSubscriptions(): Promise<ItemValue<ContainerSubscriptionModel>[]> {
    const domainUid = await session.domain;
    const userUid = await session.userId;
    const syncOptions = await getOrCreateSyncOptions(`${userUid}@${domainUid}.subscriptions`, "owner_subscriptions");
    const client = new OwnerSubscriptionsClient(await session.sid, domainUid, userUid);
    const changeset = await client.changesetById(syncOptions.version);
    const { created, updated, deleted, version } = changeset as Required<ContainerChangeset<number>>;

    const versionUpdated = version !== syncOptions.version;
    if (versionUpdated) {
        const ids = created.concat(updated);
        const updatedOwnerSubscriptions = await client.multipleGetById(ids);
        await db.deleteOwnerSubscriptions(deleted);
        await db.putOwnerSubscriptions(updatedOwnerSubscriptions);
        await db.updateSyncOptions({ ...syncOptions, version });
        return updatedOwnerSubscriptions;
    }
    return [];
}
export async function isSubscribedAndSynced(uid: string, filter?: ItemFlagFilter) {
    if (isRequestFilterSynced(filter) && (await db.isSubscribed(uid))) {
        const syncOptions = await db.getSyncOptions(uid);
        if (syncOptions?.pending) {
            await syncMailFolder(uid);
        }
        return true;
    }
    return false;
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
        const client = new MailboxItemsClient(await session.sid, uid);
        const changeset = await client.filteredChangesetById(syncOptions.version, SYNC_FILTER);
        const { created, updated, deleted, version } = changeset as Required<ContainerChangeset<ItemVersion>>;
        const versionUpdated = changeset.version !== syncOptions.version;
        if (versionUpdated) {
            const ids = created
                .reverse()
                .concat(updated.reverse())
                .map(({ id }) => id as number);
            const items = await fetchMailItemsByChunks(ids, uid);
            const deletedIds = deleted.map(({ id }) => id as number);
            await db.reconciliate({ uid, items, deletedIds }, { ...syncOptions, version });
        }
        await db.updateSyncOptions({ ...syncOptions, version, pending: false });
        return versionUpdated;
    } catch (error) {
        logger.error("[SW][MailFolder] error while syncing changeset", error);
        return false;
    }
}

export async function syncMyMailbox(): Promise<string[]> {
    const updatedMailFolders = await syncMailbox(await session.domain, await session.userId);
    return await Promise.all(
        updatedMailFolders
            .map(async folder => await markFolderSyncAsPending(folder.uid as string))
            .slice(0, 100)
            .map(async folderUidPromise => {
                const folderUid = await folderUidPromise;
                await syncMailFolder(folderUid);
                return folderUid;
            })
    );
}

export async function syncMailbox(
    domain: string,
    userId: string,
    pushedVersion?: number
): Promise<ItemValue<MailboxFolder>[]> {
    return await limit(userId + domain, async () => {
        const syncOptions = await getOrCreateSyncOptions(await getMailboxFullPath(), "mail_folder");
        return pushedVersion && pushedVersion <= syncOptions.version
            ? []
            : syncMailboxToVersion(domain, userId, syncOptions);
    });
}

export async function syncMailboxToVersion(
    domain: string,
    userId: string,
    syncOptions: SyncOptions
): Promise<ItemValue<MailboxFolder>[]> {
    const mailboxRoot = `user.${userId}`;
    const client = new MailboxFoldersClient(await session.sid, domain.replace(/\./g, "_"), `user.${userId}`);
    const changeset = await client.changesetById(syncOptions.version);
    const { created, updated, deleted, version } = changeset as Required<ContainerChangeset<number>>;
    if (version !== syncOptions.version) {
        const toBeUpdated = created.concat(updated);
        const mailFolders = await client.multipleGetById(toBeUpdated);
        await db.deleteMailFolders(mailboxRoot, deleted);
        await db.putMailFolders(mailboxRoot, mailFolders);
        await db.updateSyncOptions({ ...syncOptions, version });
        return mailFolders;
    }
    return [];
}

export async function unsyncMyMailbox() {
    await db.deleteSyncOptions(await getMailboxFullPath());
    const mailFolders = await db.getAllMailFolders(`user.${await session.userId}`);
    mailFolders.forEach(mailFolder => db.deleteSyncOptions(mailFolder.uid as string));
}

async function fetchMailItemsByChunks(ids: number[], uid: string): Promise<ItemValue<MailboxItem>[]> {
    const chunks = [...chunk(ids, 200)];
    const client = new MailboxItemsClient(await session.sid, uid);
    const responses = await Promise.all(chunks.map(async chunk => client.multipleGetById(chunk)));
    const result: ItemValue<MailboxItem>[] = [];
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

const markFolderSyncAsPending = async (folderUid: string): Promise<string> => {
    const syncOptions = await getOrCreateSyncOptions(folderUid, "mail_item");
    await db.updateSyncOptions({ ...syncOptions, pending: true });
    return folderUid;
};

async function getMailboxFullPath(): Promise<string> {
    return `user.${await session.userId}@${(await session.domain).replace(/\./g, "_")}`;
}

function isRequestFilterSynced(filter: ItemFlagFilter | undefined) {
    // All flag excluded from sync are also excluded for request
    // And all flag included for request are also included for sync.
    if (filter) {
        return (
            SYNC_FILTER.mustNot?.every(flag => filter.mustNot?.includes(flag)) &&
            SYNC_FILTER.must?.every(flag => filter.must?.includes(flag))
        );
    }
    return true;
}
