import { MimeType } from "@bluemind/email";
import { MockMailboxItemsClient } from "@bluemind/test-utils";
import ServiceLocator from "@bluemind/inject";

import apiMessages from "../../../api/apiMessages";
import { saveAsap } from "../../actions/save";
import { MessageStatus, createWithMetadata } from "~model/message";
import htmlWithBase64Images from "../data/htmlWithBase64Images";
import { MY_DRAFTS } from "~getters";
import { SET_MESSAGES_STATUS } from "~mutations";
import { AttachmentStatus } from "~model/attachment";

jest.mock("../../../api/apiMessages");
let itemsService, draft, context, saveParams;

const draftInternalId = "draft-internal-id";
const draftFolderKey = "draft-folder-key";
const messageCompose = { editorContent: "", collapsedContent: null, inlineImagesSaved: [] };

describe("[Mail-WebappStore][actions] :  save", () => {
    beforeEach(() => {
        itemsService = new MockMailboxItemsClient();
        apiMessages.multipleById.mockResolvedValueOnce([{ remoteRef: { imapUid: "" } }]);
        ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => itemsService });

        draft = createWithMetadata({
            internalId: draftInternalId,
            folder: { key: draftFolderKey, uid: draftFolderKey }
        });
        draft.inlinePartsByCapabilities = [{ capabilities: [MimeType.TEXT_HTML], parts: [] }];
        draft.date = new Date();
        draft.status = MessageStatus.LOADED;

        saveParams = { draft, messageCompose };

        context = {
            dispatch: jest.fn().mockReturnValue(Promise.resolve([1, 2, 3])),
            commit: jest.fn(),
            state: {
                [draft.key]: draft
            },
            rootGetters: {
                ["mail/" + MY_DRAFTS]: { key: draftFolderKey }
            }
        };
    });

    test("Save new draft", async () => {
        await saveAsap(context, saveParams);
        expect(context.commit).toHaveBeenNthCalledWith(1, SET_MESSAGES_STATUS, [
            {
                key: draft.key,
                status: MessageStatus.SAVING
            }
        ]);
        expect(context.commit).toHaveBeenNthCalledWith(8, SET_MESSAGES_STATUS, [
            {
                key: draft.key,
                status: MessageStatus.LOADED
            }
        ]);
        expect(itemsService.updateById).toHaveBeenCalledWith(draftInternalId, expect.anything());
    });

    test("Subject is modified", async () => {
        draft.subject = "Modified subject";
        await saveAsap(context, saveParams);

        expect(itemsService.updateById).toHaveBeenCalledWith(draftInternalId, expect.anything());
        const mailboxItemArg = itemsService.updateById.mock.calls[0][1];
        expect(mailboxItemArg).toMatchObject({ body: { subject: "Modified subject" } });
    });

    test("With attachments, expect a mixed structure", async () => {
        draft.attachments = [
            { address: "2", status: AttachmentStatus.LOADED, mime: "anything" },
            { address: "3", status: AttachmentStatus.LOADED, mime: "anything" }
        ];
        await saveAsap(context, saveParams);

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
                                    address: "1.1",
                                    charset: "utf-8",
                                    encoding: "quoted-printable",
                                    mime: "text/plain"
                                },
                                {
                                    address: "1.2",
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

    test.only("With error", async () => {
        itemsService.updateById.mockImplementation(() => {
            throw new Error();
        });
        await saveAsap(context, saveParams);
        expect(itemsService.updateById).toHaveBeenCalledWith(draftInternalId, expect.anything());
        expect(itemsService.updateById).toThrow(new Error());
        expect(context.commit).toHaveBeenNthCalledWith(7, SET_MESSAGES_STATUS, [
            {
                key: draft.key,
                status: MessageStatus.SAVE_ERROR
            }
        ]);
    });

    test("With inline images", async () => {
        messageCompose.editorContent = htmlWithBase64Images;
        const expectedStructureInlineImages = {
            children: [
                {
                    address: "1",
                    charset: "utf-8",
                    encoding: "quoted-printable",
                    mime: "text/plain"
                },
                {
                    children: [
                        {
                            address: "2.1",
                            charset: "utf-8",
                            encoding: "quoted-printable",
                            mime: "text/html"
                        },
                        {
                            address: "2.2",
                            contentId: "<cid1@bluemind.net>",
                            dispositionType: "INLINE",
                            encoding: "base64",
                            mime: "image/png"
                        },
                        {
                            address: "2.3",
                            contentId: "<cid2@bluemind.net>",
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

        await saveAsap(context, saveParams);
        expect(context.commit).toHaveBeenNthCalledWith(1, SET_MESSAGES_STATUS, [
            {
                key: draft.key,
                status: MessageStatus.SAVING
            }
        ]);
        expect(context.commit).toHaveBeenNthCalledWith(8, SET_MESSAGES_STATUS, [
            {
                key: draft.key,
                status: MessageStatus.LOADED
            }
        ]);
        expect(itemsService.uploadPart).toBeCalledTimes(4);
        expect(itemsService.uploadPart).toHaveBeenCalledWith(
            expect.stringContaining('<img data-bm-cid="<cid1@bluemind.net>" src="cid:cid1@bluemind.net">')
        );
        expect(itemsService.uploadPart).toHaveBeenCalledWith(
            expect.stringContaining('<img data-bm-cid="<cid2@bluemind.net>" src="cid:cid2@bluemind.net">')
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
