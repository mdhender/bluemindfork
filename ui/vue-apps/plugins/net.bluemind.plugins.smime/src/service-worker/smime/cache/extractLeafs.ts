import { MimeType } from "@bluemind/email";
import TreeWalker, { Visitor } from "@bluemind/mime-tree-walker";
import { MessageBody } from "@bluemind/backend.mail.api";

export default function extractLeafs(structure: MessageBody.Part) {
    const visitor = new GetLeafsVisitor();
    new TreeWalker(structure, [visitor]).walk();
    return visitor.result();
}

class GetLeafsVisitor implements Visitor {
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
