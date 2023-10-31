import global from "@bluemind/global";
import { EventTarget } from "@bluemind/event";
import isEqual from "lodash.isequal";

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
type SessionChangedEvent = CustomEvent<{ value: SessionInfos; old?: SessionInfos }>;
type SessionRefreshEvent = CustomEvent<SessionInfos>;

interface SessionEventMap {
    change: SessionChangedEvent;
    refresh: SessionRefreshEvent;
}

type SessionEventType = keyof SessionEventMap;
interface Session extends EventTarget {
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
    addEventListener<T extends SessionEventType>(
        type: T,
        listener: (this: Session, ev: SessionEventMap[T]) => unknown,
        options?: AddEventListenerOptions
    ): void;
    removeEventListener<T extends SessionEventType>(
        type: T,
        listener?: (this: Session, ev: SessionEventMap[T]) => unknown
    ): void;
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
let expiration = 0;
const target = new EventTarget();
async function instance(): Promise<SessionInfos> {
    if (!infos || shouldRefreshSession()) {
        const old = infos;
        try {
            infos = await fetchSession();
            expiration = Date.now() + REFRESH_SESSION_INTERVAL;
        } catch (e) {
            // For now fetchSession should never fail...
            infos = ANONYMOUS;
            expiration = Date.now() + 1000;
        }
        if (!isEqual(old, infos)) {
            target.dispatchEvent(new CustomEvent("change", { detail: { old, value: infos } }));
        }
        target.dispatchEvent(new CustomEvent("refresh", { detail: infos }));
    }
    return infos;
}

function shouldRefreshSession() {
    return expiration < Date.now();
}

async function fetchSession(): Promise<SessionInfos> {
    try {
        const response = await fetch("/session-infos");
        if (response.ok) {
            return response.json();
        }
        if (response.status === 401) {
            return Promise.reject(`${response.status} Unauthorized`);
        }
        return Promise.reject(`Error while fetching infos ${response.status}`);
    } catch {
        // If offline return last infos.
        return infos || ANONYMOUS;
    }
}

if (!global.session) {
    global.session = {
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
            expiration = Date.now() - 1;
        },
        addEventListener: function (...args: Parameters<EventTarget["addEventListener"]>): void {
            target.addEventListener(...args);
        },
        dispatchEvent: function (event: Event): boolean {
            return target.dispatchEvent(event);
        },
        removeEventListener: function (...args: Parameters<EventTarget["removeEventListener"]>): void {
            target.removeEventListener(...args);
        }
    } as Session;
}

export default global.session as Session; // expiration must be internal in code but global at execution (cross JS)
