import { DraftStatus } from "@bluemind/backend.mail.store";
import { Flag } from "@bluemind/email";
import { MockMailboxItemsClient } from "@bluemind/test-mocks";
import { saveDraft } from "../../src/actions/saveDraft";
import ServiceLocator from "@bluemind/inject";

let itemsService;
ServiceLocator.register({ provide: "MailboxItemsPersistence", factory: () => itemsService });
ServiceLocator.register({ provide: "UserSession", factory: () => "" });

const context = {
    dispatch: jest.fn().mockReturnValue(Promise.resolve([1, 2, 3])),
    commit: jest.fn(),
    state: {},
    getters: {
        my: {
            DRAFTS: { key: "draft-key", uid: "trash-uid" }
        }
    }
};

let expectedMailItem;

describe("[Mail-WebappStore][actions] :  saveDraft", () => {
    beforeEach(() => {
        itemsService = new MockMailboxItemsClient();
        itemsService.create = jest.fn().mockReturnValue(Promise.resolve({ id: "mock-id" }));
        context.dispatch.mockClear();
        context.commit.mockClear();
        context.state.draft = {
            id: null,
            status: DraftStatus.NEW,
            saveDate: null,
            parts: { attachments: [], inlines: [] },
            content: "Bla blabla bla.",
            previousMessage: "",
            type: "html",
            subject: "TestSubject",
            recipients: ["toto@toto.pom"]
        };
        expectedMailItem = {
            body: {
                subject: "TestSubject",
                headers: undefined,
                recipients: [
                    {
                        address: undefined,
                        dn: undefined,
                        kind: "Originator"
                    }
                ],
                messageId: undefined,
                references: undefined,
                structure: {
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
                }
            },
            flags: [Flag.SEEN]
        };
    });
    test("Save new draft", async () => {
        await saveDraft(context);
        expect(context.commit).toHaveBeenNthCalledWith(1, "draft/update", { status: DraftStatus.SAVING });
        expect(context.commit).toHaveBeenNthCalledWith(2, "draft/update", {
            status: DraftStatus.SAVED,
            saveDate: expect.anything(),
            id: expect.anything()
        });
        expect(itemsService.create).toHaveBeenCalledWith(expectedMailItem);
    });
    test("Modify existing draft", async () => {
        const contentText = "My wonderful content!";
        context.state.draft.content = contentText;
        await saveDraft(context);
        expect(context.commit).toHaveBeenNthCalledWith(1, "draft/update", { status: DraftStatus.SAVING });
        expect(context.commit).toHaveBeenNthCalledWith(2, "draft/update", {
            status: DraftStatus.SAVED,
            saveDate: expect.anything(),
            id: expect.anything()
        });
        expect(itemsService.create).toHaveBeenCalledWith(expectedMailItem);

        const newContentText = contentText + " Deal with it bee hatch!";
        context.state.draft.content = newContentText;
        context.state.draft.subject = "ModifiedSubject";
        await saveDraft(context);
        expect(context.commit).toHaveBeenNthCalledWith(1, "draft/update", { status: DraftStatus.SAVING });
        expect(context.commit).toHaveBeenNthCalledWith(2, "draft/update", {
            status: DraftStatus.SAVED,
            saveDate: expect.anything(),
            id: expect.anything()
        });
        expectedMailItem.body.subject = "ModifiedSubject";
        expect(itemsService.create).toHaveBeenCalledWith(expectedMailItem);
    });
    test("With attachments", async () => {
        context.state.draft.parts.attachments.push("attachment1");
        context.state.draft.parts.attachments.push("attachment2");
        await saveDraft(context);
        expect(context.commit).toHaveBeenNthCalledWith(1, "draft/update", { status: DraftStatus.SAVING });
        expect(context.commit).toHaveBeenNthCalledWith(2, "draft/update", {
            status: DraftStatus.SAVED,
            saveDate: expect.anything(),
            id: expect.anything()
        });
        expectedMailItem.body.structure = {
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
                "attachment1",
                "attachment2"
            ]
        };
        expect(itemsService.create).toHaveBeenCalledWith(expectedMailItem);
    });
    test("With error", async () => {
        itemsService.create.mockImplementation(() => {
            throw new Error();
        });
        await saveDraft(context);
        expect(context.commit).toHaveBeenNthCalledWith(1, "draft/update", { status: DraftStatus.SAVING });
        expect(itemsService.create).toHaveBeenCalledWith(expectedMailItem);
        expect(itemsService.create).toThrow(new Error());
        expect(context.commit).toHaveBeenNthCalledWith(2, "draft/update", {
            status: DraftStatus.SAVE_ERROR,
            saveDate: null,
            id: undefined
        });
    });
});
