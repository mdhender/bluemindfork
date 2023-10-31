export const MOVABLE_CONTENT_DROP_ID = "moving-content";

export default function (node, tooltip, placeholder) {
    if (placeholder) {
        node.setAttribute("style", "user-select: none; cursor: grab; cursor: -webkit-grab;");
        node.setAttribute("draggable", "true");
        node.addEventListener("dragstart", event => {
            event.dataTransfer.dropEffect = "move";
            event.dataTransfer.effectAllowed = "move";
            event.dataTransfer.setData(MOVABLE_CONTENT_DROP_ID, "true");
        });
    } else {
        node.setAttribute("style", "user-select: none; cursor: not-allowed;");
    }
    node.setAttribute("contenteditable", "false");
    node.setAttribute("title", tooltip);
    node.querySelectorAll("img, a").forEach(imgNode => {
        imgNode.setAttribute("draggable", "false");
    });
    node.querySelectorAll("a").forEach(imgNode => {
        imgNode.setAttribute("target", "_blank");
    });

    // for accessibility purpose
    node.setAttribute("tabindex", "0");
    node.addEventListener("keydown", event => {
        event.preventDefault(); // avoid scrolling
        if (event.keyCode === 38) {
            // arrow up
            if (node.previousSibling) {
                if (node.previousSibling.nodeName === "DIV" && node.previousSibling.childNodes.length > 0) {
                    node.previousElementSibling.appendChild(node);
                } else {
                    node.previousSibling.before(node);
                }
            } else if (node.parentElement?.previousElementSibling) {
                node.parentElement.previousElementSibling.appendChild(node);
            }
            node.focus();
        } else if (event.keyCode === 40) {
            // arrow down
            if (node.nextSibling) {
                if (node.nextSibling.nodeName === "DIV" && node.nextSibling.childNodes.length > 0) {
                    node.nextSibling.prepend(node);
                } else {
                    node.nextSibling.after(node);
                }
            } else if (node.parentElement?.nextElementSibling) {
                node.parentElement.nextElementSibling.prepend(node);
            }
            node.focus();
        }
    });
    return node;
}
