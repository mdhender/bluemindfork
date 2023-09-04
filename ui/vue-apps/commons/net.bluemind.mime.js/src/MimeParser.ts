import MimeParser, { PostalMime } from "postal-mime";
import partition from "lodash.partition";
import { DispositionType, MessageBody } from "@bluemind/backend.mail.api";

type OptionalPart = MessageBody.Part | undefined;

export default class {
    protected message: PostalMime.Message | undefined;
    private partsContent: Map<MessageBody.Part, ArrayBuffer> = new Map();
    structure: MessageBody.Part | undefined;

    constructor(private address = "1") {}

    async parse(raw: string | ArrayBuffer): Promise<this> {
        if (this.message) {
            throw new Error("The parser is already initialized");
        }
        this.message = await new MimeParser().parse(raw);

        const [relatedParts, mixedParts] = partition(this.message.attachments, "related");
        const textPart = this.text(this.message.text);
        let htmlPart = this.html(this.message.html);
        htmlPart = this.related(
            htmlPart,
            relatedParts.map(part => this.attachment(part))
        );
        const content = this.alternative([textPart, htmlPart]);
        const structure = this.mixed([content, ...mixedParts.map(part => this.attachment(part))]);
        this.structure = structure ? structure : this.text(" ");
        return this;
    }

    hasAttachment(): boolean {
        return this.message?.attachments.some(({ disposition }) => disposition === "attachment") || false;
    }

    getPartContent(address: string): ArrayBuffer | undefined {
        for (const [part, content] of this.partsContent) {
            if (part.address === address) {
                return content;
            }
        }
    }
    getParts(): IterableIterator<MessageBody.Part> {
        return this.partsContent.keys();
    }
    private html(content?: string): OptionalPart {
        if (content && content.length > 0) {
            const part = inline(content, "text/html", this.address);
            this.partsContent.set(part, new TextEncoder().encode(content).buffer);
            return part;
        }
        return undefined;
    }
    private text(content?: string): OptionalPart {
        if (content && content.length > 0) {
            const part = inline(content, "text/plain", this.address);
            this.partsContent.set(part, new TextEncoder().encode(content).buffer);
            return part;
        }
        return undefined;
    }
    private attachment(original: PostalMime.Attachment): MessageBody.Part {
        const part = attachment(original, this.address);
        this.partsContent.set(part, original.content);
        return part;
    }
    private related(main: OptionalPart, related: Array<OptionalPart>) {
        const children = related.filter(Boolean) as Array<MessageBody.Part>;
        if (children.length > 0 && main) {
            return multipart([main, ...children], "related", this.address);
        }
        return main;
    }
    private mixed(parts: Array<OptionalPart>) {
        const children = parts.filter(Boolean) as Array<MessageBody.Part>;
        if (children.length > 1) {
            return multipart(children, "mixed", this.address);
        }
        return children[0];
    }
    private alternative(parts: Array<OptionalPart>) {
        const children = parts.filter(Boolean) as Array<MessageBody.Part>;
        if (children.length > 1) {
            return multipart(children, "alternative", this.address);
        }
        return children[0];
    }
}

function attachment(part: PostalMime.Attachment, address?: string): MessageBody.Part {
    return {
        address: address || "1",
        contentId: part.contentId,
        charset: "us-ascii",
        dispositionType: part.disposition?.toLocaleUpperCase() as DispositionType,
        encoding: "base64",
        fileName: part.filename,
        mime: part.mimeType,
        size: part.content.byteLength,
        children: []
    };
}

function inline(content: string, mime: string, address?: string): MessageBody.Part {
    return {
        address: address || "1",
        dispositionType: "INLINE",
        mime,
        size: content.length,
        encoding: "quoted-printable",
        charset: "utf-8",
        children: []
    };
}

function multipart(parts: Array<MessageBody.Part>, subType: string, address?: string): OptionalPart {
    return {
        mime: `multipart/${subType}`,
        address: address || "TEXT",
        children: adopt(parts, address)
    };
}

function adopt(parts: Array<MessageBody.Part>, parentAddress?: string): Array<MessageBody.Part> {
    return parts.map((part, index) => {
        part.address = (parentAddress ? `${parentAddress}.` : "") + (index + 1);
        if (part.children) {
            part.children = adopt(part.children, part.address);
        }
        return part;
    });
}
