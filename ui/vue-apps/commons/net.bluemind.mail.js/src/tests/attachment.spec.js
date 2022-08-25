import injector from "@bluemind/inject";
import { AttachmentStatus, create } from "../attachment";

injector.register({
    provide: "i18n",
    factory: () => ({
        t: () => ""
    })
});
describe("attachment model", () => {
    test("adapt attachment mime type according to filename if it's an application/octet-stream", () => {
        const status = AttachmentStatus.UPLOADED;
        let part = { mime: "application/octet-stream", fileName: "file.pdf" };
        let attachment = create(part, status);
        expect(attachment.mime).toBe("application/pdf");

        part = { mime: "application/octet-stream", fileName: "" };
        attachment = create(part, status);
        expect(attachment.mime).toBe("application/octet-stream");

        part = { mime: "application/octet-stream", fileName: "file.unknowntype" };
        attachment = create(part, status);
        expect(attachment.mime).toBe("application/octet-stream");
    });
});
