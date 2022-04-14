import { BmProgress } from "@bluemind/styleguide";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));

import MailAttachmentsBlock from "../src/components/MailAttachment/MailAttachmentsBlock";
import { createStore, createWrapper } from "./testUtils";

describe("MailAttachmentsBlock", () => {
    test("is a Vue instance", () => {
        const wrapper = mountAttachmentBlock(1);
        expect(wrapper.vm).toBeTruthy();
    });

    test("should display an error bar if attachments are too heavy", async () => {
        const wrapper = mountAttachmentBlock(11);
        const progressComponent = wrapper.findComponent(BmProgress);
        expect(progressComponent.exists()).toBe(true);
        expect(progressComponent.props().variant).toBe("danger");
    });
    test("should display a warning bar if attachments are more than 50% than the authorized weight", async () => {
        const wrapper = mountAttachmentBlock(7);
        expect(wrapper.findComponent(BmProgress).props().variant).toBe("warning");
    });
    test("should display a success bar if attachments are less than 50% than the authorized weight", async () => {
        const wrapper = mountAttachmentBlock(3);
        expect(wrapper.findComponent(BmProgress).props().variant).toBe("success");
    });
});

function mountAttachmentBlock(attachmentSize) {
    const message = {
        key: "truc",
        composing: true,
        attachments: [{ mime: "image/jpeg", size: attachmentSize, progress: {} }]
    };
    const store = createStore();
    const key = Object.keys(store.state.mail.conversations.conversationByKey).pop();
    message.conversationRef = { key };
    store.commit("mail/ADD_MESSAGES", { messages: [message] });

    return createWrapper(
        MailAttachmentsBlock,
        { store },
        { message, attachments: message.attachments, expanded: false }
    );
}
