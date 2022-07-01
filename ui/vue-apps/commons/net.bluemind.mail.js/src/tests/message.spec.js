import shuffle from "lodash.shuffle";
import { createOnlyMetadata, isForward, MessageHeader } from "../message";

describe("Message model", () => {
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
