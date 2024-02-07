export class LineElement {
    constructor(private node: Node) { }

    get isEmpty() {
        if (
            !this.node ||
            (this.node.nodeType !== Node.ELEMENT_NODE &&
                this.node.nodeType === Node.TEXT_NODE &&
                !this.node.textContent)
        ) {
            return true;
        }

        return !this.node.textContent?.trim() && !this.containsImage();
    }

    private containsImage() {
        return (
            isElementNode(this.node) &&
            ((this.node.tagName === "IMG" && this.node.getAttribute("src")) ||
                this.node?.querySelector?.("img[src]:not([src=''])") ||
                this.node.style?.getPropertyValue?.("background-image") ||
                this.node
                    ?.querySelector<HTMLDivElement>("[style*='background-image']")
                    ?.style.getPropertyValue("background-image"))
        );
    }
}

function isElementNode(node: Node): node is HTMLElement {
    return node.nodeType === Node.ELEMENT_NODE;
}
