import Bottleneck from "bottleneck";
import { MailDB, FolderSyncInfo } from "./MailDB";
import { MailAPI, getSessionInfos } from "./MailAPI";
import { MailFolder } from "./entry";

const db = new MailDB();

const chunkSize = 200;
function initializeLimiter() {
    return new Bottleneck({
        maxConcurrent: 1,
        minTime: 30 * 1000
    });
}

function createFolderSyncInfo(folder: MailFolder): FolderSyncInfo {
    return {
        fullName: folder.value.fullName,
        uid: folder.uid,
        version: 0,
        minInterval: 60 * 1000
    };
}

export async function registerPeriodicSync() {
    const { sid, domain, userId } = await getSessionInfos();
    const mailapi = new MailAPI(sid);
    const folders = await mailapi.fetchMailFolders(domain, userId).then(response => response.json());
    await db.putMailFolders(folders);
    const foldersSyncInfo = folders.map(folder => createFolderSyncInfo(folder));
    await Promise.all(
        foldersSyncInfo.map(folderSyncInfo => {
            return db.updateFolderSyncInfo(folderSyncInfo);
        })
    );
    foldersSyncInfo.forEach(syncInfo => {
        scheduleUpdates(mailapi, syncInfo.uid);
        setInterval(() => {
            scheduleUpdates(mailapi, syncInfo.uid);
        }, syncInfo.minInterval);
    });
}

async function scheduleUpdates(mailapi: MailAPI, uid: string) {
    const syncInfo = await db.getFolderSyncInfo(uid);
    if (syncInfo === undefined) {
        return;
    }
    const changeSet = await mailapi.fetchChangeset(syncInfo.uid, syncInfo.version).then(response => response.json());
    const folderUid = syncInfo.uid;

    if (changeSet.version !== syncInfo.version) {
        await db.applyChangeset(changeSet, folderUid, syncInfo);
        const limiter = initializeLimiter();
        const chunks = await buildChunks(folderUid, chunkSize);
        for (const ids of chunks.ids) {
            if (ids.length > 0) {
                const response = await limiter.schedule(() => mailapi.fetchMailItems(chunks.folderUid, ids));
                const mailItems = await response.json();
                await db.putMailItems(mailItems, folderUid);
            }
        }
    }
}

async function buildChunks(folderUid: string, chunkSize: number) {
    let cursor = await (await db.dbPromise)
        .transaction("ids_stack", "readwrite")
        .store.index("by-folderUid")
        .openCursor(folderUid);
    const ids: number[][] = [[]];
    while (cursor) {
        ids[ids.length - 1].push(cursor.value.internalId);
        if (ids[ids.length - 1].length === chunkSize) {
            ids.push([]);
        }
        cursor = await cursor.continue();
    }
    return {
        folderUid,
        ids
    };
}
