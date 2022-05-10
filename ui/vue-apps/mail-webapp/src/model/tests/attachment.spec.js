import Decorator from "../attachment/FileHostingDecorator";

const attachment = {
    mime: "application/octet-stream",
    headers: [],
    size: 0
};

const bmDispositionAttachment = {
    ...attachment,
    headers: [
        {
            name: "X-BM-Disposition",
            values: [
                "filehosting;url=https://bm.blue-mind.net/fh/bm-fh/id;name=example.jpeg;size=5596767;mime=video/mp4"
            ]
        }
    ]
};

const mozCloudPartAttachment = {
    ...attachment,
    headers: [
        {
            name: "X-Mozilla-Cloud-Part",
            values: ["cloudFile;url=https://bm.blue-mind.net/fh/bm-fh/id;name=example.jpeg"]
        }
    ]
};
describe("Attachment decorator", () => {
    describe("type property", () => {
        test("filehosting when there is a x-bm-disposition header", () => {
            const decoratedAttachment = Decorator.decorate(bmDispositionAttachment);
            expect(decoratedAttachment.type).toBe("filehosting");
        });
        test("add filehosting when there is a x-mozilla-cloud-part header", () => {
            const decoratedAttachment = Decorator.decorate(mozCloudPartAttachment);
            expect(decoratedAttachment.type).toBe("filehosting");
        });
        test("undefined type property whithout these headers", () => {
            const decoratedAttachment = Decorator.decorate(attachment);
            expect(decoratedAttachment.type).toBeUndefined();
        });
    });

    describe("extra property", () => {
        test("extract data from header in extra property when there is a x-bm-disposition header", () => {
            const decoratedAttachment = Decorator.decorate(bmDispositionAttachment);
            expect(decoratedAttachment.extra).toStrictEqual({
                url: "https://bm.blue-mind.net/fh/bm-fh/id",
                fileName: "example.jpeg",
                size: "5596767",
                mime: "video/mp4"
            });
        });
        test("extract data from header in extra property when there is a x-mozilla-cloud-part header", () => {
            const decoratedAttachment = Decorator.decorate(mozCloudPartAttachment);
            expect(decoratedAttachment.extra).toStrictEqual({
                url: "https://bm.blue-mind.net/fh/bm-fh/id",
                fileName: "example.jpeg"
            });
        });
        test("undefined extra property whithout these headers", () => {
            const decoratedAttachment = Decorator.decorate(attachment);
            expect(decoratedAttachment.extra).toBeUndefined();
        });
    });
});
