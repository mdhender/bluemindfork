import api from "../api/apiMessages";
import {
    ADD_MESSAGES,
    MARK_AS_READ as MUTATION_MARK_AS_READ,
    MARK_AS_UNREAD as MUTATION_MARK_AS_UNREAD
} from "./mutations";

export const FETCH_MESSAGES = "FETCH_MESSAGES";
const fetchMessages = async ({ commit }, { folderUid }) => {
    try {
        const messages = await api.fetchMessages({ folderUid });
        commit(
            ADD_MESSAGES,
            messages.map(message => ({
                ...message,
                key: message.uid,
                folder: folderUid
            }))
        );
    } catch (error) {
        console.error("unable to fetch messages", error);
    }
};

export const MARK_AS_READ = "MARK_AS_READ";
const markAsRead = async ({ commit }, { key, folder }) => {
    commit(MUTATION_MARK_AS_READ, { key });
    try {
        await api.markAsRead({ uid: folder }, { key });
    } catch (error) {
        commit(MUTATION_MARK_AS_UNREAD, { key });
    }
};

export default {
    [FETCH_MESSAGES]: fetchMessages,
    [MARK_AS_READ]: markAsRead
};
