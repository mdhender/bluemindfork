import { create, isAttachment } from "~model/attachment";

export default class GetAttachmentPartsVisitor {
    constructor() {
        this.results = [];
    }

    visit(part) {
        if (isAttachment(part)) {
            const attachment = create(
                part.address,
                part.charset,
                part.fileName,
                part.encoding,
                part.mime,
                part.size,
                true
            );
            this.results.push(attachment);
        }
    }

    result() {
        return this.results;
    }
}
