import { MimeType } from "@bluemind/email";
import { MockMailboxItemsClient } from "@bluemind/test-utils";
import ServiceLocator from "@bluemind/inject";
import { messageUtils, fileUtils } from "@bluemind/mail";

import { saveAsap } from "../../actions/save";
import { MY_DRAFTS } from "~/getters";
import { SET_MESSAGES_STATUS } from "~/mutations";

const { MessageAdaptor, MessageStatus, createWithMetadata } = messageUtils;
const { FileStatus } = fileUtils;
import store from "@bluemind/store";
jest.mock("../../../api/apiMessages");
let itemsService, draft, context, saveParams;

const draftInternalId = "draft-internal-id";
const draftFolderKey = "draft-folder-key";
const messageCompose = { editorContent: "", collapsedContent: null, inlineImagesSaved: [] };

describe("[Mail-WebappStore][actions] :  save", () => {
    beforeEach(() => {
        store.commit = jest.fn();

        itemsService = new MockMailboxItemsClient();
        ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => itemsService });

        draft = createWithMetadata({
            internalId: draftInternalId,
            folder: { key: draftFolderKey, uid: draftFolderKey }
        });
        draft.structure = {
            mime: "multipart/mixed",
            children: [
                {
                    mime: "multipart/alternative",
                    children: [
                        {
                            address: "old",
                            charset: "utf-8",
                            encoding: "quoted-printable",
                            mime: "text/plain"
                        },
                        {
                            address: "old",
                            charset: "utf-8",
                            encoding: "quoted-printable",
                            mime: "text/html"
                        }
                    ]
                },
                { address: "2" },
                { address: "3" }
            ]
        };
        draft.inlinePartsByCapabilities = [{ capabilities: [MimeType.TEXT_HTML], parts: [] }];
        draft.date = new Date();
        draft.status = MessageStatus.IDLE;

        saveParams = { draft, messageCompose, files: [] };
        MessageAdaptor.fromMailboxItem = jest.fn();
        MessageAdaptor.fromMailboxItem.mockReturnValue({ inlinePartsByCapabilities: null });
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
        expect(store.commit).toHaveBeenNthCalledWith(1, `mail/${SET_MESSAGES_STATUS}`, [
            {
                key: draft.key,
                status: MessageStatus.SAVING
            }
        ]);
        expect(store.commit).toHaveBeenNthCalledWith(9, `mail/${SET_MESSAGES_STATUS}`, [
            {
                key: draft.key,
                status: MessageStatus.IDLE
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
        draft.attachments = [{ address: "2" }, { address: "3" }];
        const files = [
            { address: "2", status: FileStatus.UPLOADED, mime: "anything" },
            { address: "3", status: FileStatus.UPLOADED, mime: "anything" }
        ];
        saveParams = { ...saveParams, files };
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
                                    address: "tmpAddress",
                                    charset: "utf-8",
                                    encoding: "quoted-printable",
                                    mime: "text/plain"
                                },
                                {
                                    address: "tmpAddress",
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

    test("With error", async () => {
        itemsService.updateById.mockImplementation(() => {
            throw new Error();
        });
        await saveAsap(context, saveParams);
        expect(itemsService.updateById).toHaveBeenCalledWith(draftInternalId, expect.anything());
        expect(itemsService.updateById).toThrow(new Error());
        expect(store.commit).toHaveBeenNthCalledWith(6, `mail/${SET_MESSAGES_STATUS}`, [
            {
                key: draft.key,
                status: MessageStatus.SAVE_ERROR
            }
        ]);
        jest.spyOn(global.console, "error");
    });
});
