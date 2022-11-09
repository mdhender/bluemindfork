import MoveMixin from "../MoveMixin";

import { mapActions, mapGetters, mapState } from "vuex";
jest.mock("vuex");
mapActions.mockReturnValue({});
mapGetters.mockReturnValue({});
mapState.mockReturnValue({});

jest.mock("postal-mime", () => ({ TextEncoder: jest.fn() }));

describe("MoveMixin", () => {
    beforeAll(() => {
        MoveMixin.$_MoveMixin_move = jest.fn();
        MoveMixin.$_MoveMixin_moveConversations = jest.fn();
        MoveMixin.$_MoveMixin_create = jest.fn();
        MoveMixin.$router = { navigate: jest.fn() };
        MoveMixin.$_MoveMixin_folders = {
            key: { key: "key", name: "foldername" },
            key2: { key: "key2", name: "foldername2" }
        };
        MoveMixin.$_MoveMixin_mailbox = {};
        MoveMixin.$store = {
            getters: {
                "mail/NEXT_CONVERSATION": jest.fn(() => ({ key: "nextKey", messages: ["m1", "m2"] })),
                "mail/IS_CURRENT_CONVERSATION": jest.fn().mockReturnValue(false)
            }
        };
        MoveMixin.MOVE_CONVERSATIONS = MoveMixin.methods.MOVE_CONVERSATIONS;
        MoveMixin.navigateTo = jest.fn();
    });
    let conversations;
    let folder;
    beforeEach(() => {
        MoveMixin.$_MoveMixin_move.mockClear();
        MoveMixin.$_MoveMixin_create.mockClear();
        MoveMixin.$router.navigate.mockClear();
        MoveMixin.$store.getters["mail/IS_CURRENT_CONVERSATION"].mockClear();
        conversations = [{ key: "conversation", folderRef: { key: "key" } }];
        folder = MoveMixin.$_MoveMixin_folders["key2"];
    });

    test("MOVE_CONVERSATIONS to call moveConversations action", async () => {
        MoveMixin.$store.getters["mail/CONVERSATIONS_ACTIVATED"] = true;
        await MoveMixin.MOVE_CONVERSATIONS({ conversations, folder: folder });
        expect(MoveMixin.$_MoveMixin_moveConversations).toHaveBeenCalledWith({
            conversations,
            conversationsActivated: true,
            folder,
            mailbox: {}
        });
    });

    test("MOVE_CONVERSATIONS not to call create action if folder exist", async () => {
        await MoveMixin.MOVE_CONVERSATIONS({ conversations, folder: folder });
        expect(MoveMixin.$_MoveMixin_create).not.toHaveBeenCalled();
    });
    test("MOVE_CONVERSATIONS to call create action if folder does not exist", async () => {
        const folder = { name: "toto" };
        await MoveMixin.MOVE_CONVERSATIONS({ conversations, folder: folder });
        expect(MoveMixin.$_MoveMixin_create).toHaveBeenCalledWith({
            mailbox: MoveMixin.$_MoveMixin_mailbox,
            ...folder
        });
    });
    test("MOVE_CONVERSATIONS to call navigate if current conversation is moved", async () => {
        MoveMixin.$store.getters["mail/IS_CURRENT_CONVERSATION"].mockReturnValue(true);
        await MoveMixin.MOVE_CONVERSATIONS({ conversations, folder: {} });
        const next = { key: "nextKey", messages: ["m1", "m2"] };
        expect(MoveMixin.navigateTo).toHaveBeenCalledWith(next, conversations[0].folderRef);
    });
});
