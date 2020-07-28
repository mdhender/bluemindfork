export function isSameSearch(state) {
    return (pattern, folder) => {
        const isSamePattern = pattern === state.search.pattern;
        const isSameFolder = folder && folder === state.search.searchFolder;
        const isAllFoldersAgain = !folder && !state.search.searchFolder;
        return isSamePattern && (isSameFolder || isAllFoldersAgain);
    };
}
