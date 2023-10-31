import { MessageBody } from "@bluemind/backend.mail.api";
import { MimeType } from "@bluemind/email";
import Visitor from "./Visitor";

export default class GetLeafsVisitor implements Visitor {
    results: MessageBody.Part[];
    constructor() {
        this.results = [];
    }
    visit(part: MessageBody.Part) {
        if (isLeaf(part)) {
            this.results.push(part);
        }
    }
    result() {
        return this.results;
    }
}

function isLeaf(part: MessageBody.Part) {
    return (!part.children || part.children.length === 0) && !MimeType.isMultipart(part);
}
