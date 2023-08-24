import TreeWalker, { GetLeafsVisitor } from "@bluemind/mime-tree-walker";
import { isAttachment } from "../attachment";
import { isLeaf, isViewable } from "../part";
import GetAttachmentPartsVisitor from "./GetAttachmentPartsVisitor";
import GetInlinePartsVisitor from "./GetInlinePartsVisitor";
import GetReportPartsVisitor from "./GetReportPartsVisitor";

export function computeParts(structure) {
    if (!structure || !Object.values(structure).length) {
        return {
            attachments: [],
            inlinePartsByCapabilities: []
        };
    }
    const inlineVisitor = new GetInlinePartsVisitor();
    const attachmentVisitor = new GetAttachmentPartsVisitor();

    const walker = new TreeWalker(structure, [inlineVisitor, attachmentVisitor]);
    walker.walk();
    return {
        attachments: attachmentVisitor.result(),
        inlinePartsByCapabilities: inlineVisitor.result()
    };
}

export function hasAttachment(node) {
    if (isAttachment(node) || (isLeaf(node) && !isViewable(node))) {
        return true;
    }
    return node.children?.some(hasAttachment);
}

export function getReportsParts(structure) {
    const reportVisitor = new GetReportPartsVisitor();
    const walker = new TreeWalker(structure, [reportVisitor]);
    walker.walk();
    return reportVisitor.result();
}

export function getLeafParts(structure) {
    const visitor = new GetLeafsVisitor();
    const walker = new TreeWalker(structure, [visitor]);
    walker.walk();
    return visitor.result();
}
