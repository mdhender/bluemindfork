import { EmptyTransformer } from "@bluemind/html-utils";

export default class {
    constructor(transformer) {
        this.transformer = transformer || new EmptyTransformer();
    }

    transform(text) {
        return this.transformer.transform(processForward(text));
    }
}

function processForward(text) {
    const forwardRegex = /-{8,9}.+-{8,9}/g;
    const i = text.search(forwardRegex);
    if (i !== -1) {
        text =
            text.substring(0, i - 1) +
            "<blockquote class='forwarded'>" +
            text.substring(i, text.length) +
            "</blockquote>";
    }
    return text;
}
