import { attachmentUtils, fileUtils } from "@bluemind/mail";

const { create, isAttachment } = attachmentUtils;
const { FileStatus } = fileUtils;

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
