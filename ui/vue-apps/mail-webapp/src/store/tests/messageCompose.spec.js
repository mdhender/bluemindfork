import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import inject from "@bluemind/inject";
import { MockMailboxesClient } from "@bluemind/test-utils";
import storeOptions from "../messageCompose";
import { LOAD_MAX_MESSAGE_SIZE, SET_DRAFT_CONTENT, DEBOUNCED_SET_MESSAGE_CONTENT } from "~/actions";
import { GET_DRAFT_CONTENT, IS_SENDER_SHOWN } from "~/getters";
import {
    ADD_FILE,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_FILE_ADDRESS,
    SET_FILE_HEADERS,
    SET_FILE_PROGRESS,
    SET_FILE_STATUS,
    SET_MAIL_TIPS,
    SET_MAX_MESSAGE_SIZE,
    SHOW_SENDER
} from "~/mutations";
import { wrapCorporateSignature, wrapDisclaimer } from "@bluemind/mail/src/signature/signature";

Vue.use(Vuex);

const file1 = {
    key: 1,
    fileName: "image.jpg",
    size: 100
};

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
        describe("Signatures", () => {
            test("SET_CORPORATE_SIGNATURE", () => {
                const corpSign = { uid: "my-uid", html: "html sign" };
                const mailTips = [{ matchingTips: [{ mailtipType: "Signature", value: JSON.stringify(corpSign) }] }];

                store.commit(SET_MAIL_TIPS, mailTips);

                expect(store.getters["SIGNATURE"].html).toStrictEqual(wrapCorporateSignature(corpSign.html));
            });
            test("SET_DISCLAIMER", () => {
                const disclaimer = { uid: "my-disc-uid", html: "disc-html sign" };
                const mailTips = MailTipsBuilder().disclaimer(disclaimer);

                store.commit(SET_MAIL_TIPS, mailTips);

                expect(store.getters["DISCLAIMER"].textContent).toEqual(disclaimer.html);
            });

            test("change corporate signature if it's not the same html", () => {
                store.commit(SET_MAIL_TIPS, MailTipsBuilder().corporateSignature({ uid: "my-uid", html: "html sign" }));
                store.commit(
                    SET_MAIL_TIPS,
                    MailTipsBuilder().corporateSignature({ uid: "my-uid", html: "html sign 2" })
                );

                expect(store.getters["SIGNATURE"].html).toEqual(wrapCorporateSignature("html sign 2"));
            });

            test("change disclaimer if it's not the same html", () => {
                store.commit(SET_MAIL_TIPS, MailTipsBuilder().disclaimer({ uid: "my-uid", html: "html sign" }));
                store.commit(SET_MAIL_TIPS, MailTipsBuilder().disclaimer({ uid: "my-uid", html: "html sign 2" }));

                expect(store.getters["DISCLAIMER"]).toEqual(wrapDisclaimer("html sign 2"));
            });

            function MailTipsBuilder() {
                return {
                    corporateSignature({ uid, html }) {
                        return [createMailTipsCorporateSignature(uid, html)];
                    },
                    disclaimer({ uid, html }) {
                        return [createDisclaimerMailTip(uid, html)];
                    }
                };
            }

            function createMailTipsCorporateSignature(uid, html) {
                return {
                    matchingTips: [
                        {
                            mailtipType: "Signature",
                            value: JSON.stringify({ uid, html })
                        }
                    ]
                };
            }

            function createDisclaimerMailTip(uid, html) {
                return {
                    matchingTips: [
                        {
                            mailtipType: "Signature",
                            value: JSON.stringify({ uid, html, isDisclaimer: true })
                        }
                    ]
                };
            }
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
        test("SET_MAIL_TIPS", () => {
            expect(store.state.mailTips.length).toBe(0);
            const mailTips = [
                {
                    forRecipient: {
                        email: "keytest@test.com",
                        name: "keytest",
                        addressType: "SMTP",
                        recipientType: "TO"
                    },
                    matchingTips: [{ mailtipType: "mailTip", value: "true" }]
                }
            ];
            store.commit(SET_MAIL_TIPS, mailTips);
            expect(store.state.mailTips).toBe(mailTips);
        });
    });
    describe("uploadfingFiles", () => {
        describe("ADD", () => {
            test("ADD_FILE", () => {
                store.commit(ADD_FILE, { file: file1 });
                expect(store.state.uploadingFiles[file1.key]).toEqual(file1);
            });
        });
        describe("File properties", () => {
            beforeEach(() => {
                store.commit(ADD_FILE, { file: file1 });
            });
            test("SET_FILE_PROGRESS", () => {
                store.commit(SET_FILE_PROGRESS, {
                    key: file1.key,
                    progress: { loaded: 500, total: 10000 }
                });
                expect(store.state.uploadingFiles[file1.key]).toEqual(
                    expect.objectContaining({ progress: { loaded: 500, total: 10000 } })
                );
            });
            test("SET_FILE_STATUS", () => {
                const status = "STATUS";
                store.commit(SET_FILE_STATUS, { key: file1.key, status });
                expect(store.state.uploadingFiles[file1.key]).toEqual(expect.objectContaining({ status }));
            });
            test("SET_FILE_ADDRESS", () => {
                const address = "1234";
                store.commit(SET_FILE_ADDRESS, { key: file1.key, address });
                expect(store.state.uploadingFiles[file1.key]).toEqual(expect.objectContaining({ address }));
            });
            test("SET_FILE_HEADERS", () => {
                const headers = [
                    { name: "header1", value: "value1" },
                    { name: "header2", value: "value2" }
                ];
                store.commit(SET_FILE_HEADERS, { key: file1.key, headers });
                expect(store.state.uploadingFiles[file1.key]).toEqual(expect.objectContaining({ headers }));
            });
        });
    });

    describe("actions", () => {
        test("LOAD_MAX_MESSAGE_SIZE", async () => {
            const mailboxesService = new MockMailboxesClient();
            inject.register({ provide: "MailboxesPersistence", factory: () => mailboxesService });
            mailboxesService.getMailboxConfig.mockReturnValue({ messageMaxSize: 30 });

            await store.dispatch(LOAD_MAX_MESSAGE_SIZE, 3);
            expect(mailboxesService.getMailboxConfig).toHaveBeenCalledWith(3);
            expect(store.state.maxMessageSize).toBe(30 / 1.33);
        });
        test("SET_DRAFT_CONTENT", async () => {
            storeOptions.actions[DEBOUNCED_SET_MESSAGE_CONTENT] = jest.fn();
            const store2 = new Vuex.Store(cloneDeep(storeOptions));

            const collapsed = "collapsed";
            const content = "content";
            store2.commit(SET_DRAFT_COLLAPSED_CONTENT, collapsed);
            await store2.dispatch(SET_DRAFT_CONTENT, { draft: { key: "key" }, html: content });
            expect(store2.state.editorContent).toEqual(content);
            expect(storeOptions.actions[DEBOUNCED_SET_MESSAGE_CONTENT]).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({ content: content + collapsed })
            );
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
        test("GET_DRAFT_CONTENT", () => {
            expect(store.getters[GET_DRAFT_CONTENT]).toEqual("");

            const content = "content";
            store.commit(SET_DRAFT_EDITOR_CONTENT, content);
            expect(store.getters[GET_DRAFT_CONTENT]).toEqual(content);

            const collapsed = "collapsed";
            store.commit(SET_DRAFT_COLLAPSED_CONTENT, collapsed);
            expect(store.getters[GET_DRAFT_CONTENT]).toEqual(content + collapsed);
        });

        test("should replace corp sign by placeholder", () => {
            store.commit(SET_MAIL_TIPS, [
                {
                    matchingTips: [
                        {
                            mailtipType: "Signature",
                            value: JSON.stringify({
                                uid: 1,
                                html: `<div class="bm-corporate-signature"> MY CORP IS BEAUTIFUL</div>`,
                                usePlaceholder: true
                            })
                        }
                    ]
                }
            ]);
            store.commit(
                SET_DRAFT_EDITOR_CONTENT,
                '<p>Content + signature</p> <div class="bm-corporate-signature"> MY CORP IS BEAUTIFUL</div>'
            );
            expect(store.getters[GET_DRAFT_CONTENT]).toEqual("<p>Content + signature</p> --X-BM-SIGNATURE--");
        });
    });
});
