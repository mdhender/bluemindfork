// insert a build version number here ${version} to invalidate the previous installed Service Worker on clients 
const CACHE_NAME = "bm-assets";
const files = ["${scope}/", "${files?join("\",\"", "", "\"]")};
const UNREAD_CACHE_TIMEOUT = 10 * 1000; 

self.addEventListener("install", function (event) {
    event.waitUntil(precacheredirected(files).then(self.skipWaiting()));
});

self.addEventListener("fetch", function (event) {
    event.respondWith(serve(event.request));
});

async function serve(request) {
    if (/session-infos.js$/.test(request.url)) {
        return fetchSessionInfo(request);
    } else if (/\/api\/mailboxes\/.*\/_unread$/.test(request.url)) {
        return fromUnreadCache(request);
    } else if (request.destination === "document") {
        return fromNetwork(request);
    } else {
        return fromCache(request);
    }
}

async function fromUnreadCache(request) {
    const cache = await caches.open("bm-api-legacy");
    let response = await cache.match(request);
    if(!isUnreadResponseValid(response)) {
        response = await fetch(request);
        if(!isUnreadResponseValid(response)) {
            response = new Response(response);
            response.headers.set(date, new Date().toString());
        }
        cache.put(request, response.clone());
    }
    return response
}


function isUnreadResponseValid(response) {
    if (!response || !response.headers.has("date")) {
        return false;
    }
    const date = new Date(response.headers.get("date"));
    if (date instanceof Date && !isNaN(date)) {
        return Date.now() - date.getTime() < UNREAD_CACHE_TIMEOUT
    }

}

async function fromCache(request) {
    const matching = await caches.match(request);
    return matching || fetch(request);
}

async function fromNetwork(request, opt_cache) {
    const name = opt_cache || CACHE_NAME;
    try {
        const response = await fetch(request);
        const cache = await caches.open(name);
        cache.put(request, response.clone());
        return response;
    } catch {
        return caches.match(request);
    }
}

async function precacheredirected(files) {
    const cache = await caches.open(CACHE_NAME);
    Promise.all(
        files.map(async function (url) {
            const response = await fetch(new Request(url, { redirect: "manual" })); // Required by https://bugs.chromium.org/p/chromium/issues/detail?id=669363&desc=2#c1
            cache.put(url, response);
        })
    );
}

// Copy of Reset mechanism from root-webapp service worker.

self.addEventListener("message", async ({ data }) => {
    switch (data.type) {
        case "RESET":
            await BrowserData.reset(await Session.infos());
            break;
    }
});

// root-webapp/sevice-worker/workbox/registerSessionInfoRoute.js

async function fetchSessionInfo(request) {
    try {
        await BrowserData.resetIfNeeded(await Session.infos());
    } catch (e) {
        logger.error("[SW][BrowserData] Fail to reset browser data");
        logger.error(e);
    }    
    return fromNetwork(request);
}


let instance;

// root-webapp/sevice-worker/session.ts

class Session {
    constructor(infos) {
        this.infos = infos;
        this._environment = null;
    }

    environment() {
        if (!this._environment) {
            this._environment = new EnvironmentDB();
        }
        return this._environment;
    }

    static async instance() {
        if (!instance) {
            const response = await fetch("/session-infos");
            const infos = await response.json();
            instance = new Session(infos);
        }
        return instance;
    }

    static async infos() {
        return (await Session.instance()).infos;
    }

    static async environment() {
        return (await Session.instance()).environment();
    }
}

// root-webapp/sevice-worker/workbox/EnvironmentDB.ts

class EnvironmentDB {
    static version = 1;

    constructor() {
        this.db = this.openDB();
    }

    async openDB() {
        return openDB("environment", EnvironmentDB.version, {
            upgrade(db, oldVersion) {
                logger.log(`[SW][DB] Upgrading from ` + oldVersion + ` to ` + EnvironmentDB.version);
                if (oldVersion < EnvironmentDB.version) {
                    logger.log("[SW][DB] Upgrading deleting existing object store");
                    for (const name of Object.values(db.objectStoreNames)) {
                        db.deleteObjectStore(name);
                    }
                }
                db.createObjectStore("system", { keyPath: "key" });
            },
            blocking: async () => {
                (await this.db).close();
                this.db = this.initialize();
            }
        });
    }

    async setMailboxCopyGuid(uid) {
        logger.log(`[SW][DB] Initialize environment mailboxCopyGuid to ` + uid + `.`);
        await putInDb(await this.db, "system", { key: "mailboxCopyGuid", value: uid });
    }

    async getMailboxCopyGuid() {
        const data = await getFromDb(await this.db,"system", "mailboxCopyGuid");
        if (data === undefined) {
            return undefined;
        }
        return data.value;
    }
}

