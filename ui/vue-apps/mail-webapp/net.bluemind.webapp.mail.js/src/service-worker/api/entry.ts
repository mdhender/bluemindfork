export type UID = string;

interface ItemValue extends Object {
    created: number;
    createdBy: string;
    displayName: string;
    externalId?: any;
    flags: Array<any>;
    internalId: number;
    uid: UID;
    updated: number;
    updatedBy: string;
    version: number;
}
interface MailFolderValue {
    acls: Array<{ rights: string; subject: string }>;
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
export interface MailFolder extends ItemValue {
    value: MailFolderValue;
}
interface MailItemValue {
    body: any;
    flags: Array<"Seen">;
    imapUid: number;
    internalDate: number;
    internalFlags: Array<any>;
    lastUpdated: number;
    messageBody: string;
    modSeq: number;
}
export interface MailItem extends ItemValue {
    value: MailItemValue;
}

export interface ChangeSet {
    created: Array<{ id: number; version: number }>;
    deleted: Array<{ id: number; version: number }>;
    updated: Array<{ id: number; version: number }>;
    version: number;
}
