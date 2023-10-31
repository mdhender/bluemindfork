import { FileStatus } from "../file";
import { create, isAttachment } from "../attachment";

export default class GetAttachmentPartsVisitor {
    constructor() {
        this.results = [];
    }

    visit(part) {
        if (isAttachment(part)) {
            const attachment = create(part, FileStatus.UPLOADED);
            this.results.push(attachment);
        }
    }

    result() {
        return this.results;
    }
}
