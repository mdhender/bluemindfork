import global from "@bluemind/global";

interface SessionInfos {
    accountType: string;
    bmBrandVersion: string;
    bmVersion: string;
    defaultEmail: string;
    domain: string;
    formatedName: string;
    mailboxCopyGuid: string;
    lang: string;
    login: string;
    roles: string;
    sid: string;
    userId: string;
}

const ANONYMOUS: SessionInfos = {
    accountType: "",
    bmBrandVersion: "",
    bmVersion: "",
    defaultEmail: "",
    domain: "",
    formatedName: "",
    lang: "",
    login: "",
    mailboxCopyGuid: "",
    roles: "",
    sid: "",
    userId: ""
};
const REFRESH_SESSION_INTERVAL = 30 * 1000;
let infos: SessionInfos | undefined;
async function instance(): Promise<SessionInfos> {
    if (!infos || shouldRefreshSession()) {
        try {
            infos = await fetchSession();
            global.session.expiration = Date.now() + REFRESH_SESSION_INTERVAL;
        } catch (e) {
            // For now fetchSession should never fail...
            infos = ANONYMOUS;
            global.session.expiration = Date.now() + 1000;
        }
    }
    return infos;
}

function shouldRefreshSession() {
    return global.session.expiration < Date.now();
}

async function fetchSession(): Promise<SessionInfos> {
    const response = await fetch("/session-infos");
    if (response.ok) {
        return response.json();
    }
    if (response.status === 401) {
        return Promise.reject(`${response.status} Unauthorized`);
    }
    return Promise.reject(`Error while fetching infos ${response.status}`);
}

interface Session {
    accountType: Promise<string>;
    bmBrandVersion: Promise<string>;
    bmVersion: Promise<string>;
    defaultEmail: Promise<string>;
    domain: Promise<string>;
    formatedName: Promise<string>;
    mailboxCopyGuid: Promise<string>;
    lang: Promise<string>;
    login: Promise<string>;
    roles: Promise<string[]>;
    sid: Promise<string>;
    userId: Promise<string>;
    revalidate: () => void;
}
function init() {
    const session: Session = {
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
        get mailboxCopyGuid() {
            return instance().then(({ mailboxCopyGuid }) => mailboxCopyGuid);
        },
        get roles() {
            return instance().then(({ roles }) => roles.split(","));
        },
        get sid() {
            return instance().then(({ sid }) => sid);
        },
        get userId() {
            return instance().then(({ userId }) => userId);
        },
        revalidate() {
            global.session.expiration = Date.now() - 1;
        }
    };
    return { expiration: 0, infos: session };
}

if (!global.session) {
    global.session = init();
}

export default global.session.infos as Session; // expiration must be internal in code but global at execution (cross JS)
