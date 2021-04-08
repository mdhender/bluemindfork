import { ChangeSet, FilteredChangeSet, MailFolder, MailItem, OwnerSubscription, SessionInfo } from "./entry";

interface MailAPIOptions {
    sid: string;
}

type Uid = string;
type UserInfos = { userId: string; domain: string };
type Endpoint = string;
type EndpointMethods = { [key: string]: string };

interface API<T, V extends EndpointMethods> {
    methods: () => V;
    endpoint: (method: string, options: T) => Endpoint;
}

interface ChangelogAPIEndpointMethods extends EndpointMethods {
    mget: string;
    changeset: string;
    filteredChangeset: string;
}

interface IChangelogAPI<T, U> extends API<T, ChangelogAPIEndpointMethods> {
    methods: () => ChangelogAPIEndpointMethods;
    mget: (options: T, ids: number[]) => Promise<U[]>;
    changeset: (options: T, version: number) => Promise<ChangeSet>;
    filteredChangeset?: (options: T, version: number) => Promise<FilteredChangeSet>;
}

abstract class ChangelogAPI<T, U> implements IChangelogAPI<T, U> {
    requestInit: RequestInit;

    constructor(requestInit: RequestInit ) {
        this.requestInit = requestInit;
    }

    abstract endpoint(method: string, options: T): Endpoint;

    methods() {
        return {
            mget: "_mgetById",
            changeset: "_changesetById",
            filteredChangeset: "_filteredChangesetById"
        };
    }

    async mget(options: T, ids: number[]): Promise<U[]> {
        const endpoint = this.endpoint(this.methods().mget, options);
        return fetchAPI<U[]>(endpoint, {
            ...this.requestInit,
            body: JSON.stringify(ids),
            method: "POST"
        });
    }

    async changeset(options: T, version: number) {
        const endpoint = this.endpoint(this.methods().changeset, options);
        return fetchAPI<ChangeSet>(`${endpoint}?since=${version}`, this.requestInit);
    }
}

class MailItemAPI extends ChangelogAPI<Uid, MailItem> {
    endpoint(method: string, uid: Uid) {
        return `/api/mail_items/${uid}/${method}`;
    }

    methods() {
        return { ...super.methods(), mget: "_multipleById" };
    }

    async filteredChangeset(uid: Uid, version: number) {
        const endpoint = this.endpoint(this.methods().filteredChangeset, uid);
        return fetchAPI<FilteredChangeSet>(`${endpoint}?since=${version}`, {
            ...this.requestInit,
            body: JSON.stringify({ must: [], mustNot: ["Deleted"] }),
            method: "POST"
        });
    }
}

class MailFolderAPI extends ChangelogAPI<UserInfos, MailFolder> {
    endpoint(method: string, { userId, domain }: UserInfos) {
        return `/api/mail_folders/${domain.replace(".", "_")}/user.${userId}/${method}`;
    }
}

class OwnerSubscriptionsAPI extends ChangelogAPI<UserInfos, OwnerSubscription> {
    endpoint(method: string, { userId, domain }: UserInfos) {
        return `/api/containers/_subscriptions/${domain}/${userId}/${method}`;
    }
}

export class MailAPI {
    mailItem: MailItemAPI;
    mailFolder: MailFolderAPI;
    ownerSubscriptions: OwnerSubscriptionsAPI;

    constructor(options: MailAPIOptions) {
        const requestInit: RequestInit = {
            headers: {
                "x-bm-apikey": options.sid
            },
            mode: "cors",
            credentials: "include"
        };

        this.mailItem = new MailItemAPI(requestInit);
        this.mailFolder = new MailFolderAPI(requestInit);
        this.ownerSubscriptions = new OwnerSubscriptionsAPI(requestInit);
    }

    static async fetchSessionInfos(): Promise<SessionInfo> {
        return fetchAPI<SessionInfo>("/session-infos");
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
