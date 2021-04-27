import RemoveMixin from "../RemoveMixin";

import { mapActions, mapGetters, mapState } from "vuex";
jest.mock("vuex");
mapActions.mockReturnValue({});
mapGetters.mockReturnValue({});
mapState.mockReturnValue({});

describe("RemoveMixin", () => {
    beforeAll(() => {
        RemoveMixin.$_RemoveMixin_move = jest.fn();
        RemoveMixin.$_RemoveMixin_remove = jest.fn();
        RemoveMixin.$router = { navigate: jest.fn() };
        RemoveMixin.$bvModal = { msgBoxConfirm: jest.fn() };
        RemoveMixin.$tc = jest.fn();
        RemoveMixin.$t = jest.fn();
        RemoveMixin.$_RemoveMixin_trash = { key: "trash" };
        RemoveMixin.$_RemoveMixin_mailbox = {};
        RemoveMixin.$store = {
            getters: {
                "mail/NEXT_MESSAGE": "next",
                "mail/IS_ACTIVE_MESSAGE": jest.fn().mockReturnValue(false)
            }
        };
        RemoveMixin.REMOVE_MESSAGES = RemoveMixin.methods.REMOVE_MESSAGES;
        RemoveMixin.MOVE_MESSAGES_TO_TRASH = RemoveMixin.methods.MOVE_MESSAGES_TO_TRASH;
    });
    beforeEach(() => {
        RemoveMixin.$_RemoveMixin_move.mockClear();
        RemoveMixin.$_RemoveMixin_remove.mockClear();
        RemoveMixin.$router.navigate.mockClear();
        RemoveMixin.$bvModal.msgBoxConfirm.mockClear();
        RemoveMixin.$tc.mockClear();
        RemoveMixin.$t.mockClear();
        RemoveMixin.$store.getters["mail/IS_ACTIVE_MESSAGE"].mockClear();
    });

    test("REMOVE_MESSAGES to call call popup", async () => {
        const messages = [];
        await RemoveMixin.REMOVE_MESSAGES(messages);
        expect(RemoveMixin.$bvModal.msgBoxConfirm).toHaveBeenCalled();
    });
    test("REMOVE_MESSAGES to call remove action if popup is confirmed", async () => {
        const messages = [];
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.REMOVE_MESSAGES(messages);
        expect(RemoveMixin.$_RemoveMixin_remove).toHaveBeenCalledWith(messages);
    });

    test("REMOVE_MESSAGES not to call remove action if popup is not confirm", async () => {
        const messages = [];
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValue(false);
        await RemoveMixin.REMOVE_MESSAGES(messages);
        expect(RemoveMixin.$_RemoveMixin_move).not.toHaveBeenCalled();
    });

    test("MOVE_TO_TRASH to call move action if one of the messages is not in trash", async () => {
        const messages = [{ folderRef: { key: "trash" } }, { folderRef: { key: "not-trash" } }];
        await RemoveMixin.MOVE_MESSAGES_TO_TRASH(messages);
        expect(RemoveMixin.$_RemoveMixin_move).toHaveBeenCalledWith({
            folder: RemoveMixin.$_RemoveMixin_trash,
            messages
        });
    });
    test("MOVE_TO_TRASH to call remove action if all  messages are in trash", async () => {
        const messages = [{ folderRef: { key: "trash" } }, { folderRef: { key: "trash" } }];
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.MOVE_MESSAGES_TO_TRASH(messages);
        expect(RemoveMixin.$_RemoveMixin_remove).toHaveBeenCalledWith(messages);
    });

    test("MOVE_TO_TRASH or REMOVE_MESSAGES to call navigate if current message is removed", async () => {
        const messages = { key: "key", folderRef: { key: "no-trash" } };
        RemoveMixin.$store.getters["mail/IS_ACTIVE_MESSAGE"].mockReturnValue(true);
        await RemoveMixin.MOVE_MESSAGES_TO_TRASH(messages);
        expect(RemoveMixin.$router.navigate).toHaveBeenCalledWith({
            name: "v:mail:message",
            params: { message: "next" }
        });
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.REMOVE_MESSAGES(messages);
        expect(RemoveMixin.$router.navigate).toHaveBeenCalledWith({
            name: "v:mail:message",
            params: { message: "next" }
        });
    });

    test("MOVE_TO_TRASH or REMOVE_MESSAGES not to call navigate if current message is not the only message removed", async () => {
        const messages = [
            { key: "key", folderRef: { key: "no-trash" } },
            { key: "not-current", folderRef: { key: "no-trash" } }
        ];
        RemoveMixin.$store.getters["mail/IS_ACTIVE_MESSAGE"].mockReturnValue(true);
        await RemoveMixin.MOVE_MESSAGES_TO_TRASH(messages);
        expect(RemoveMixin.$router.navigate).not.toHaveBeenCalled();
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.REMOVE_MESSAGES(messages);
        expect(RemoveMixin.$router.navigate).not.toHaveBeenCalled();
    });

    test("MOVE_TO_TRASH or REMOVE_MESSAGES not to call navigate if moved message is not the current one", async () => {
        const messages = { key: "not-current", folderRef: { key: "no-trash" } };
        RemoveMixin.$store.getters["mail/IS_ACTIVE_MESSAGE"].mockReturnValue(false);
        await RemoveMixin.MOVE_MESSAGES_TO_TRASH(messages);
        expect(RemoveMixin.$router.navigate).not.toHaveBeenCalled();
        RemoveMixin.$bvModal.msgBoxConfirm.mockResolvedValueOnce(true);
        await RemoveMixin.REMOVE_MESSAGES(messages);
        expect(RemoveMixin.$router.navigate).not.toHaveBeenCalled();
    });
});
