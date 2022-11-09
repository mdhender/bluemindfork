import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import inject from "@bluemind/inject";
import { MockMailboxesClient, MockMailtipClient } from "@bluemind/test-utils";
import storeOptions from "../messageCompose";
import { CHECK_CORPORATE_SIGNATURE, LOAD_MAX_MESSAGE_SIZE } from "~/actions";
import { IS_SENDER_SHOWN } from "~/getters";
import {
    SET_CORPORATE_SIGNATURE,
    SET_DISCLAIMER,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MAX_MESSAGE_SIZE,
    SHOW_SENDER
} from "~/mutations";

Vue.use(Vuex);

jest.mock("postal-mime", () => ({ TextEncoder: jest.fn() }));

describe("messageCompose", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(storeOptions));
    });

    describe("state", () => {
        test("showFormattingToolbar is synced with app data and its default is false", async () => {
            expect(storeOptions.state.showFormattingToolbar).toEqual(false);
            expect(storeOptions.state.synced.includes("showFormattingToolbar")).toEqual(true);
        });
    });

    describe("mutations", () => {
        test("SET_CORPORATE_SIGNATURE", () => {
            const corpSign = { uid: "my-uid", html: "html sign" };
            store.commit(SET_CORPORATE_SIGNATURE, corpSign);
            expect(store.state.corporateSignature).toStrictEqual(corpSign);
        });
        test("SET_DISCLAIMER", () => {
            const disclaimer = { uid: "my-disc-uid", html: "disc-html sign" };
            store.commit(SET_DISCLAIMER, disclaimer);
            expect(store.state.disclaimer).toStrictEqual(disclaimer);
        });
        test("dont change corporate signature or disclaimer if it's same uid", () => {
            const corpSign = { uid: "my-uid", html: "html sign" };
            store.commit(SET_CORPORATE_SIGNATURE, corpSign);

            const corpSignWithSameUid = { uid: "my-uid", html: "just to check result" };
            store.commit(SET_CORPORATE_SIGNATURE, corpSignWithSameUid);
            expect(store.state.corporateSignature.html).toEqual("html sign");

            const disclaimer = { uid: "my-uid", html: "html sign" };
            store.commit(SET_DISCLAIMER, disclaimer);

            const disclaimerWithSameUid = { uid: "my-uid", html: "just to check result" };
            store.commit(SET_DISCLAIMER, disclaimerWithSameUid);
            expect(store.state.disclaimer.html).toEqual("html sign");
        });
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
        test("CHECK_CORPORATE_SIGNATURE", async () => {
            const corpSign =
                '{"html":"<p>________</p> \\n<p>THIS IS CORP SIGN \\"test2\\"</p>","text":"________\\n\\nTHIS IS CORP SIGN \\"test2\\"\\n","uid":"140C00FB-8B4A-40D4-8443-7C3A64E78DFB","isDisclaimer":false,"usePlaceholder":false}';
            const disclaimer =
                '{"html":"<p>IM DISCLAIMER !!</p>","text":"IM DISCLAIMER !!\\n","uid":"a7a1c863.internal:bm-signature-plugin:disclaimer","isDisclaimer":true,"usePlaceholder":false}';
            const mailtipService = new MockMailtipClient();
            inject.register({ provide: "MailTipPersistence", factory: () => mailtipService });
            mailtipService.getMailTips.mockReturnValue([
                {
                    forRecipient: null,
                    matchingTips: [
                        { mailtipType: "Signature", value: corpSign },
                        { mailtipType: "Signature", value: disclaimer }
                    ]
                }
            ]);

            inject.register({ provide: "UserSession", factory: () => ({ defaultEmail: "user@mail.com" }) });
            const message = { from: {}, to: [{ address: "test@mail.com", dn: "" }], cc: [], bcc: [] };
            await store.dispatch(CHECK_CORPORATE_SIGNATURE, { message });
            expect(mailtipService.getMailTips).toHaveBeenCalled();
            expect(store.state.disclaimer).toStrictEqual(JSON.parse(disclaimer));
            expect(store.state.corporateSignature).toStrictEqual(JSON.parse(corpSign));
        });

        test("LOAD_MAX_MESSAGE_SIZE", async () => {
            const mailboxesService = new MockMailboxesClient();
            inject.register({ provide: "MailboxesPersistence", factory: () => mailboxesService });
            mailboxesService.getMailboxConfig.mockReturnValue({ messageMaxSize: 30 });

            await store.dispatch(LOAD_MAX_MESSAGE_SIZE, 3);
            expect(mailboxesService.getMailboxConfig).toHaveBeenCalledWith(3);
            expect(store.state.maxMessageSize).toBe(30 / 1.33);
        });
    });

    describe("getters", () => {
        test("IS_SENDER_SHOWN", () => {
            const userSettings = {};
            expect(store.getters[IS_SENDER_SHOWN](userSettings)).toBeFalsy();
            userSettings.always_show_from = "true";
            expect(store.getters[IS_SENDER_SHOWN](userSettings)).toBeTruthy();
            userSettings.always_show_from = "false";
            store.commit(SHOW_SENDER, true);
            expect(store.getters[IS_SENDER_SHOWN](userSettings)).toBeTruthy();
        });
    });
});
