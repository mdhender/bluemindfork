interface SessionInfo {
    login: string;
    accountType: string;
    defaultEmail: string;
    sid: string;
    userId: string;
    hasIM: string;
    lang: string;
    domain: string;
    roles: string;
    formatedName: string;
    bmVersion: string;
    bmBrandVersion: string;
}
export interface Session {
    // login: Promise<string>;
    // accountType: Promise<string>;
    // defaultEmail: Promise<string>;
    sid: Promise<string>;
    // userId: Promise<string>;
    // hasIM: Promise<string>;
    // lang: Promise<string>;
    // domain: Promise<string>;
    // roles: string;
    // formatedName: Promise<string>;
    // bmVersion: Promise<string>;
    // bmBrandVersion: Promise<string>;
    clear(): void;
}
let infos: SessionInfo | undefined;
function instance(): Promise<SessionInfo> {
    if (!infos) {
        return new Promise(async resolve => {
            infos = await fetchSessionInfos();
            resolve(infos);
        });
    }
    return Promise.resolve(infos);
}
const session: Session = {
    get sid() {
        return instance().then(({ sid }) => sid);
    },
    clear() {
        infos = undefined;
    }
};
export default session;

async function fetchSessionInfos(): Promise<SessionInfo> {
    const response = await fetch("/session-infos");
    if (response.ok) {
        return response.json();
    }
    if (response.status === 401) {
        return Promise.reject(`${response.status} Unauthorized`);
    }
    return Promise.reject(`Error while fetching infos ${response.status}`);
}
