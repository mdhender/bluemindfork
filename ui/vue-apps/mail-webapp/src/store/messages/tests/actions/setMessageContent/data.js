export const htmlContentWithInline = `<div style="font-family: Montserrat, montserrat"><br></div><div data-bm-signature="75D5C3DE-A459-434D-AA9B-3186FC632A37"></div><img src="data:image/png;base64,c2hvcnRlc3Q=" style="max-width: 100%;" data-bm-cid="<6BFEB154-6806-4E66-9B2F-F59C4C013AEC@bluemind.net>" id="imageSelected0"><br>`;
export const simpleDiv = `<div style="font-family: Montserrat, montserrat"></div>`;
export const simpleAlternative = {
    children: [
        {
            address: "tmpAddress",
            charset: "utf-8",
            encoding: "quoted-printable",
            mime: "text/plain"
        },
        {
            address: "tmpAddress",
            charset: "utf-8",
            encoding: "quoted-printable",
            mime: "text/html"
        }
    ],
    mime: "multipart/alternative"
};

export const alternativewithOneRelative = {
    children: [
        {
            address: "tmpAddress",
            charset: "utf-8",
            encoding: "quoted-printable",
            mime: "text/plain"
        },
        {
            children: [
                {
                    address: "tmpAddress",
                    charset: "utf-8",
                    encoding: "quoted-printable",
                    mime: "text/html"
                },
                {
                    address: "tmpAddress",
                    contentId: "<6BFEB154-6806-4E66-9B2F-F59C4C013AEC@bluemind.net>",
                    dispositionType: "INLINE",
                    encoding: "base64",
                    mime: "image/png",
                    size: 8
                }
            ],
            mime: "multipart/related"
        }
    ],
    mime: "multipart/alternative"
};

export const alternativewithTwoAttachments = {
    children: [
        {
            children: [
                {
                    address: "tmpAddress",
                    charset: "utf-8",
                    encoding: "quoted-printable",
                    mime: "text/plain"
                },
                {
                    address: "tmpAddress",
                    charset: "utf-8",
                    encoding: "quoted-printable",
                    mime: "text/html"
                }
            ],
            mime: "multipart/alternative"
        }
    ],
    mime: "multipart/mixed"
};
export const alternativewithTwoAttachmentsOneInline = {
    children: [
        {
            children: [
                {
                    address: "tmpAddress",
                    charset: "utf-8",
                    encoding: "quoted-printable",
                    mime: "text/plain"
                },
                {
                    children: [
                        {
                            address: "tmpAddress",
                            charset: "utf-8",
                            encoding: "quoted-printable",
                            mime: "text/html"
                        },
                        {
                            address: "tmpAddress",
                            contentId: "<6BFEB154-6806-4E66-9B2F-F59C4C013AEC@bluemind.net>",
                            dispositionType: "INLINE",
                            encoding: "base64",
                            mime: "image/png",
                            size: 8
                        }
                    ],
                    mime: "multipart/related"
                }
            ],
            mime: "multipart/alternative"
        }
    ],
    mime: "multipart/mixed"
};
