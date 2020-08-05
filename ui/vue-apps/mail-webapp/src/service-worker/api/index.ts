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

export class MailAPI {
    requestInit: RequestInit;
    constructor(apikey: string) {
        this.requestInit = {
            headers: {
                "x-bm-apikey": apikey
            },
            mode: "cors",
            credentials: "include"
        };
    }

    async getMailFolders(partition: string, mailboxRoot: string, init: RequestInit = {}): Promise<Array<MailFolder>> {
        const route: RouteName = "mail_folders";
        const method: MethodName = "_all";
        const response = await fetch(`/api/${route}/${partition}/${mailboxRoot}/${method}`, {
            ...this.requestInit,
            ...init
        });
        if (!response.ok) {
            throw new Error("Network response was not ok");
        }
        return response.json();
    }

    async getMailItems(
        folder: Pick<MailFolder, "uid">,
        ids: Array<Pick<MailItem, "internalId">>
    ): Promise<Array<MailItem>> {
        const route: RouteName = "mail_items";
        const method: MethodName = "_multipleById";
        const response = await fetch(`/api/${route}/${folder.uid}/${method}`, {
            ...this.requestInit,
            body: JSON.stringify(ids.map(({ internalId }) => internalId)),
            method: "POST"
        });
        if (!response.ok) {
            throw new Error("Network response was not ok");
        }
        return response.json();
    }

    async getChangeset(folder: Pick<MailFolder, "uid">, version: number): Promise<ChangeSet> {
        const method: MethodName = "_filteredChangesetById";
        const route: RouteName = "mail_items";
        const response = await fetch(`/api/${route}/${folder.uid}/${method}?since=${version}`, {
            ...this.requestInit,
            method: "POST",
            body: JSON.stringify({ must: [], mustNot: ["Deleted"] })
        });
        if (!response.ok) {
            throw new Error("Network response was not ok");
        }
        return response.json();
    }
}
