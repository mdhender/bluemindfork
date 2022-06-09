import { attachment } from "@bluemind/mail";

const { AttachmentStatus, create, isAttachment } = attachment;

export default class GetAttachmentPartsVisitor {
    constructor() {
        this.results = [];
    }

    visit(part) {
        if (isAttachment(part)) {
            const attachment = create(part, AttachmentStatus.UPLOADED);
            this.results.push(attachment);
        }
    }

    result() {
        return this.results;
    }
}
