import { PostalMime } from "postal-mime";
import { MessageBody, MessageBodyRecipientKind as RecipientKind } from "@bluemind/backend.mail.api";
import MimeParser from "./MimeParser";

export default class extends MimeParser {
    body: MessageBody;
    constructor(body: MessageBody) {
        super();
        this.body = body;
    }
    async parse(raw: string | ArrayBuffer): Promise<this> {
        await super.parse(raw);
        this.body.structure = this.structure as MessageBody.Part;
        const message = this.message as PostalMime.Message;
        message.attachments.some(({ disposition }) => disposition === "attachment");
        this.body.smartAttach = this.hasAttachment();
        this.body.preview = preview(message);
        this.body.recipients = [
            recipient(RecipientKind.Originator, this.message?.from),
            ...(message.to?.map(recipient.bind(null, RecipientKind.Primary)) || []),
            ...(message.cc?.map(recipient.bind(null, RecipientKind.CarbonCopy)) || []),
            ...(message.bcc?.map(recipient.bind(null, RecipientKind.BlindCarbonCopy)) || [])
        ].filter(Boolean) as Array<MessageBody.Recipient>;

        this.body.date = new Date(message.date || "").getTime();
        this.body.headers = headers(message.headers);
        this.body.messageId = message.messageId;
        this.body.subject = message.subject;
        return this;
    }
}

function preview({ text }: PostalMime.Message): string {
    return text?.replace(/\s+/g, " ").trim().substring(0, 160) || "";
}

function recipient(kind: RecipientKind, recipient?: PostalMime.Recipient): MessageBody.Recipient | undefined {
    if (recipient) {
        return { kind, dn: recipient.name, address: recipient.address };
    }
}

function headers(headers: Array<PostalMime.Header>): Array<MessageBody.Header> {
    const values: Map<string, MessageBody.Header> = new Map();
    headers.forEach(header => {
        const key = header.key.toLowerCase();
        if (!BLACKLIST.includes(key)) {
            if (!values.has(key)) {
                values.set(key, { name: key, values: [header.value] });
            } else {
                values.get(key)?.values.push(header.value);
            }
        }
    });
    return [...values.values()];
}

const BLACKLIST: Array<string> = [
    "bcc",
    "cc",
    "content-transfer-encoding",
    "content-type",
    "date",
    "from",
    "message-id",
    "mime-version",
    "subject",
    "to"
];
