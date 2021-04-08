import MoveMixin from "../MoveMixin";

import { mapActions, mapGetters, mapState } from "vuex";
jest.mock("vuex");
mapActions.mockReturnValue({});
mapGetters.mockReturnValue({});
mapState.mockReturnValue({});

describe("MoveMixin", () => {
    beforeAll(() => {
        MoveMixin.$_MoveMixin_move = jest.fn();
        MoveMixin.$_MoveMixin_create = jest.fn();
        MoveMixin.$router = { navigate: jest.fn() };
        MoveMixin.$_MoveMixin_folders = { key: { key: "key", name: "foldername" } };
        MoveMixin.$_MoveMixin_mailbox = {};
        MoveMixin.$store = {
            state: { mail: { messages: { next: "next" } } },
            getters: {
                "mail-webapp/nextMessageKey": "next",
                "mail/IS_CURRENT_MESSAGE": jest.fn().mockReturnValue(false)
            }
        };
        MoveMixin.MOVE_MESSAGES = MoveMixin.methods.MOVE_MESSAGES;
    });
    beforeEach(() => {
        MoveMixin.$_MoveMixin_move.mockClear();
        MoveMixin.$_MoveMixin_create.mockClear();
        MoveMixin.$router.navigate.mockClear();
        MoveMixin.$store.getters["mail/IS_CURRENT_MESSAGE"].mockClear();
    });

    test("MOVE_MESSAGES to call move action", async () => {
        const messages = [{ key: "message" }];
        const folder = MoveMixin.$_MoveMixin_folders["key"];
        MoveMixin.MOVE_MESSAGES({ messages, folder });
        expect(MoveMixin.$_MoveMixin_move).toHaveBeenCalledWith({ folder, messages });
    });

    test("MOVE_MESSAGES  not to call create action if folder exist", () => {
        const messages = [{ key: "message" }];
        const folder = MoveMixin.$_MoveMixin_folders["key"];
        MoveMixin.MOVE_MESSAGES({ messages, folder });
        expect(MoveMixin.$_MoveMixin_create).not.toHaveBeenCalled();
    });
    test("MOVE_MESSAGES to call create action if folder does not exist", () => {
        const messages = [{ key: "message" }];
        const folder = { name: "toto" };
        MoveMixin.MOVE_MESSAGES({ messages, folder });
        expect(MoveMixin.$_MoveMixin_create).toHaveBeenCalledWith({
            mailbox: MoveMixin.$_MoveMixin_mailbox,
            ...folder
        });
    });

    test("MOVE_MESSAGES to call navigate is current message is moved", async () => {
        const messages = { key: "key" };
        const folder = {};
        MoveMixin.$store.getters["mail/IS_CURRENT_MESSAGE"].mockReturnValue(true);
        MoveMixin.MOVE_MESSAGES({ messages, folder });
        expect(MoveMixin.$router.navigate).toHaveBeenCalledWith({
            name: "v:mail:message",
            params: { message: "next" }
        });
    });
    test("MOVE_MESSAGES not to call navigate is current message is not the only moved message", async () => {
        let messages = [{ key: "key" }, { key: "another" }];
        const folder = {};
        MoveMixin.$store.getters["mail/IS_CURRENT_MESSAGE"].mockReturnValue(true);
        MoveMixin.MOVE_MESSAGES({ messages, folder });
        expect(MoveMixin.$router.navigate).not.toHaveBeenCalledWith();
        messages = { key: "another" };
        MoveMixin.MOVE_MESSAGES({ messages, folder });
        expect(MoveMixin.$router.navigate).not.toHaveBeenCalledWith();
    });
});
