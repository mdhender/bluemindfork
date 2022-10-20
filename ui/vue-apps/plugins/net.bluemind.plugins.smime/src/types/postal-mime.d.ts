declare module "postal-mime" {
    export default class PostalMime {
        parse(email: string | ArrayBuffer): Promise<PostalMime.Message>;
    }

    export namespace PostalMime {
        export type Message = {
            headers: Array<Header>;
            from: Recipient;
            sender: Recipient;
            replyTo?: Recipient;
            deliveredTo?: EmailAddress;
            returnPath?: EmailAddress;
            to: Array<Recipient>;
            cc: Array<Recipient>;
            bcc: Array<Recipient>;
            subject: string;
            messageId: string;
            inReplyTo: string;
            references: string;
            date: string;
            html: string;
            text: string;
            attachments: Array<Attachment>;
        };
        export type Header = {
            key: string;
            value: string;
        };
        export type Recipient = {
            name: string;
            address: EmailAddress;
        };
        export type EmailAddress = string;
        export type Attachment = {
            filename?: string;
            mimeType: string;
            disposition?: "attachment" | "inline" | null;
            related?: boolean;
            contentId?: string;
            content: ArrayBuffer | string;
        };
    }
}
