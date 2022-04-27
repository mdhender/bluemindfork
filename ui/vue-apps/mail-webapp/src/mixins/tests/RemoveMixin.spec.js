import RemoveMixin from "../RemoveMixin";

import { mapActions, mapGetters, mapState } from "vuex";
import { MOVE_CONVERSATIONS, REMOVE_CONVERSATIONS, REMOVE_CONVERSATION_MESSAGES } from "~/actions";

jest.mock("vuex");
mapActions.mockReturnValue({});
mapGetters.mockReturnValue({});
mapState.mockReturnValue({});

describe("RemoveMixin", () => {
    beforeAll(() => {
        RemoveMixin.$router = { navigate: jest.fn() };
        RemoveMixin.$bvModal = { msgBoxConfirm: jest.fn() };
        RemoveMixin.$tc = jest.fn();
        RemoveMixin.$t = jest.fn();

        RemoveMixin.$_RemoveMixin_trash = { key: "trash" };
        RemoveMixin.$_RemoveMixin_mailbox = {};

        RemoveMixin.$store = {
            dispatch: jest.fn(() => Promise.resolve()),
            getters: {
                "mail/NEXT_CONVERSATION": jest.fn(() => ({ key: "nextKey", messages: ["m1", "m2"] })),
                "mail/IS_CURRENT_CONVERSATION": jest.fn().mockReturnValue(false),
                "mail/MAILBOX_TRASH": () => ({ key: "trash" }),
                "mail/CURRENT_MAILBOX": { key: "currentMailbox" }
            },
            state: {
                mail: {
                    folders: {
                        "no-trash": { key: "no-trash", mailboxRef: { key: "my" } },
                        trash: { key: "trash", mailboxRef: { key: "my" } }
                    },
                    mailboxes: { my: {} }
                }
            }
        };
        RemoveMixin.MOVE_CONVERSATIONS_TO_TRASH = RemoveMixin.methods.MOVE_CONVERSATIONS_TO_TRASH;
        RemoveMixin.REMOVE_CONVERSATIONS = RemoveMixin.methods.REMOVE_CONVERSATIONS;
        RemoveMixin.REMOVE_MESSAGES = RemoveMixin.methods.REMOVE_MESSAGES;
        RemoveMixin.navigateTo = jest.fn();
    });
    beforeEach(() => {
        RemoveMixin.$store.dispatch.mockClear();
        RemoveMixin.$router.navigate.mockClear();
        RemoveMixin.$bvModal.msgBoxConfirm.mockClear();
        RemoveMixin.$tc.mockClear();
        RemoveMixin.$t.mockClear();
        RemoveMixin.$store.getters["mail/IS_CURRENT_CONVERSATION"].mockClear();
    });

    test("REMOVE_MESSAGES to call popup", async () => {
        const messages = [];
        await RemoveMixin.REMOVE_MESSAGES({}, messages);
        expect(RemoveMixin.$bvModal.msgBoxConfirm).toHaveBeenCalled();
    });
    test("REMOVE_MESSAGES to call remove action if popup is confirmed", async () => {
        const conversation = { key: "conversation", folderRef: {}, messages: [] };
        const messages = [];
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.REMOVE_MESSAGES(conversation, messages);
        expect(RemoveMixin.$store.dispatch).toHaveBeenCalledWith(`mail/${REMOVE_CONVERSATION_MESSAGES}`, {
            conversation,
            messages
        });
    });

    test("REMOVE_MESSAGES not to call remove action if popup is not confirm", async () => {
        const messages = [];
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValue(false);
        await RemoveMixin.REMOVE_MESSAGES({}, messages);
        expect(RemoveMixin.$store.dispatch).not.toHaveBeenCalledWith();
    });

    test("MOVE_CONVERSATIONS_TO_TRASH to call move action if one of the messages is not in trash", async () => {
        const conversations = [{ folderRef: { key: "trash" } }, { folderRef: { key: "not-trash" } }];
        RemoveMixin.$store.getters["mail/CONVERSATIONS_ACTIVATED"] = true;
        await RemoveMixin.MOVE_CONVERSATIONS_TO_TRASH(conversations);
        expect(RemoveMixin.$store.dispatch).toHaveBeenCalledWith(`mail/${MOVE_CONVERSATIONS}`, {
            destinationFolder: RemoveMixin.$_RemoveMixin_trash,
            conversations: conversations,
            conversationsActivated: true,
            mailbox: { key: "currentMailbox" }
        });
    });
    test("MOVE_CONVERSATIONS_TO_TRASH to call remove action if all messages are in trash", async () => {
        const conversations = [
            { messages: [], folderRef: { key: "trash" } },
            { messages: [], folderRef: { key: "trash" } }
        ];
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        RemoveMixin.$store.getters["mail/CONVERSATIONS_ACTIVATED"] = true;

        await RemoveMixin.MOVE_CONVERSATIONS_TO_TRASH(conversations);
        expect(RemoveMixin.$store.dispatch).toHaveBeenCalledWith(`mail/${REMOVE_CONVERSATIONS}`, {
            conversations,
            conversationsActivated: true,
            mailbox: { key: "currentMailbox" }
        });
    });

    test("MOVE_CONVERSATIONS_TO_TRASH or REMOVE_CONVERSATIONS to call navigate if current message is removed", async () => {
        const conversations = [{ key: "key", messages: ["k1", "k2"], folderRef: { key: "no-trash" } }];
        RemoveMixin.$store.getters["mail/IS_CURRENT_CONVERSATION"].mockReturnValue(true);
        RemoveMixin.$store.getters["mail/CONVERSATIONS_ACTIVATED"] = true;

        await RemoveMixin.MOVE_CONVERSATIONS_TO_TRASH(conversations);
        const next = { key: "nextKey", messages: ["m1", "m2"] };
        expect(RemoveMixin.navigateTo).toHaveBeenCalledWith(next, conversations[0].folderRef);
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.REMOVE_CONVERSATIONS(conversations);
        expect(RemoveMixin.navigateTo).toHaveBeenCalledWith(next, conversations[0].folderRef);
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
