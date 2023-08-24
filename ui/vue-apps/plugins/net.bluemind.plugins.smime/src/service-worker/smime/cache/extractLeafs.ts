import TreeWalker, { GetLeafsVisitor } from "@bluemind/mime-tree-walker";
import { MessageBody } from "@bluemind/backend.mail.api";

export default function extractLeafs(structure: MessageBody.Part) {
    const visitor = new GetLeafsVisitor();
    new TreeWalker(structure, [visitor]).walk();
    return visitor.result();
}
