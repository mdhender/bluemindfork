interface ItemValue<T> {
    created: number;
    createdBy: string;
    displayName: string;
    externalId?: any;
    flags: any[];
    internalId: number;
    uid: string;
    updated: number;
    updatedBy: string;
    version: number;
    value: T;
}
interface MailFolderValue {
    acls: { rights: string; subject: string }[];
    dataLocation: any;
    deleted: boolean;
    fullName: string;
    highestModSeq: number;
    lastAppendDate: number;
    lastUid: number;
    name: string;
    options: string;
    parentUid: any;
    pop3LastLogin: number;
    quotaRoot: any;
    recentTime: number;
    recentUid: number;
    syncCRC: number;
    uidValidity: number;
}

export type MailFolder = ItemValue<MailFolderValue>;
interface MailItemValue {
    body: any;
    flags: "Seen"[];
    imapUid: number;
    internalDate: number;
    internalFlags: any[];
    lastUpdated: number;
    messageBody: string;
    modSeq: number;
}

export type MailItem = ItemValue<MailItemValue>;

export type MailItemLight = {
    internalId: number;
    flags: "Seen"[];
    date: number;
    subject: string;
    size: number;
    sender: string;
};

export interface FilteredChangeSet {
    created: { id: number; version: number }[];
    deleted: { id: number; version: number }[];
    updated: { id: number; version: number }[];
    version: number;
}

export interface ChangeSet {
    created: number[];
    deleted: number[];
    updated: number[];
    version: number;
}

export interface Reconciliation<T> {
    uid: string;
    items: T[];
    deletedIds: number[];
}

export interface SessionInfo {
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

export interface Flags {
    must: string[];
    mustNot: string[];
}

export interface Field {
    column: string;
    dir: "Desc" | "Asc";
}

interface OwnerSubscriptionValue {
    containerUid: string;
    offlineSync: boolean;
    containerType: string;
    owner: string;
    defaultContainer: boolean;
    name: string;
}
export type OwnerSubscription = ItemValue<OwnerSubscriptionValue>;

export type SortDescriptor = {
    filter: Flags;
    fields: Array<Field>;
};
