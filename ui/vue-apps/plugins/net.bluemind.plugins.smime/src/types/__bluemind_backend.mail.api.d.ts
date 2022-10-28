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
        subject?: string;
        preview?: string;
        smartAttach: boolean;
        structure: MessageBody.Part;
        headers: Array<MessageBody.Header>;
        recipients: Array<MessageBody.Recipient>;
        messageId?: string;
        date: number;
    }

    export namespace MessageBody {
        export interface Part {
            address: string;
            charset?: string;
            contentId?: string;
            dispositionType?: DispositionType;
            encoding?: string;
            fileName?: string;
            mime: string;
            size?: number;
            children?: Array<Part>;
        }
        export interface Header {
            name: string;
            values: Array<string>;
        }

        export interface Recipient {
            kind: RecipientKind;
            dn: string;
            address: string;
        }
        export type RecipientKind = "Originator" | "Sender" | "Primary" | "CarbonCopy" | "BlindCarbonCopy";
    }
    export type DispositionType = "ATTACHMENT" | "INLINE";

    export const MessageBodyRecipientKind: {
        Originator: MessageBody.RecipientKind;
        Sender: MessageBody.RecipientKind;
        Primary: MessageBody.RecipientKind;
        CarbonCopy: MessageBody.RecipientKind;
        BlindCarbonCopy: MessageBody.RecipientKind;
    };
}
