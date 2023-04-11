import LuceneQueryParser from "lucene-query-parser";

const folderParser = (node, result) => {
    if (node.field && node.field === "in") {
        result.folder = node.term;
    }
    return undefined;
};
const deepParser = (node, result) => {
    if (node.field && node.field === "is") {
        result.deep = node.term === "deep";
    }
    return undefined;
};
const parsers = [folderParser, deepParser];

function parseQuery(expression) {
    let result = {};
    if (expression) {
        const rootNode = LuceneQueryParser.parse(expression);
        if (rootNode) {
            result.pattern = rootNode.left.term;
            const nodeFunction = node => parsers.forEach(parser => parser(node, result));
            walkLuceneTree(rootNode, nodeFunction);
        }
    }
    return result;
}

function isSameSearch(previousPattern, pattern, previousFolderKey, folderKey, isDeep, previousIsDeep) {
    const isSamePattern = pattern === previousPattern;
    const isSameFolder = folderKey === previousFolderKey;
    const isSameDepth = !!isDeep === !!previousIsDeep;
    const isAllFoldersAgain = !folderKey && !previousFolderKey;
    return isSamePattern && isSameDepth && (isSameFolder || isAllFoldersAgain);
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
