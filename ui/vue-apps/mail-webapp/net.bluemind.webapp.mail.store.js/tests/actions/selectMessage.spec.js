import { selectMessage } from "../../src/actions/selectMessage";
import { ItemUri } from "@bluemind/item-uri";
import { MimeType } from "@bluemind/email";

const context = {
    dispatch: jest.fn().mockReturnValue(
        Promise.resolve({
            computeParts() {
                return {
                    attachments: "All attachments",
                    inlines: [
                        { capabilities: [MimeType.TEXT_CALENDAR], parts: ["The bad one"] },
                        { capabilities: [MimeType.TEXT_PLAIN], parts: ["The", "good", "one"] }
                    ]
                };
            }
        })
    ),
    commit: jest.fn(),
    state: {
        currentFolderKey: "key",
        currentMessage: {},
        messages: { itemKeys: [1, 2, 3] },
        sorted: "up to down"
    }
};

const folderUid = "folder:uid",
    messageId = 10;
const messageKey = ItemUri.encode(messageId, folderUid);
describe("[Mail-WebappStore][actions] : selectMessage", () => {
    beforeEach(() => {
        context.dispatch.mockClear();
        context.commit.mockClear();
        context.state.currentMessage.key = messageKey;
        context.state.messages.itemKeys = [1, 2, 3];
    });
    test("do nothing if message key is the same", () => {
        selectMessage(context, messageKey);
        expect(context.commit).not.toHaveBeenCalled();
        expect(context.dispatch).not.toHaveBeenCalled();
    });
    test("to load the selected message", done => {
        const another = ItemUri.encode(20, folderUid);
        selectMessage(context, another).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", another);
            done();
        });
    });
    test("set the current message in state", done => {
        const another = ItemUri.encode(20, folderUid);
        selectMessage(context, another).then(() => {
            expect(context.commit).toHaveBeenCalledWith("currentMessage/update", { key: another });
            expect(context.commit).toHaveBeenCalledWith("currentMessage/setParts", {
                attachments: "All attachments",
                inlines: ["The", "good", "one"]
            });
            done();
        });
    });
    test("to fetch inline parts", done => {
        const another = ItemUri.encode(20, folderUid);
        selectMessage(context, another).then(() => {
            ["The", "good", "one"].forEach(part => {
                expect(context.dispatch).toHaveBeenCalledWith("messages/fetch", {
                    messageKey: another,
                    part,
                    isAttachment: false
                });
            });
            done();
        });
    });
});
