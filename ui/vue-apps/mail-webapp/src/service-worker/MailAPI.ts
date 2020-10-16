import { MailFolder, ChangeSet, MailItem } from "./entry";

//@FIXME: it might be possible to force certain methods depending on the route.
type Route = "mail_folders" | "mail_items";
type Method = "_all" | "_filteredChangesetById" | "_multipleById";

interface MailAPIOptions {
    sid: string;
}

export class MailAPI {
    requestInit: RequestInit;

    constructor(options: MailAPIOptions) {
        this.requestInit = {
            headers: {
                "x-bm-apikey": options.sid
            },
            mode: "cors",
            credentials: "include"
        };
    }

    async fetchMailFolders(domain: string, userId: string) {
        const route: Route = "mail_folders";
        const method: Method = "_all";
        return fetchAPI<MailFolder[]>(
            `/api/${route}/${domain.replace(".", "_")}/${`user.${userId}`}/${method}`,
            this.requestInit
        );
    }

    fetchMailItems(uid: string, ids: number[]) {
        const route: Route = "mail_items";
        const method: Method = "_multipleById";
        return fetchAPI<MailItem[]>(`/api/${route}/${uid}/${method}`, {
            ...this.requestInit,
            body: JSON.stringify(ids),
            method: "POST"
        });
    }

    async fetchChangeset(uid: string, version: number) {
        const method: Method = "_filteredChangesetById";
        const route: Route = "mail_items";
        return fetchAPI<ChangeSet>(`/api/${route}/${uid}/${method}?since=${version}`, {
            ...this.requestInit,
            body: JSON.stringify({ must: [], mustNot: ["Deleted"] }),
            method: "POST"
        });
    }
}

async function fetchAPI<T>(url: string, requestInit?: RequestInit): Promise<T> {
    const response = await fetch(url, requestInit);
    if (response.ok) {
        return response.json();
    }
    return Promise.reject(response.statusText);
}

export async function getSessionInfos() {
    return fetchAPI<{ sid: string; userId: string; domain: string }>("/session-infos");
}
