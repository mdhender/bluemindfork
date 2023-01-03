import Vue from "vue";
import Vuex from "vuex";
import storeOptions from "../index";
import cloneDeep from "lodash.clonedeep";

Vue.use(Vuex);

describe("smime store", () => {
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(storeOptions));
    });

    describe("cannotEncryptEmails", () => {
        test("empty mail tips", async () => {
            expect(store.state.cannotEncryptEmails.length).toEqual(0);
            const mailTips = [];
            store.commit("SET_MAIL_TIPS", mailTips);
            expect(store.state.cannotEncryptEmails.length).toEqual(0);
        });
        test("populate cannotEncryptEmails", async () => {
            expect(store.state.cannotEncryptEmails.length).toEqual(0);
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
            expect(store.state.cannotEncryptEmails).toStrictEqual(["keytest@test.com"]);
        });
    });
});
