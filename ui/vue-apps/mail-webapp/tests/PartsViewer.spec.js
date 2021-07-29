import flushPromises from "flush-promises";

import { createStore, createWrapper } from "./testUtils";
import PartsViewer from "../src/components/MailViewer/PartsViewer/PartsViewer";
import MailAttachmentItem from "../src/components/MailAttachment/MailAttachmentItem";

describe("PartsViewer", () => {
    test("image/* file type is a viewer capacity", async () => {
        const inlinePartsByCapabilities = [
            {
                capabilities: ["image/jpeg"],
                parts: [
                    {
                        mime: "image/jpeg",
                        address: "2",
                        encoding: "base64",
                        charset: "us-ascii",
                        fileName: "IMG_20160724_083322.jpeg",
                        size: 13582
                    }
                ]
            },
            {
                capabilities: ["text/plain"],
                parts: [{ mime: "text/plain", address: "1", encoding: "7bit", charset: "us-ascii", size: 6 }]
            }
        ];

        const wrapper = mountComponent(inlinePartsByCapabilities);
        await flushPromises();
        expect(wrapper.vm.parts.length).toBe(1);
        expect(wrapper.vm.parts[0].mime).toBe("image/jpeg");
    });

    test("unsupported part are displayed as attachment", async () => {
        const inlinePartsByCapabilities = [
            { capabilities: [], parts: [{ mime: "application/pdf", address: "1", encoding: "base64", size: 27032 }] }
        ];
        const wrapper = mountComponent(inlinePartsByCapabilities);
        await flushPromises();

        expect(wrapper.vm.localAttachments.length).toBe(1);
        expect(wrapper.props().message.attachments.length).toBe(1);

        // if message key, localAttachments are cleaned
        const clonedMessage = JSON.parse(JSON.stringify(message));
        clonedMessage.key = "new-one";
        clonedMessage.inlinePartsByCapabilities = [];
        clonedMessage.attachments = [];
        mockedStore.commit("mail/ADD_MESSAGES", [clonedMessage]);
        wrapper.setProps({ message: clonedMessage });
        await flushPromises();

        expect(wrapper.vm.localAttachments.length).toBe(0);
        expect(wrapper.props().message.attachments.length).toBe(0);
    });
});

let mockedStore;
const message = {
    key: "truc",
    folderRef: { uid: "folderUid" },
    remoteRef: { imapUid: "imapUid" },
    composing: true,
    inlinePartsByCapabilities: [],
    attachments: []
};

function mountComponent(inlinePartsByCapabilities) {
    message.inlinePartsByCapabilities = inlinePartsByCapabilities;
    mockedStore = createStore();
    mockedStore.commit("mail/ADD_MESSAGES", [message]);
    return createWrapper(PartsViewer, { store: mockedStore }, { message });
}
