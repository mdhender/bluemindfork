import { MailFolder, ChangeSet, MailItem } from "./entry";

enum Route {
    mail_folders,
    mail_items
}
type RouteName = keyof typeof Route;

enum Method {
    _all,
    _filteredChangesetById,
    _multipleById
}
type MethodName = keyof typeof Method;

export interface BMResponse<T> extends Response {
    json(): Promise<T>;
}

export class MailAPI {
    requestInit: RequestInit;
    constructor(sid: string) {
        this.requestInit = {
            headers: {
                "x-bm-apikey": sid
            },
            mode: "cors",
            credentials: "include"
        };
    }

    async fetchMailFolders(domain: string, userId: string, init: RequestInit = {}): Promise<BMResponse<MailFolder[]>> {
        const route: RouteName = "mail_folders";
        const method: MethodName = "_all";
        return fetch(`/api/${route}/${domain.replace(".", "_")}/${`user.${userId}`}/${method}`, {
            ...this.requestInit,
            ...init
        });
    }

    fetchMailItems(uid: string, ids: number[]): Promise<BMResponse<MailItem[]>> {
        const route: RouteName = "mail_items";
        const method: MethodName = "_multipleById";
        return fetch(`/api/${route}/${uid}/${method}`, {
            ...this.requestInit,
            body: JSON.stringify(ids),
            method: "POST"
        });
    }

    async fetchChangeset(uid: string, version: number): Promise<BMResponse<ChangeSet>> {
        const method: MethodName = "_filteredChangesetById";
        const route: RouteName = "mail_items";
        return fetch(`/api/${route}/${uid}/${method}?since=${version}`, {
            ...this.requestInit,
            method: "POST",
            body: JSON.stringify({ must: [], mustNot: ["Deleted"] })
        });
    }
}

export async function getSessionInfos(): Promise<{ sid: string; userId: string; domain: string }> {
    const response = await fetch("/session-infos");
    return response.json();
}
