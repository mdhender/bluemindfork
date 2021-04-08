import { create, createOnlyMetadata } from "../message";
import MessageAdaptor from "../../store/messages/helpers/MessageAdaptor";

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
});
