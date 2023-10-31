export default {
    mime: "multipart/alternative",
    children: [
        {
            mime: "text/plain",
            address: "2865b8fd-082f-4f15-b787-dc9a1a06bd08",
            encoding: "quoted-printable",
            charset: "utf-8"
        },
        {
            mime: "multipart/related",
            children: [
                {
                    mime: "text/html",
                    address: "67780d73-14ed-4f79-ae90-2cec770b8008",
                    encoding: "quoted-printable",
                    charset: "utf-8"
                },
                {
                    address: "d018b8e7-61d4-42df-9acd-6a5499634b4f",
                    mime: "image/png",
                    dispositionType: "INLINE",
                    encoding: "base64",
                    contentId: "<37E0D401-EC3D-4A26-BB5E-0F02C2CB7413@bluemind.net>",
                    size: 79360
                }
            ]
        }
    ]
};
