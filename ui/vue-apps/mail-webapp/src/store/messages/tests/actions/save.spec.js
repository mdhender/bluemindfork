import { MimeType } from "@bluemind/email";
import { MockMailboxItemsClient } from "@bluemind/test-utils";
import ServiceLocator from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";

import save from "../../actions/save";
import { MessageStatus, createWithMetadata } from "../../../../model/message";
import mutationTypes from "../../../mutationTypes";
import htmlWithBase64Images from "../data/htmlWithBase64Images";

const mockedCidUid = "myCid";
let itemsService, draft, context;

const draftMessageKey = "draft-key";
const draftInternalId = "draft-internal-id";
const draftFolderKey = "draft-folder-key";
const messageCompose = { editorContent: "", collapsedContent: null };
const saveParams = {
    userPrefTextOnly: false,
    draftKey: draftMessageKey,
    myDraftsFolderKey: draftFolderKey,
    messageCompose
};

describe("[Mail-WebappStore][actions] :  save", () => {
    beforeEach(() => {
        itemsService = new MockMailboxItemsClient();
        ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => itemsService });

        draft = createWithMetadata({
            internalId: draftInternalId,
            folder: { key: draftFolderKey, uid: draftFolderKey }
        });
        draft.inlinePartsByCapabilities = [{ capabilities: [MimeType.TEXT_HTML], parts: [] }];
        draft.date = new Date();

        context = {
            dispatch: jest.fn().mockReturnValue(Promise.resolve([1, 2, 3])),
            commit: jest.fn(),
            state: {
                [draftMessageKey]: draft
            },
            rootGetters: {
                "mail/MY_DRAFTS": {
                    key: draftFolderKey
                }
            }
        };
    });

    test("Save new draft", async () => {
        await save(context, saveParams);
        expect(context.commit).toHaveBeenNthCalledWith(1, mutationTypes.SET_MESSAGES_STATUS, [
            {
                key: draftMessageKey,
                status: MessageStatus.SAVING
            }
        ]);
        expect(context.commit).toHaveBeenNthCalledWith(4, mutationTypes.SET_MESSAGES_STATUS, [
            {
                key: draftMessageKey,
                status: MessageStatus.LOADED
            }
        ]);
        expect(itemsService.updateById).toHaveBeenCalledWith(draftInternalId, expect.anything());
    });

    test("Subject is modified", async () => {
        draft.subject = "Modified subject";
        await save(context, saveParams);

        expect(itemsService.updateById).toHaveBeenCalledWith(draftInternalId, expect.anything());
        const mailboxItemArg = itemsService.updateById.mock.calls[0][1];
        expect(mailboxItemArg).toMatchObject({ body: { subject: "Modified subject" } });
    });

    test("With attachments, expect a mixed structure", async () => {
        draft.attachments = [{ address: "2" }, { address: "3" }];
        await save(context, saveParams);

        expect(itemsService.updateById).toHaveBeenCalledWith(draftInternalId, expect.anything());
        const mailboxItemArg = itemsService.updateById.mock.calls[0][1];
        expect(mailboxItemArg).toMatchObject({
            body: {
                structure: {
                    mime: "multipart/mixed",
                    children: [
                        {
                            mime: "multipart/alternative",
                            children: [
                                {
                                    address: undefined,
                                    charset: "utf-8",
                                    encoding: "quoted-printable",
                                    mime: "text/plain"
                                },
                                {
                                    address: undefined,
                                    charset: "utf-8",
                                    encoding: "quoted-printable",
                                    mime: "text/html"
                                }
                            ]
                        },
                        { address: "2" },
                        { address: "3" }
                    ]
                }
            }
        });
    });

    test("With error", async done => {
        itemsService.updateById.mockImplementation(() => {
            throw new Error();
        });
        try {
            await save(context, saveParams);
            done.fail("should not reach here");
        } catch (e) {
            expect(itemsService.updateById).toHaveBeenCalledWith(draftInternalId, expect.anything());
            expect(itemsService.updateById).toThrow(new Error());
            expect(context.commit).toHaveBeenNthCalledWith(4, mutationTypes.SET_MESSAGES_STATUS, [
                {
                    key: draftMessageKey,
                    status: MessageStatus.SAVE_ERROR
                }
            ]);
            done();
        }
    });

    test("With inline images", async () => {
        UUIDGenerator.generate = jest
            .fn()
            .mockReturnValueOnce(mockedCidUid)
            .mockReturnValueOnce(mockedCidUid + "2");

        messageCompose.editorContent = htmlWithBase64Images;
        const expectedStructureInlineImages = {
            children: [
                {
                    address: undefined,
                    charset: "utf-8",
                    encoding: "quoted-printable",
                    mime: "text/plain"
                },
                {
                    children: [
                        {
                            address: undefined,
                            charset: "utf-8",
                            encoding: "quoted-printable",
                            mime: "text/html"
                        },
                        {
                            address: undefined,
                            contentId: mockedCidUid + "@bluemind.net",
                            dispositionType: "INLINE",
                            encoding: "base64",
                            mime: "image/png"
                        },
                        {
                            address: undefined,
                            contentId: mockedCidUid + "2@bluemind.net",
                            dispositionType: "INLINE",
                            encoding: "base64",
                            mime: "image/svg+xml"
                        }
                    ],
                    mime: "multipart/related"
                }
            ],
            mime: "multipart/alternative"
        };

        await save(context, saveParams);
        expect(context.commit).toHaveBeenNthCalledWith(1, mutationTypes.SET_MESSAGES_STATUS, [
            {
                key: draftMessageKey,
                status: MessageStatus.SAVING
            }
        ]);
        expect(context.commit).toHaveBeenNthCalledWith(4, mutationTypes.SET_MESSAGES_STATUS, [
            {
                key: draftMessageKey,
                status: MessageStatus.LOADED
            }
        ]);
        expect(itemsService.uploadPart).toBeCalledTimes(4);
        expect(itemsService.uploadPart).toHaveBeenCalledWith(
            expect.stringContaining('<img src="cid:' + mockedCidUid + '@bluemind.net" />')
        );
        expect(itemsService.uploadPart).toHaveBeenCalledWith(
            expect.stringContaining('<img src="cid:' + mockedCidUid + '2@bluemind.net" />')
        );

        expect(itemsService.updateById).toHaveBeenCalledWith(draftInternalId, expect.anything());
        const mailboxItemArg = itemsService.updateById.mock.calls[0][1];
        expect(mailboxItemArg).toMatchObject({
            body: {
                structure: expectedStructureInlineImages
            }
        });
    });
});
