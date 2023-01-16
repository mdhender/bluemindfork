import Builder from "@bluemind/emailjs-mime-builder";
import { MessageBody } from "@bluemind/backend.mail.api";

export default class MimeBuilder {
    private getContent: (part: MessageBody.Part) => Promise<Uint8Array | string>;

    constructor(getContent: (part: MessageBody.Part) => Promise<Uint8Array | string>) {
        this.getContent = getContent;
    }

    async build(part: MessageBody.Part): Promise<string> {
        const node = await this.appendPart(part);
        return node.build();
    }

    private async appendPart(part: MessageBody.Part): Promise<Builder> {
        const isEncoded = !part.encoding; // if encoding is undefined, consider content is already encoded
        const node = new Builder(this.getContentType(part), { isEncoded });
        if (part.mime!.startsWith("multipart/") && part.children && part.children.length > 0) {
            for (const child of part.children) {
                const newNode = await this.appendPart(child);
                node.appendChild(newNode);
            }
        } else {
            const content = await this.getContent(part);
            node.setContent(content);
            if (part.dispositionType === "ATTACHMENT" && part.fileName) {
                node.setHeader("Content-Disposition", `attachment; filename="${part.fileName}`);
            } else if (part.dispositionType) {
                node.setHeader("Content-Disposition", part.dispositionType.toLowerCase());
            }
            if (part.contentId) {
                node.setHeader("Content-ID", part.contentId);
            }
        }
        part.headers?.forEach(header => {
            if (header.name && header.values && header.values[0]) {
                node.setHeader(header.name, header.values[0]);
            }
        });
        return node;
    }

    private getContentType(part: MessageBody.Part): string {
        const hasHeader = part.headers?.find(({ name }) => name?.toLowerCase() === "content-type");
        if (hasHeader) {
            return hasHeader.values![0];
        }
        return " " + part.mime + (part.charset ? "; charset=" + part.charset : "");
    }
}
