import { Flag } from "@bluemind/email";
import { MockMailboxItemsClient } from "@bluemind/test-mocks";
import ServiceLocator from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";
import { saveDraft } from "../../src/actions/saveDraft";
import DraftStatus from "../../mailbackend/MailboxItemsStore/Message";
import htmlWithBase64Images from "../data/htmlWithBase64Images";

const mockedCidUid = "myCid";
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
const alternativeStructure = {
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
};

describe("[Mail-WebappStore][actions] :  saveDraft", () => {
    beforeEach(() => {
        UUIDGenerator.generate = jest
            .fn()
            .mockReturnValueOnce(mockedCidUid)
            .mockReturnValueOnce(mockedCidUid + "2");

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
            recipients: ["toto@toto.pom"],
            attachmentStatuses: {}
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
                structure: alternativeStructure
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
        context.state.draft.parts.attachments.push({ uid: "attachment1" });
        context.state.draft.parts.attachments.push({ uid: "attachment2" });
        await saveDraft(context);
        expect(context.commit).toHaveBeenNthCalledWith(1, "draft/update", { status: DraftStatus.SAVING });
        expect(context.commit).toHaveBeenNthCalledWith(2, "draft/update", {
            status: DraftStatus.SAVED,
            saveDate: expect.anything(),
            id: "mock-id"
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
                { uid: "attachment1" },
                { uid: "attachment2" }
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

    test("With inline images", async () => {
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

        context.state.draft = Object.assign(context.state.draft, { content: htmlWithBase64Images });
        await saveDraft(context);
        expect(context.commit).toHaveBeenNthCalledWith(1, "draft/update", { status: DraftStatus.SAVING });
        expect(context.commit).toHaveBeenNthCalledWith(2, "draft/update", {
            status: DraftStatus.SAVED,
            saveDate: expect.anything(),
            id: "mock-id"
        });
        expect(itemsService.uploadPart).toBeCalledTimes(4);
        expect(itemsService.uploadPart).toHaveBeenCalledWith(
            expect.stringContaining('<img src="cid:' + mockedCidUid + '@bluemind.net" />')
        );
        expect(itemsService.uploadPart).toHaveBeenCalledWith(
            expect.stringContaining('<img src="cid:' + mockedCidUid + '2@bluemind.net" />')
        );

        expectedMailItem.body.structure = expectedStructureInlineImages;
        expect(itemsService.create).toHaveBeenCalledWith(expectedMailItem);

        // THEN TEST REMOVING IMAGES
        context.state.draft = Object.assign(context.state.draft, { content: "<html> blabla </html>" });
        await saveDraft(context);
        expect(context.commit).toHaveBeenCalledWith("draft/update", {
            status: DraftStatus.SAVED,
            saveDate: expect.anything(),
            id: "mock-id"
        });
        expectedMailItem.body.structure = alternativeStructure;
        expect(itemsService.create).toHaveBeenCalledWith(expectedMailItem);
    });
});
