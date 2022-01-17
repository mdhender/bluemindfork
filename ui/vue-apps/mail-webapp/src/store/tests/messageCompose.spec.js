import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import inject from "@bluemind/inject";
import { MockMailboxesClient } from "@bluemind/test-utils";
import storeOptions from "../messageCompose";
import { SET_DRAFT_COLLAPSED_CONTENT, SET_DRAFT_EDITOR_CONTENT, SET_MAX_MESSAGE_SIZE, SHOW_SENDER } from "~/mutations";
import { LOAD_MAX_MESSAGE_SIZE } from "~/actions";
import { IS_SENDER_SHOWN } from "~/getters";

Vue.use(Vuex);

describe("messageCompose", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(storeOptions));
    });

    describe("mutations", () => {
        test("SET_DRAFT_EDITOR_CONTENT", () => {
            store.commit(SET_DRAFT_EDITOR_CONTENT, "Content");
            expect(store.state.editorContent).toEqual("Content");
        });
        test("SET_DRAFT_COLLAPSED_CONTENT", () => {
            store.commit(SET_DRAFT_COLLAPSED_CONTENT, true);
            expect(store.state.collapsedContent).toBeTruthy();
            store.commit(SET_DRAFT_COLLAPSED_CONTENT, false);
            expect(store.state.collapsedContent).toBeFalsy();
        });
        test("SET_MAX_MESSAGE_SIZE", () => {
            expect(store.state.maxMessageSize).toBe(0);
            store.commit(SET_MAX_MESSAGE_SIZE, 30);
            expect(store.state.maxMessageSize).toBe(30);
        });
        test("SHOW_SENDER", () => {
            expect(store.state.isSenderShown).toBeFalsy();
            store.commit(SHOW_SENDER, "pouet");
            expect(store.state.isSenderShown).toBe("pouet");
        });
    });

    describe("actions", () => {
        const mailboxesService = new MockMailboxesClient();

        beforeEach(() => {
            inject.register({ provide: "MailboxesPersistence", factory: () => mailboxesService });
            mailboxesService.getMailboxConfig.mockReturnValue({ messageMaxSize: 30 });
        });

        test("LOAD_MAX_MESSAGE_SIZE", async () => {
            await store.dispatch(LOAD_MAX_MESSAGE_SIZE, 3);
            expect(mailboxesService.getMailboxConfig).toHaveBeenCalledWith(3);
            expect(store.state.maxMessageSize).toBe(30 / 1.33);
        });
    });

    describe("getters", () => {
        test("IS_SENDER_SHOWN", () => {
            const userSettings = {};
            expect(store.getters[IS_SENDER_SHOWN](userSettings)).toBeFalsy();
            userSettings.always_show_from = true;
            expect(store.getters[IS_SENDER_SHOWN](userSettings)).toBeTruthy();
            userSettings.always_show_from = false;
            store.commit(SHOW_SENDER, true);
            expect(store.getters[IS_SENDER_SHOWN](userSettings)).toBeTruthy();
        });
    });
});
