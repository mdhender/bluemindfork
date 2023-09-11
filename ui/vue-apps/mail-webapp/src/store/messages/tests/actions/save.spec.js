import { MimeType } from "@bluemind/email";
import { MockMailboxItemsClient } from "@bluemind/test-utils";
import ServiceLocator from "@bluemind/inject";
import { messageUtils } from "@bluemind/mail";

import { saveAsap } from "../../actions/save";
import { MY_DRAFTS } from "~/getters";
import { SET_MESSAGES_STATUS } from "~/mutations";

const { MessageAdaptor, MessageStatus, createWithMetadata } = messageUtils;
jest.mock("../../../api/apiMessages");
let itemsService, draft, context, saveParams;

const draftInternalId = "draft-internal-id";
const draftFolderKey = "draft-folder-key";
const messageCompose = { editorContent: "", collapsedContent: null, inlineImagesSaved: [] };

describe("[Mail-WebappStore][actions] :  save", () => {
    beforeEach(() => {
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
        expect(context.commit).toHaveBeenNthCalledWith(1, SET_MESSAGES_STATUS, [
            {
                key: draft.key,
                status: MessageStatus.SAVING
            }
        ]);
        expect(context.commit).toHaveBeenNthCalledWith(8, SET_MESSAGES_STATUS, [
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

    test("With error", async () => {
        itemsService.updateById.mockImplementation(() => {
            throw new Error();
        });
        await saveAsap(context, saveParams);
        expect(itemsService.updateById).toHaveBeenCalledWith(draftInternalId, expect.anything());
        expect(itemsService.updateById).toThrow(new Error());
        expect(context.commit).toHaveBeenNthCalledWith(5, SET_MESSAGES_STATUS, [
            {
                key: draft.key,
                status: MessageStatus.SAVE_ERROR
            }
        ]);
        jest.spyOn(global.console, "error");
    });
});
