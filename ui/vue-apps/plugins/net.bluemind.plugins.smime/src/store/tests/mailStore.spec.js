import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import storeOptions from "../mail/";
import { DISPLAY_UNTRUSTED } from "../mail/types";

Vue.use(Vuex);

describe("smime store", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(storeOptions));
    });

    describe("missingCertificates", () => {
        test("empty mail tips", async () => {
            expect(store.state.missingCertificates.length).toEqual(0);
            const mailTips = [];
            store.commit("SET_MAIL_TIPS", mailTips);
            expect(store.state.missingCertificates.length).toEqual(0);
        });
        test("populate missingCertificates", async () => {
            expect(store.state.missingCertificates.length).toEqual(0);
            const mailTips = [
                {
                    forRecipient: null,
                    matchingTips: [{ mailtipType: "Signature", value: "false" }]
                },
                {
                    forRecipient: {
                        email: "keytest@test.com",
                        name: "keytest",
                        addressType: "SMTP",
                        recipientType: "TO"
                    },
                    matchingTips: [{ mailtipType: "HasPublicKeyCertificate", value: "false" }]
                }
            ];
            store.commit("SET_MAIL_TIPS", mailTips);
            expect(store.state.missingCertificates).toStrictEqual(["keytest@test.com"]);
        });
    });

    describe("store untrusted message key when user choose to display it", () => {
        test("add message key when DISPLAY_UNTRUSTED mutation is called", () => {
            expect(store.state.displayUntrusted.length).toEqual(0);
            store.commit(DISPLAY_UNTRUSTED, ["message-key"]);
            expect(store.state.displayUntrusted.length).toEqual(1);
            expect(store.state.displayUntrusted[0]).toEqual("message-key");
        });
    });

    describe("set encrypt error", () => {
        test("if draft save failed with an error containing [SMIME_ENCRYPTION_ERROR:errorCode]", () => {
            expect(store.state.encryptError).toEqual(null);
            store.commit("SET_SAVE_ERROR", {});
            expect(store.state.encryptError).toEqual(null);

            store.commit("SET_SAVE_ERROR", { message: "anything" });
            expect(store.state.encryptError).toEqual(null);

            store.commit("SET_SAVE_ERROR", { message: "SMIME_ENCRYPTION_ERROR:" });
            expect(store.state.encryptError).toEqual(null);

            store.commit("SET_SAVE_ERROR", { message: "[SMIME_ENCRYPTION_ERROR:5]" });
            expect(store.state.encryptError).toEqual(5);

            store.commit("SET_SAVE_ERROR", { message: "[SMIME_ENCRYPTION_ERROR:ohyeayea10]" });
            expect(store.state.encryptError).toEqual(null);
        });
    });
});
