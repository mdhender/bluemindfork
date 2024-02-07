import { LineElement } from "./LineElement";

type htmlStringContent = string;

export class PersonnalSignatureHtml {
    private node: HTMLElement;
    constructor(private signature: htmlStringContent) {
        this.node = this.extractHtmlNodeContainingSignature(signature);
    }

    /** Remove leading and trailing empty lines. */
    public trim() {
        if (!this.signature?.length) {
            return;
        }
        return this.trimSignatureHtmlNode() ?? this.signature;
    }

    /** We ignore all nodes that are just wrappers without content (real content that with actual signature) */
    private extractHtmlNodeContainingSignature(signature: htmlStringContent) {
        let node = new DOMParser().parseFromString(signature, "text/html")?.body;
        while (
            node.childElementCount === 1 &&
            node.children[0].hasChildNodes() &&
            Array.from(node.children[0].childNodes).some(c => c.nodeType === Node.ELEMENT_NODE)
        ) {
            node = node.children[0] as HTMLElement;
        }
        return node;
    }

    private trimSignatureHtmlNode() {
        const lines = this.node && Array.from(this.node.childNodes);
        if (lines) {
            this.removeEmptyLines(lines);
            return this.node.innerHTML;
        }
    }

    private removeEmptyLines(lines: ChildNode[]) {
        this.removeLeadingEmptyLines(lines);
        if (this.node.hasChildNodes()) {
            this.removeTrailingEmptyLines(lines);
        }
    }

    private removeTrailingEmptyLines(lines: ChildNode[]) {
        this.removeLeadingEmptyLines(lines.reverse());
    }

    private removeLeadingEmptyLines(lines: ChildNode[]) {
        for (const line of lines) {
            if (this.isSignatureLineEmpty(line)) {
                this.node.removeChild(line);
            } else {
                break;
            }
        }
    }

    private isSignatureLineEmpty(node: ChildNode) {
        return new LineElement(node).isEmpty;
    }
}
