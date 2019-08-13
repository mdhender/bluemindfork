import PartsHelper from "./PartsHelper";

export default class GetAttachmentPartsVisitor {
    constructor() {
        this.results = [];
    }

    visit(part) {
        if (PartsHelper.isAttachment(part)) {
            this.results.push({
                address: part.address,
                filename: part.fileName,
                encoding: part.encoding,
                mime: part.mime,
                size: part.size
            });
        }
    }

    result() {
        return this.results;
    }
}
