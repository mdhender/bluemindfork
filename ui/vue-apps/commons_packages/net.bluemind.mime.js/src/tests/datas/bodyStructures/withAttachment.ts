export default {
    mime: "multipart/mixed",
    children: [
        {
            mime: "multipart/alternative",
            children: [
                {
                    mime: "text/plain",
                    address: "64e6458e-d0c1-4fec-8d6e-a455b8880b33",
                    encoding: "quoted-printable",
                    charset: "utf-8"
                },
                {
                    mime: "text/html",
                    address: "c0abfbac-1b5d-4107-b8a4-055ec9740ac5",
                    encoding: "quoted-printable",
                    charset: "utf-8"
                }
            ]
        },
        {
            address: "2bbf0994-026c-4391-ab80-d052fa445ccf",
            key: "CA1BFD9E-6821-4725-B253-0FE5672F9F99",
            charset: "us-ascii",
            encoding: "base64",
            name: "capture.png",
            mime: "image/png",
            size: 79360,
            progress: {
                loaded: 79360,
                total: 79360
            },
            status: "UPLOADED",
            url: "part/url/?folderUid=ba43a5f7-d120-bb9b-8f0c-0aa9e3014b03&imapUid=433&address=2bbf0994-026c-4391-ab80-d052fa445ccf&charset=us-ascii&encoding=base64&mime=image%2Fpng&filename=capture.png",
            dispositionType: "ATTACHMENT",
            fileName: "capture.png",
            filename: "capture.png",
            headers: [
                {
                    name: "Content-Disposition",
                    values: ["attachment;filename=capture.png;size=79360"]
                },
                {
                    name: "Content-Transfer-Encoding",
                    values: ["base64"]
                }
            ]
        }
    ]
};
