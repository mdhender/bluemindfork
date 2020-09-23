import { PartsHelper } from "@bluemind/email";

import { create } from "../../model/attachment";

export default class GetAttachmentPartsVisitor {
    constructor() {
        this.results = [];
    }

    visit(part) {
        if (PartsHelper.isAttachment(part)) {
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
