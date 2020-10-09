import Bottleneck from "bottleneck";
import { MailDB, FolderSyncInfo } from "./MailDB";
import { MailAPI, getSessionInfos } from "./MailAPI";
import { MailFolder } from "./entry";

function log(...args: any[]) {
    const styles = [
        `background: #00acac`,
        `border-radius: 0.5em`,
        `color: white`,
        `font-weight: bold`,
        `padding: 2px 0.5em`
    ];
    console.log(...["%cperiodicSync.ts", styles.join(";")], ...args);
}

const db = new MailDB();

const chunkSize = 200;
function initializeBottleneck() {
    const limiter = new Bottleneck({
        maxConcurrent: 1,
        minTime: 10 * 1000
    });
    return limiter;
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
    let limiter = initializeBottleneck();
    const { sid, domain, userId } = await getSessionInfos();
    const mailapi = new MailAPI(sid);
    const folders = await mailapi.fetchMailFolders(domain, userId).then(response => response.json());
    await db.putMailFolders(folders);
    const foldersSyncInfo = folders.map(folder => createFolderSyncInfo(folder));
    await Promise.all(foldersSyncInfo.map(folderSyncInfo => db.updateFolderSyncInfo(folderSyncInfo)));
    log("registerPeriodicSync", { folders, foldersSyncInfo });
    limiter = await fillInLimiter(limiter, foldersSyncInfo, mailapi);

    for (const syncInfo of foldersSyncInfo) {
        interval(async () => {
            const updated = await updateIdStack(mailapi, syncInfo.uid);
            if (updated) {
                limiter = await fillInLimiter(limiter, foldersSyncInfo, mailapi);
            }
        }, syncInfo.minInterval);
    }
}

async function fillInLimiter(oldLimiter: Bottleneck, foldersSyncInfo: FolderSyncInfo[], mailapi: MailAPI) {
    oldLimiter.stop();
    const limiter = initializeBottleneck();
    for (const syncInfo of foldersSyncInfo) {
        let cursor = await (await db.dbPromise)
            .transaction("ids_stack", "readwrite")
            .store.index("by-folderUid")
            .openCursor(syncInfo.uid, "prev");
        while (cursor) {
            const ids: number[] = [];
            ids.push(cursor.value.internalId);
            cursor = await cursor.continue();
            while (cursor && ids.length < chunkSize) {
                ids.push(cursor.value.internalId);
                cursor = await cursor.continue();
            }
            limiter
                .schedule(async () => {
                    if (ids.length > 0) {
                        const response = await fetchData(mailapi, syncInfo.uid, ids);
                        const mailItems = await response.json();
                        log("limiter.schedul", { syncInfo, ids, mailItems });
                        return db.putMailItems(mailItems, syncInfo.uid);
                    }
                })
                .catch(() => {});
        }
    }
    return limiter;
}

function interval(fn: Function, minInterval: number) {
    log("interval", { minInterval });
    fn();
    setInterval(fn, minInterval);
}

export async function updateIdStack(mailapi: MailAPI, uid: string) {
    const syncInfo = await db.getFolderSyncInfo(uid);
    if (syncInfo) {
        const response = await mailapi.fetchChangeset(syncInfo.uid, syncInfo.version);
        const changeSet = await response.json();

        const outofdate = changeSet.version !== syncInfo.version;
        log("updateIdStack", { uid, syncInfo, changeSet });
        if (outofdate) {
            await db.applyChangeset(changeSet, syncInfo.uid, syncInfo);
            return Promise.resolve(true);
        }
    }
    return Promise.resolve(false);
}

function fetchData(mailapi: MailAPI, uid: string, ids: number[]) {
    return mailapi.fetchMailItems(uid, ids);
}
