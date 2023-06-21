import global from "@bluemind/global";

export interface Session {
    accountType: string;
    bmBrandVersion: string;
    bmVersion: string;
    defaultEmail: string;
    domain: string;
    formatedName: string;
    lang: string;
    login: string;
    roles: string;
    sid: string;
    userId: string;
}

const REFRESH_SESSION_INTERVAL = 30 * 1000;
let infos: Session | undefined;
async function instance(): Promise<Session> {
    if (!infos || shouldRefreshSession()) {
        infos = await fetchSession();
        global.session.expiration = Date.now() + REFRESH_SESSION_INTERVAL;
    }
    return infos;
}

function shouldRefreshSession() {
    return global.session.expiration < Date.now();
}

async function fetchSession(): Promise<Session> {
    const response = await fetch("/session-infos");
    if (response.ok) {
        return response.json();
    }
    if (response.status === 401) {
        return Promise.reject(`${response.status} Unauthorized`);
    }
    return Promise.reject(`Error while fetching infos ${response.status}`);
}

interface SessionPromise {
    accountType: Promise<string>;
    bmBrandVersion: Promise<string>;
    bmVersion: Promise<string>;
    defaultEmail: Promise<string>;
    domain: Promise<string>;
    formatedName: Promise<string>;
    lang: Promise<string>;
    login: Promise<string>;
    roles: Promise<string[]>;
    sid: Promise<string>;
    userId: Promise<string>;
}
function init() {
    const session: SessionPromise = {
        get accountType() {
            return instance().then(({ accountType }) => accountType);
        },
        get bmBrandVersion() {
            return instance().then(({ bmBrandVersion }) => bmBrandVersion);
        },
        get bmVersion() {
            return instance().then(({ bmVersion }) => bmVersion);
        },
        get defaultEmail() {
            return instance().then(({ defaultEmail }) => defaultEmail);
        },
        get domain() {
            return instance().then(({ domain }) => domain);
        },
        get formatedName() {
            return instance().then(({ formatedName }) => formatedName);
        },
        get lang() {
            return instance().then(({ lang }) => lang);
        },
        get login() {
            return instance().then(({ login }) => login);
        },
        get roles() {
            return instance().then(({ roles }) => roles.split(","));
        },
        get sid() {
            return instance().then(({ sid }) => sid);
        },
        get userId() {
            return instance().then(({ userId }) => userId);
        }
    };
    return { expiration: 0, infos: session };
}

if (!global.session) {
    global.session = init();
}

export default global.session.infos as SessionPromise; // expiration must be internal in code but global at execution (cross JS)
