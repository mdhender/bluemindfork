//Temporary fix to support types in client
declare module "@bluemind/backend.mail.api" {
    export class MailboxItemsClient {
        next(): never;
        replicatedMailboxUid: string;
        constructor(sid: string, replicatedMailboxUid: string);
        fetch(imapUid: string, address: string, encoding?: string, charset?: string, filename?: string): Promise<Blob>;
    }

    export interface ItemValue<T> {
        value: T;
    }

    export interface MailboxItem {
        body: MessageBody;
        imapUid: string;
    }

    export interface MessageBody {
        preview: string;
        smartAttach: boolean;
        structure: MessageBody.Part;
    }

    export namespace MessageBody {
        export interface Part {
            address: string;
            contentId?: string;
            dispositionType?: DispositionType;
            encoding?: string;
            fileName?: string;
            mime: string;
            size?: number;
            children?: Array<Part>;
        }
    }
    export type DispositionType = "ATTACHMENT" | "INLINE";
}
