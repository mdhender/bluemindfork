import { ChangeSet, FilteredChangeSet, MailFolder, MailItem, SessionInfo } from "./entry";

//@FIXME: it might be possible to force certain methods depending on the route.
type Route = "mail_folders" | "mail_items";
type Method = "_all" | "_filteredChangesetById" | "_multipleById" | "_changesetById";

interface MailAPIOptions {
    sid: string;
}

interface MailItemAPI {
    fetch: (uid: string, ids: number[]) => Promise<MailItem[]>;
    changeset: (uid: string, version: number) => Promise<FilteredChangeSet>;
}

interface MailFolderAPI {
    fetch: ({ userId, domain }: { userId: string; domain: string }) => Promise<MailFolder[]>;
    changeset: ({ userId, domain }: { userId: string; domain: string }, version: number) => Promise<ChangeSet>;
}

class MailAPI {
    mailItem: MailItemAPI;
    mailFolder: MailFolderAPI;

    constructor(options: MailAPIOptions) {
        const requestInit: RequestInit = {
            headers: {
                "x-bm-apikey": options.sid
            },
            mode: "cors",
            credentials: "include"
        };

        this.mailItem = {
            fetch(uid: string, ids: number[]) {
                const route: Route = "mail_items";
                const method: Method = "_multipleById";
                return fetchAPI<MailItem[]>(`/api/${route}/${uid}/${method}`, {
                    ...requestInit,
                    body: JSON.stringify(ids),
                    method: "POST"
                });
            },

            async changeset(uid: string, version: number) {
                const method: Method = "_filteredChangesetById";
                const route: Route = "mail_items";
                return fetchAPI<FilteredChangeSet>(`/api/${route}/${uid}/${method}?since=${version}`, {
                    ...requestInit,
                    body: JSON.stringify({ must: [], mustNot: ["Deleted"] }),
                    method: "POST"
                });
            }
        };

        this.mailFolder = {
            async fetch({ userId, domain }: { userId: string; domain: string }) {
                const route: Route = "mail_folders";
                const method: Method = "_all";
                return fetchAPI<MailFolder[]>(
                    `/api/${route}/${domain.replace(".", "_")}/${`user.${userId}`}/${method}`,
                    requestInit
                );
            },

            async changeset({ userId, domain }: { userId: string; domain: string }, version: number) {
                const method: Method = "_changesetById";
                const route: Route = "mail_folders";
                return fetchAPI<ChangeSet>(
                    `/api/${route}/${domain.replace(".", "_")}/${`user.${userId}`}/${method}?since=${version}`,
                    {
                        ...requestInit
                    }
                );
            }
        };
    }
}

async function fetchAPI<T>(url: string, requestInit?: RequestInit): Promise<T> {
    const response = await fetch(url, requestInit);
    if (response.ok) {
        return response.json();
    }
    if (response.status === 401) {
        return Promise.reject(`${response.status} Unauthorized`);
    }
    return Promise.reject(`Error in BM API ${response.status}`);
}

export const sessionInfos = (function () {
    let instance: SessionInfo | null;

    function init() {
        return fetchAPI<SessionInfo>("/session-infos");
    }
    return {
        getInstance: async function () {
            if (!instance) {
                instance = await init();
            }
            return instance;
        },
        clear: function () {
            instance = null;
        }
    };
})();

export const mailapi = (function () {
    let instance: MailAPI | null;

    async function init() {
        const { sid } = await sessionInfos.getInstance();
        return new MailAPI({ sid });
    }

    return {
        getInstance: async function () {
            if (!instance) {
                instance = await init();
            }
            return instance;
        },
        clear: function () {
            instance = null;
        }
    };
})();

export function userAtDomain({ userId, domain }: { userId: string; domain: string }) {
    return `user.${userId}@${domain.replace(".", "_")}`;
}

export async function getDBName() {
    return userAtDomain(await sessionInfos.getInstance());
}
