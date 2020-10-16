export type UID = string;

interface ItemValue<T> {
    created: number;
    createdBy: string;
    displayName: string;
    externalId?: any;
    flags: any[];
    internalId: number;
    uid: UID;
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
export interface ChangeSet {
    created: { id: number; version: number }[];
    deleted: { id: number; version: number }[];
    updated: { id: number; version: number }[];
    version: number;
}

export interface Reconciliation<T> {
    uid: string;
    items: T[];
    deletedIds: number[];
}
