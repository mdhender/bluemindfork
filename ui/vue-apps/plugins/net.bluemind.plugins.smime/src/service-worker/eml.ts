import partition from "lodash.partition";
import MimeParser, { PostalMime } from "postal-mime";
import { DispositionType, ItemValue, MailboxItem, MessageBody } from "@bluemind/backend.mail.api";

type OptionalPart = MessageBody.Part | undefined;

export async function parseEmlPart(item: ItemValue<MailboxItem>, raw: string): Promise<MessageBody> {
    return await parseBodyStructure(item.value.body, raw);
}
export async function parseEml(raw: string): Promise<string> {
    return raw;
}
export async function parseBodyStructure(body: MessageBody, eml: string | ArrayBuffer): Promise<MessageBody> {
    const message = await new MimeParser().parse(eml);
    body.smartAttach = message.attachments.some(({ disposition }) => disposition === "attachment");
    body.structure = buildStructure(message);
    if (message.text) {
        body.preview = `[Decrypted message] ${message.text}`;
    } else if (message.html) {
        body.preview = `[Decrypted message] ${message.html.replace(/<[^>]*>/g, "")}`;
    }
    body.preview = body.preview.replace(/\n/g, "").substring(0, 120).trim();
    return body;
}

export default { parseEmlPart, parseEml, parseBodyStructure };
/*
function buildBody(message) {
    const body = {};
    body.smartAttach = message.attachments.some(({ disposition }) => disposition === "attachment");
    body.structure = buildStructure(message);
    const partsData = {};
    const visit = part => {
        if (part.content) {
            partsData[part.address] = part.content;
            delete part.content;
        }
        part.children?.forEach(visit);
    };
    visit(body.structure);

    if (message.text) {
        body.preview = message.text;
    } else if (message.html) {
        body.preview = message.html.replaceAll(/<[^>]*>/g, "");
    }
    body.preview = body.preview.replaceAll("\n", "").replaceAll("\n", "").substring(0, 120).trim();

    const from = { kind: RecipientKind.Originator, dn: message.from.name, address: message.from.address };
    const toArray = message.to?.map(to => ({ kind: RecipientKind.Primary, dn: to.name, address: to.address })) || [];
    const ccArray = message.cc?.map(cc => ({ kind: RecipientKind.CarbonCopy, dn: cc.name, address: cc.address })) || [];
    const bccArray =
        message.bcc?.map(bcc => ({
            kind: RecipientKind.BlindCarbonCopy,
            dn: bcc.name,
            address: bcc.address
        })) || [];
    body.recipients = [from, ...toArray, ...ccArray, ...bccArray];

    body.date = message.date;
    body.headers = message.headers.map(({ key, value }) => {
        let json;
        try {
            json = JSON.parse(value);
        } catch {
            json = null;
        }
        return { name: key, values: json ? [value] : value.split(/,\s* /) };
    });
    body.messageId = message.messageId;
    body.subject = message.subject;

    return { body, partsData };
}
*/

function buildStructure({ html, text, attachments }: PostalMime.Message): MessageBody.Part {
    const [related, files] = partition(attachments, "related");
    const textPart = DefaultPartBuilder.build({ content: text, mimeType: "text/plain" });
    let htmlPart = DefaultPartBuilder.build({ content: html, mimeType: "text/html" });
    const relatedParts = related.map(attachment => DefaultPartBuilder.build(attachment));
    htmlPart = RelatedPartBuilder.build(htmlPart, relatedParts);
    const content = DefaultMultiPartBuilder.build([textPart, htmlPart], "alternative");
    const fileParts = files.map(attachment => DefaultPartBuilder.build(attachment));
    const structure = DefaultMultiPartBuilder.build([content, ...fileParts], "mixed") as MessageBody.Part;
    return structure;
}

const DefaultPartBuilder = {
    build({ mimeType: mime, content, disposition, filename, contentId }: PostalMime.Attachment): OptionalPart {
        if (content) {
            const part: MessageBody.Part = {
                address: "1",
                dispositionType: (disposition?.toUpperCase() as DispositionType) || "INLINE",
                mime,
                size: content instanceof ArrayBuffer ? content.byteLength : content.length,
                encoding: ""
            };
            if (contentId) {
                part.contentId = contentId;
            }
            if (filename) {
                part.fileName = filename;
            }
            return part;
        }
    }
};

const RelatedPartBuilder = {
    build(main: OptionalPart, related: Array<OptionalPart>): OptionalPart {
        const children = related.filter(Boolean) as Array<MessageBody.Part>;
        if (children.length > 0 && main) {
            return {
                mime: "multipart/related",
                address: "TEXT",
                children: adopt([main, ...children])
            };
        }
        return main;
    }
};

const DefaultMultiPartBuilder = {
    build(parts: Array<OptionalPart>, subType: string): OptionalPart {
        const children = parts.filter(Boolean) as Array<MessageBody.Part>;
        if (children.length > 1) {
            return {
                mime: `multipart/${subType}`,
                address: "TEXT",
                children: adopt(children)
            };
        }
        return children[0];
    }
};

function adopt(parts: Array<MessageBody.Part>, prefix = ""): Array<MessageBody.Part> {
    return parts.map((part, index) => {
        part.address = `${prefix}${index + 1}`;
        if (part.children) {
            part.children = adopt(part.children, `${part.address}.`);
        }
        return part;
    });
}