// root-webapp/sevice-worker/workbox/BrowserData.ts

const BrowserData = {
    async resetIfNeeded(userSession) {
        if (await areBrowserDataDeprecated(userSession.mailboxCopyGuid)) {
            await this.reset(userSession);
        }
    },

    async reset(userSession) {
        broadcast("RESET", { status: "START" });
        try {
            // Cache API
            logger.log(`[SW][BrowserData] Resetting caches.`);
            const cacheNames = await caches.keys();
            await Promise.all(cacheNames.map(name => deleteCache(name)));
            
            // IndexedDB
            logger.log(`[SW][BrowserData] Resetting databases.`);
            const databaseNames = await listDatabases(userSession);
            await Promise.all(databaseNames.map(name => deleteDatabase(name)));
            broadcast("RESET", { status: "SUCCESS" });
        } catch (e) {
            broadcast("RESET", { status: "ERROR" });
        }
    }
};

async function areBrowserDataDeprecated(remote) {
    const local = await (await Session.environment()).getMailboxCopyGuid();
    if (local === undefined) {
        logger.log(`[SW][BrowserData] Browser copy uid initialized (` + remote + `).`);
        await (await Session.environment()).setMailboxCopyGuid(remote);
        return false;
    }
    return local !== remote;
}

async function listDatabases(userSession) {
    try {
        const databases = await indexedDB.databases();
        return databases.map(({ name }) => name);
    } catch {
        // remove this catch once Firefox will support indexedDB.databases()
        // https://developer.mozilla.org/en-US/docs/Web/API/IDBFactory/databases#browser_compatibility
        // https://bugzilla.mozilla.org/show_bug.cgi?id=934640
        const newWebmailDbName = `user.` + userSession.userId + `@` + userSession.domain.replace(".", "_")+ `:webapp/mail`;
        return [
            "capabilities",
            "context",
            "environment",
            "tag",
            "folder",
            "contact",
            "calendarview",
            "calendar",
            "todolist",
            "auth",
            "deferredaction",
            newWebmailDbName
        ];
    }
}

function broadcast(type, data) {
    self.clients.matchAll().then(clients => {
        clients.forEach(client => client.postMessage({ type: type, ...data }));
    });
}

async function deleteCache(name) {
    logger.log(`[SW][BrowserData] Start reseting cache ` + name + `.`);
    await caches.delete(name);
    logger.log(`[SW][BrowserData] Cache ` + name + ` reseted.`);
}

async function deleteDatabase(name) {
    logger.log(`[SW][BrowserData] Start deleting database ` + name + `.`);
    await deleteDB(name);
    logger.log(`[SW][BrowserData] Database ` + name + ` deleted.`);
}

// root-webapp/sevice-worker/logger.js

const methods = {
    log: "#00acac",
    warn: "#ffbc0c",
    error: "#ff5c5c"
};
const styles = method => {
    return [
        `background: ` + methods[method],
        `border-radius: 0.5em`,
        `color: white`,
        `font-weight: bold`,
        `padding: 2px 0.5em`
    ];
};

function print(method) {
    return function (...args) {
        console[method](...["%cBM/ServiceWorker", styles(method).join(";")], ...args);
    };
}
const logger = Object.keys(methods).reduce((acc, method) => {
    return {
        ...acc,
        [method]: print(method)
    };
}, {});

// idb library mock

function deleteDB(name) {
    const promise = new Promise((resolve, reject) => {
        const request = indexedDB.deleteDatabase(name);
        request.onerror = reject;
        request.onsuccess =  () => resolve(request.result);
    });
    return promise;
}

function openDB(name, version, { upgrade, blocking }) {
    const request = indexedDB.open(name, version);
    const promise = new Promise((resolve, reject) => {
        request.onerror = reject;
        request.onsuccess = () => resolve(request.result);
    });

    request.addEventListener("upgradeneeded", event => {
        upgrade(request.result, event.oldVersion);
    });
    promise
        .then(db => {
            db.addEventListener("versionchange", () => blocking());
        })
        .catch(() => {});
    return promise;
}

function getFromDb(database, store, key) {
    const request = database.transaction(store, "readwrite").objectStore(store).get(key);
    const promise = new Promise((resolve, reject) => {
        request.onerror = reject;
        request.onsuccess = () => resolve(request.result);
    });
    return promise;
}

function putInDb(database, store, data) {
    const request = database.transaction(store, "readwrite").objectStore(store).put(data);
    const promise = new Promise((resolve, reject) => {
        request.onerror = reject;
        request.onsuccess = () => resolve(request.result);
    });
    return promise;
}
