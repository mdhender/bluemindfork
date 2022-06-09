import { message } from "@bluemind/mail";

const {
    MessageForwardAttributeSeparator,
    MessageReplyAttributeSeparator,
    MessageQuoteClasses,
    MessageQuoteOutlookId
} = message;

export default {
    findQuoteNodesUsingSeparator(messageParts) {
        const quoteNodesByPartAddress = {};
        Object.keys(messageParts).forEach(partAddress => {
            const partDoc = new DOMParser().parseFromString(messageParts[partAddress], "text/html");
            const quoteNode = findReplyOrForwardContentNodeNotInsideBlockquote(partDoc);
            quoteNodesByPartAddress[partAddress] = quoteNode ? [quoteNode] : "NOT_FOUND";
        });
        return quoteNodesByPartAddress;
    },

    /**
     * @returns the nodes in 'html' containing 'quote' and wrapping elements ('blockquote' tag, 'has wrote' header...).
     */
    findQuoteNodesUsingTextComparison(html, quote) {
        const rootDocument = new DOMParser().parseFromString(html, "text/html");
        const quoteElement = new DOMParser().parseFromString(quote, "text/html").body;
        const nodeWithQuoteInRootNode = findNodeContainingTextNodes(rootDocument, quoteElement);
        if (nodeWithQuoteInRootNode) {
            const nodeIterator = rootDocument.createNodeIterator(rootDocument.body, NodeFilter.SHOW_ALL, node =>
                node.isEqualNode(nodeWithQuoteInRootNode) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT
            );
            const quoteNode = nodeIterator.nextNode(); // assumes the first one is the only one needed
            const quoteNodes = addQuoteHeaderAndWrappingElements(quoteNode);
            return quoteNodes;
        }
    },

    removeQuotes(rootNode, quoteNodes) {
        if (quoteNodes) {
            const nodeIterator = rootNode.createNodeIterator(rootNode.body, NodeFilter.SHOW_ALL, node =>
                quoteNodes.some(qn => node.isEqualNode(qn)) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT
            );
            let node;
            while ((node = nodeIterator.nextNode())) {
                node.parentNode.removeChild(node);
            }
        }
        return rootNode;
    }
};

const SEP_IDS = [MessageReplyAttributeSeparator, MessageForwardAttributeSeparator, MessageQuoteOutlookId];
const SEP_CLASSES = MessageQuoteClasses;

function findReplyOrForwardContentNodeNotInsideBlockquote(partDoc) {
    // xpath example: //*[(@id="data-bm-reply-separator" or @id="data-bm-forward-separator" or contains(class,"data-bm-reply-separator") or contains(class,"data-bm-forward-separator")) and not(ancestor::blockquote)]
    const sepIdXpath = SEP_IDS.length ? SEP_IDS.map(sid => '@id="' + sid + '"').join(" or ") : "";
    const sepClassXpath = SEP_CLASSES.length
        ? " or " + SEP_CLASSES.map(sc => 'contains(class,"' + sc + '")').join(" or ")
        : "";
    const xpath = `//*[(${sepIdXpath}${sepClassXpath}) and not(ancestor::blockquote)]`;
    const xpathResult = partDoc?.evaluate(xpath, partDoc.body, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
    return xpathResult?.iterateNext();
}

function addQuoteHeaderAndWrappingElements(quoteNode) {
    let blockquoteNode = quoteNode.tagName === "BLOCKQUOTE" ? quoteNode : undefined;
    let currentNode = quoteNode;
    while (!blockquoteNode && currentNode) {
        if (currentNode.parentElement && currentNode.parentElement.tagName === "BLOCKQUOTE") {
            blockquoteNode = currentNode.parentNode;
        }
        currentNode = currentNode.parentNode;
    }
    return blockquoteNode ? [findQuoteHeaderFromBlockquote(blockquoteNode), blockquoteNode] : undefined;
}

function findQuoteHeaderFromBlockquote(blockquoteNode) {
    let quoteHeaderNode;
    let node = blockquoteNode.previousSibling;
    while (!quoteHeaderNode && node) {
        if (node.textContent && node.textContent.replace(/\s+/, "").length) {
            quoteHeaderNode = node;
        }
        node = node.previousSibling;
    }
    return quoteHeaderNode;
}

function findNodeContainingTextNodes(rootDocument, nodeWithTextNodesToMatch) {
    const spacesRegex = /\s|\n|&nbsp;/g;
    const textToMatch = nodeWithTextNodesToMatch.innerText.replace(spacesRegex, "");

    let deepestMatchingNode;
    let deepestMatchingNodeDepth = 0;
    const nodeIterator = rootDocument.createNodeIterator(
        rootDocument.body,
        NodeFilter.SHOW_ALL,
        () => NodeFilter.FILTER_ACCEPT
    );
    let node;
    while ((node = nodeIterator.nextNode())) {
        node.depth = node?.parentNode.depth + 1 || 0;
        if (
            node.innerText &&
            node.innerText.replace(spacesRegex, "").includes(textToMatch) &&
            node.depth > deepestMatchingNodeDepth
        ) {
            deepestMatchingNode = node;
        }
    }
    return deepestMatchingNode;
}
