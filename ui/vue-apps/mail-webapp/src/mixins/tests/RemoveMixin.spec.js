import RemoveMixin from "../RemoveMixin";

import { mapActions, mapGetters, mapState } from "vuex";
jest.mock("vuex");
mapActions.mockReturnValue({});
mapGetters.mockReturnValue({});
mapState.mockReturnValue({});

describe("RemoveMixin", () => {
    beforeAll(() => {
        RemoveMixin.$_RemoveMixin_move = jest.fn();
        RemoveMixin.$_RemoveMixin_moveConversationsToTrash = jest.fn();
        RemoveMixin.$_RemoveMixin_remove = jest.fn();
        RemoveMixin.$_RemoveMixin_removeConversations = jest.fn();
        RemoveMixin.$router = { navigate: jest.fn() };
        RemoveMixin.$bvModal = { msgBoxConfirm: jest.fn() };
        RemoveMixin.$tc = jest.fn();
        RemoveMixin.$t = jest.fn();
        RemoveMixin.$_RemoveMixin_trash = { key: "trash" };
        RemoveMixin.$_RemoveMixin_mailbox = {};
        RemoveMixin.$store = {
            getters: {
                "mail/NEXT_CONVERSATION": "next",
                "mail/IS_CURRENT_CONVERSATION": jest.fn().mockReturnValue(false)
            }
        };
        RemoveMixin.MOVE_CONVERSATIONS_TO_TRASH = RemoveMixin.methods.MOVE_CONVERSATIONS_TO_TRASH;
        RemoveMixin.REMOVE_CONVERSATIONS = RemoveMixin.methods.REMOVE_CONVERSATIONS;
        RemoveMixin.REMOVE_MESSAGES = RemoveMixin.methods.REMOVE_MESSAGES;
    });
    beforeEach(() => {
        RemoveMixin.$_RemoveMixin_move.mockClear();
        RemoveMixin.$_RemoveMixin_moveConversationsToTrash.mockClear();
        RemoveMixin.$_RemoveMixin_remove.mockClear();
        RemoveMixin.$_RemoveMixin_removeConversations.mockClear();
        RemoveMixin.$router.navigate.mockClear();
        RemoveMixin.$bvModal.msgBoxConfirm.mockClear();
        RemoveMixin.$tc.mockClear();
        RemoveMixin.$t.mockClear();
        RemoveMixin.$store.getters["mail/IS_CURRENT_CONVERSATION"].mockClear();
    });

    test("REMOVE_MESSAGES to call popup", async () => {
        const conversation = { key: "conversation" };
        const messages = [];
        await RemoveMixin.REMOVE_MESSAGES({ conversation, messages });
        expect(RemoveMixin.$bvModal.msgBoxConfirm).toHaveBeenCalled();
    });
    test("REMOVE_MESSAGES to call remove action if popup is confirmed", async () => {
        const conversation = { key: "conversation" };
        const messages = [];
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.REMOVE_MESSAGES({ conversation, messages });
        expect(RemoveMixin.$_RemoveMixin_remove).toHaveBeenCalledWith({ conversation, messages });
    });

    test("REMOVE_MESSAGES not to call remove action if popup is not confirm", async () => {
        const conversation = { key: "conversation" };
        const messages = [];
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValue(false);
        await RemoveMixin.REMOVE_MESSAGES({ conversation, messages });
        expect(RemoveMixin.$_RemoveMixin_move).not.toHaveBeenCalled();
    });

    test("MOVE_CONVERSATIONS_TO_TRASH to call move action if one of the messages is not in trash", async () => {
        const conversations = [{ folderRef: { key: "trash" } }, { folderRef: { key: "not-trash" } }];
        await RemoveMixin.MOVE_CONVERSATIONS_TO_TRASH(conversations);
        expect(RemoveMixin.$_RemoveMixin_moveConversationsToTrash).toHaveBeenCalledWith({
            folder: RemoveMixin.$_RemoveMixin_trash,
            conversations
        });
    });
    test("MOVE_CONVERSATIONS_TO_TRASH to call remove action if all messages are in trash", async () => {
        const conversations = [
            { messages: [], folderRef: { key: "trash" } },
            { messages: [], folderRef: { key: "trash" } }
        ];
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.MOVE_CONVERSATIONS_TO_TRASH(conversations);
        expect(RemoveMixin.$_RemoveMixin_removeConversations).toHaveBeenCalledWith({ conversations });
    });

    test("MOVE_CONVERSATIONS_TO_TRASH or REMOVE_CONVERSATIONS to call navigate if current message is removed", async () => {
        const conversations = [{ key: "key", messages: [], folderRef: { key: "no-trash" } }];
        RemoveMixin.$store.getters["mail/IS_CURRENT_CONVERSATION"].mockReturnValue(true);
        await RemoveMixin.MOVE_CONVERSATIONS_TO_TRASH(conversations);
        expect(RemoveMixin.$router.navigate).toHaveBeenCalledWith({
            name: "v:mail:conversation",
            params: { conversation: "next" }
        });
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.REMOVE_CONVERSATIONS(conversations);
        expect(RemoveMixin.$router.navigate).toHaveBeenCalledWith({
            name: "v:mail:conversation",
            params: { conversation: "next" }
        });
    });

    test("MOVE_CONVERSATIONS_TO_TRASH or REMOVE_CONVERSATIONS not to call navigate if current message is not the only message removed", async () => {
        const conversations = [
            { key: "key", messages: [], folderRef: { key: "no-trash" } },
            { key: "not-current", messages: [], folderRef: { key: "no-trash" } }
        ];
        RemoveMixin.$store.getters["mail/IS_CURRENT_CONVERSATION"].mockReturnValue(true);
        await RemoveMixin.MOVE_CONVERSATIONS_TO_TRASH(conversations);
        expect(RemoveMixin.$router.navigate).not.toHaveBeenCalled();
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.REMOVE_CONVERSATIONS(conversations);
        expect(RemoveMixin.$router.navigate).not.toHaveBeenCalled();
    });

    test("MOVE_CONVERSATIONS_TO_TRASH or REMOVE_CONVERSATIONS not to call navigate if moved message is not the current one", async () => {
        const conversations = [{ key: "not-current", messages: [], folderRef: { key: "no-trash" } }];
        RemoveMixin.$store.getters["mail/IS_CURRENT_CONVERSATION"].mockReturnValue(false);
        await RemoveMixin.MOVE_CONVERSATIONS_TO_TRASH(conversations);
        expect(RemoveMixin.$router.navigate).not.toHaveBeenCalled();
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.REMOVE_CONVERSATIONS(conversations);
        expect(RemoveMixin.$router.navigate).not.toHaveBeenCalled();
    });
});
