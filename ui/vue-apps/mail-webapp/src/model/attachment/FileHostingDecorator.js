export default {
    decorate(attachment) {
        let header =
            attachment.headers.find(header => header.name.toLowerCase() === "x-bm-disposition") ||
            attachment.headers.find(header => header.name.toLowerCase() === "x-mozilla-cloud-part");
        if (header) {
            return { ...attachment, ...parseHeader(header) };
        }
        return attachment;
    }
};
function parseHeader(header) {
    let { name, ...headers } = Object.fromEntries(
        header.values[0]
            .split(";")
            .slice(1)
            .map(s => s.match(/ *([^=]*)=(.*)/).slice(1, 3))
    );
    return {
        type: "filehosting",
        extra: {
            fileName: name,
            ...headers
        }
    };
}
