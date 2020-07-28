import LuceneQueryParser from "lucene-query-parser";

function parseQuery(expression) {
    let pattern, folder;
    if (expression) {
        const rootNode = LuceneQueryParser.parse(expression);
        if (rootNode) {
            pattern = rootNode.left.term;
            const nodeFunction = node => (node.field && node.field === "in" ? (folder = node.term) : undefined);
            walkLuceneTree(rootNode, nodeFunction);
        }
    }
    return { pattern, folder };
}

function isSameSearch(previousPattern, previousFolderKey, pattern, folderKey) {
    const isSamePattern = pattern === previousPattern;
    const isSameFolder = folderKey && folderKey === previousFolderKey;
    const isAllFoldersAgain = !folderKey && !previousFolderKey;
    return isSamePattern && (isSameFolder || isAllFoldersAgain);
}

function walkLuceneTree(node, nodeFunction) {
    nodeFunction(node);
    if (node.left) {
        walkLuceneTree(node.left, nodeFunction);
        if (node.right) {
            walkLuceneTree(node.right, nodeFunction);
        }
    }
}

export const SearchHelper = { parseQuery, isSameSearch };
export default SearchHelper;
