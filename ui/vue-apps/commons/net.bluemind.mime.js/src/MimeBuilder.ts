import Builder from "@bluemind/emailjs-mime-builder";
import { MessageBody } from "@bluemind/backend.mail.api";

export default class MimeBuilder {
    getContent: (part: MessageBody.Part) => Promise<Uint8Array | null>;

    constructor(getContent: (part: MessageBody.Part) => Promise<Uint8Array | null>) {
        this.getContent = getContent;
    }

    async build(part: MessageBody.Part): Promise<string | undefined> {
        return (await this.appendPart(part))?.build();
    }
    private async appendPart(part: MessageBody.Part): Promise<Builder | undefined> {
        if (part.mime) {
            const node = new Builder(part.mime);
            if (part.mime.startsWith("multipart/") && part.children) {
                for (const child of part.children) {
                    const newNode = await this.appendPart(child);
                    if (newNode) {
                        node.appendChild(newNode);
                    }
                }
            } else {
                const content = await this.getContent(part);
                if (content) {
                    node.setContent(content);
                }

                if (part.dispositionType) {
                    if (part.dispositionType === "ATTACHMENT" && part.fileName) {
                        node.setHeader("Content-Disposition", `attachment; filename="${part.fileName}`);
                    } else {
                        node.setHeader("Content-Disposition", part.dispositionType.toLowerCase());
                    }
                }
                if (part.contentId) {
                    node.setHeader("Content-ID", part.contentId);
                }
            }
            return node;
        }
        throw new Error("Malformed item");
    }
}
