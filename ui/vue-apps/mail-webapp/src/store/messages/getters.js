function activeFilterToFunction(ACTIVE_FLAG = {}) {
    return message => {
        if (message.data && message.data.flags) {
            return Object.entries(ACTIVE_FLAG).reduce(
                (match, [key, value]) => match && message.data.flags[key] === value,
                true
            );
        }
    };
}

export const ACTIVE_MESSAGES = "ACTIVE_MESSAGES";
const activeMessages = function activeMessages(state, { ACTIVE_FOLDER }) {
    return Object.entries(state)
        .map(([, message]) => message)
        .filter(message => message.folder === ACTIVE_FOLDER);
};

export const FILTERED_MESSAGES_BY_FLAG = "FILTERED_MESSAGES_BY_FLAG";
const filteredMessagesByFlag = function filteredMessages(state, { ACTIVE_FLAG }) {
    const activeFilter = activeFilterToFunction(ACTIVE_FLAG);
    return Object.entries(state)
        .map(([, message]) => message)
        .filter(activeFilter);
};

export default {
    [ACTIVE_MESSAGES]: activeMessages,
    [FILTERED_MESSAGES_BY_FLAG]: filteredMessagesByFlag
};

//TODO: voici les getters qui sont utilisées ci et là
// const count = state => {};
// const getMessagesByKey = (state, { messages }) => {};
// const getPartContent = state => {};
// const messages = state => {};
