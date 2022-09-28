import injector from "@bluemind/inject";
import { create } from "../attachment";
import { FileStatus } from "../file";

injector.register({
    provide: "i18n",
    factory: () => ({
        t: () => ""
    })
});
describe("attachment model", () => {
    test("adapt attachment mime type according to filename if it's an application/octet-stream", () => {
        const status = FileStatus.UPLOADED;
        let part = { mime: "application/octet-stream", fileName: "file.pdf" };
        let attachment = create(part, status);
        expect(attachment.mime).toBe("application/pdf");

        part = { mime: "application/octet-stream", fileName: "" };
        attachment = create(part, status);
        expect(attachment.mime).toBe("application/octet-stream");

        part = { mime: "application/octet-stream", fileName: "file.unknowntype" };
        attachment = create(part, status);
        expect(attachment.mime).toBe("application/octet-stream");

        part = { mime: "application/octet-stream", fileName: "test.eml" };
        attachment = create(part, status);
        expect(attachment.mime).toBe("message/");

        part = { mime: "message/rfc822", fileName: "" };
        attachment = create(part, status);
        expect(attachment.mime).toBe("message/rfc822");
    });
});
