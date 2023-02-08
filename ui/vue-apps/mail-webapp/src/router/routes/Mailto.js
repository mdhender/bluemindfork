export default {
    name: "mail:mailto",
    path: "/mail/:mailto(mailto.*)",
    redirect: to => ({ name: "mail:message", query: mailtoUrlToMessageQuery(to.params.mailto) })
};

function mailtoUrlToMessageQuery(mailtoUrl) {
    const url = new URL(mailtoUrl);
    const mailto = url.pathname;
    const to = url.searchParams.get("to");
    return {
        to: mailto ? (to ? `${mailto}, ${to}` : mailto) : to,
        cc: url.searchParams.get("cc") || undefined,
        bcc: url.searchParams.get("bcc") || undefined,
        subject: url.searchParams.get("subject") || undefined,
        body: url.searchParams.get("body") || undefined
    };
}
