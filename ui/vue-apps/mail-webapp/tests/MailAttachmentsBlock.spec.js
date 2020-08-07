import MailAttachmentsBlock from "../src/components/MailAttachment/MailAttachmentsBlock";
import { BmProgress } from "@bluemind/styleguide";
jest.mock("@bluemind/styleguide/css/_variables.scss", () => ({ iconsColors: "" }));
import { createWrapper } from "./testUtils";

describe("MailAttachmentsBlock", () => {
    test("is a Vue instance", () => {
        const wrapper = mountAttachmentBlock(1);
        expect(wrapper.isVueInstance()).toBeTruthy();
    });

    test("should display an error bar if attachments are too heavy", async () => {
        const wrapper = mountAttachmentBlock(11);
        expect(wrapper.find(BmProgress).props().variant).toBe("danger");
    });
    test("should display a warning bar if attachments are more than 50% than the authorized weight", async () => {
        const wrapper = mountAttachmentBlock(7);
        expect(wrapper.find(BmProgress).props().variant).toBe("warning");
    });
    test("should display a primary bar if attachments are less than 50% than the authorized weight", async () => {
        const wrapper = mountAttachmentBlock(3);
        expect(wrapper.find(BmProgress).props().variant).toBe("primary");
    });
});

function mountAttachmentBlock(attachmentSize) {
    return createWrapper(
        MailAttachmentsBlock,
        {},
        {
            attachments: [{ mime: "image/jpeg", size: attachmentSize }],
            editable: true
        }
    );
}
