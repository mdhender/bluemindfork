import MoveMixin from "../MoveMixin";

import { mapActions, mapGetters, mapState } from "vuex";
jest.mock("vuex");
mapActions.mockReturnValue({});
mapGetters.mockReturnValue({});
mapState.mockReturnValue({});

describe("MoveMixin", () => {
    beforeAll(() => {
        MoveMixin.$_MoveMixin_move = jest.fn();
        MoveMixin.$_MoveMixin_moveConversations = jest.fn();
        MoveMixin.$_MoveMixin_create = jest.fn();
        MoveMixin.$router = { navigate: jest.fn() };
        MoveMixin.$_MoveMixin_folders = { key: { key: "key", name: "foldername" } };
        MoveMixin.$_MoveMixin_mailbox = {};
        MoveMixin.$store = {
            getters: {
                "mail/NEXT_CONVERSATION": jest.fn(() => ({ key: "nextKey", messages: ["m1", "m2"] })),
                "mail/IS_CURRENT_CONVERSATION": jest.fn().mockReturnValue(false)
            }
        };
        MoveMixin.MOVE_CONVERSATIONS = MoveMixin.methods.MOVE_CONVERSATIONS;
    });
    beforeEach(() => {
        MoveMixin.$_MoveMixin_move.mockClear();
        MoveMixin.$_MoveMixin_create.mockClear();
        MoveMixin.$router.navigate.mockClear();
        MoveMixin.$store.getters["mail/IS_CURRENT_CONVERSATION"].mockClear();
    });

    test("MOVE_CONVERSATIONS to call moveConversations action", async () => {
        const conversations = [{ key: "message" }];
        const folder = MoveMixin.$_MoveMixin_folders["key"];
        MoveMixin.MOVE_CONVERSATIONS({ conversations, folder });
        expect(MoveMixin.$_MoveMixin_moveConversations).toHaveBeenCalledWith({ conversations, folder });
    });

    test("MOVE_CONVERSATIONS not to call create action if folder exist", () => {
        const conversations = [{ key: "message" }];
        const folder = MoveMixin.$_MoveMixin_folders["key"];
        MoveMixin.MOVE_CONVERSATIONS({ conversations, folder });
        expect(MoveMixin.$_MoveMixin_create).not.toHaveBeenCalled();
    });
    test("MOVE_CONVERSATIONS to call create action if folder does not exist", () => {
        const conversations = [{ key: "message" }];
        const folder = { name: "toto" };
        MoveMixin.MOVE_CONVERSATIONS({ conversations, folder });
        expect(MoveMixin.$_MoveMixin_create).toHaveBeenCalledWith({
            mailbox: MoveMixin.$_MoveMixin_mailbox,
            ...folder
        });
    });
    test("MOVE_CONVERSATIONS to call navigate if current conversation is moved", async () => {
        const conversations = { key: "key", messages: ["k1", "k2"] };
        const folder = {};
        MoveMixin.$store.getters["mail/IS_CURRENT_CONVERSATION"].mockReturnValue(true);
        MoveMixin.MOVE_CONVERSATIONS({ conversations, folder });
        expect(MoveMixin.$router.navigate).toHaveBeenCalledWith({
            name: "v:mail:conversation",
            params: { conversation: { key: "nextKey", messages: ["m1", "m2"] } }
        });
    });
});
