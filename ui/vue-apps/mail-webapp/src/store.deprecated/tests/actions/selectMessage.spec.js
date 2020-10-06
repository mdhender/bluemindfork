import { selectMessage } from "../../actions/selectMessage";
import { ItemUri } from "@bluemind/item-uri";
import { MimeType } from "@bluemind/email";
import ServiceLocator from "@bluemind/inject";

ServiceLocator.register({
    provide: "UserSession",
    factory: () => {
        return {
            roles: [["hasCalendar"]]
        };
    }
});

const folderUid = "folder:uid",
    messageId = 10;
const messageKey = ItemUri.encode(messageId, folderUid);
const anotherMessageKey = ItemUri.encode(20, folderUid);

const context = {
    dispatch: jest.fn().mockReturnValue(
        Promise.resolve([
            {
                computeParts() {
                    return {
                        attachments: "All attachments",
                        inlines: [
                            { capabilities: [MimeType.TEXT_CALENDAR], parts: ["The bad one"] },
                            { capabilities: [MimeType.TEXT_PLAIN], parts: ["The", "good", "one"] }
                        ]
                    };
                }
            }
        ])
    ),
    commit: jest.fn(),
    state: {
        currentMessage: {},
        messages: { itemKeys: [1, 2, 3] },
        sorted: "up to down"
    },
    rootState: {
        mail: {
            activeFolder: null,
            messages: {
                [messageKey]: { composing: false },
                [anotherMessageKey]: { composing: false }
            }
        }
    },
    rootGetters: {
        "mail/MY_DRAFTS": {}
    }
};

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
        selectMessage(context, anotherMessageKey).then(() => {
            expect(context.dispatch).toHaveBeenCalledWith("$_getIfNotPresent", [anotherMessageKey]);
            done();
        });
    });
    test("set the current message in state", done => {
        selectMessage(context, anotherMessageKey).then(() => {
            expect(context.commit).toHaveBeenCalledWith("currentMessage/update", { key: anotherMessageKey });
            expect(context.commit).toHaveBeenCalledWith("currentMessage/setParts", {
                attachments: "All attachments",
                inlines: ["The", "good", "one"]
            });
            done();
        });
    });
    test("to fetch inline parts", done => {
        selectMessage(context, anotherMessageKey).then(() => {
            ["The", "good", "one"].forEach(part => {
                expect(context.dispatch).toHaveBeenCalledWith("messages/fetch", {
                    messageKey: anotherMessageKey,
                    part,
                    isAttachment: false
                });
            });
            done();
        });
    });
});
