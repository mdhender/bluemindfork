import shuffle from "lodash.shuffle";
import { create, createOnlyMetadata, isForward, MessageHeader } from "~/src/message";
import MessageAdaptor from "~/store/messages/helpers/MessageAdaptor";

describe("Message model", () => {
    test("message model and message adaptor share same properties", () => {
        const emptyRemote = { value: { body: { headers: [], recipients: [], structure: { mime: "" } } } };

        const message = create();
        const adapted = MessageAdaptor.fromMailboxItem(emptyRemote, {});

        const messageProperties = Object.keys(message).sort();
        const adaptedProperties = Object.keys(adapted).sort();

        expect(messageProperties.length).toBe(adaptedProperties.length);
        messageProperties.forEach((prop, index) => {
            expect(prop).toBe(adaptedProperties[index]);
        });
    });

    test("update key when it's already set", () => {
        const oldInternalId = 123;
        const oldFolderKey = "oldFolderKey";
        const oldFolderUid = "oldFolderUid";

        const message = createOnlyMetadata({
            internalId: oldInternalId,
            folder: { key: oldFolderKey, uid: oldFolderUid }
        });
        expect(message.remoteRef.internalId).toBe(oldInternalId);
        expect(message.folderRef.key).toBe(oldFolderKey);
        expect(message.folderRef.uid).toBe(oldFolderUid);
    });

    describe("is Forward", () => {
        const randomHeaders = [{ name: "A-Random-Header" }, { name: "X-Custom-Header" }];
        const headers = [
            { name: MessageHeader.DELIVERED_TO },
            { name: MessageHeader.MAIL_FOLLOWUP_TO },
            { name: MessageHeader.X_LOOP },
            { name: MessageHeader.X_ORIGINAL_TO }
        ];
        const bmHeader = { name: MessageHeader.X_BM_DRAFT_INFO, values: ['{"type": "FORWARD"}'] };

        test("Headers", () => {
            headers.forEach(header => {
                const message = { headers: shuffle([...randomHeaders, header]) };
                expect(isForward(message)).toBeTruthy();
            });
        });
        test("BM header", () => {
            const message = { headers: shuffle([...randomHeaders, bmHeader]) };
            expect(isForward(message)).toBeTruthy();
        });
        test("Subject", () => {
            const forwardSubjects = [
                "Fw: plop plop",
                "Fwd: plop plop",
                "[Fw: plop plop] tadam",
                "[Fwd: plop plop] tadam",
                "plop plop (fwd)",
                "plop plop (fw)"
            ];
            forwardSubjects.forEach(forwardSubject => {
                const message = { subject: forwardSubject };
                expect(isForward(message)).toBeTruthy();
            });

            const notForwardSubjects = [
                "plop plop",
                "Forward: plop plop",
                "[Forward: plop plop]",
                "plop plop (forward)"
            ];
            notForwardSubjects.forEach(notForwardSubject => {
                const message = { subject: notForwardSubject };
                expect(isForward(message)).toBeFalsy();
            });
        });
    });
});
