<template>
    <iframe-container v-if="value" :body="parsed.sanitizedBody" :styles="parsed.styles" />
    <mail-viewer-content-loading v-else />
</template>

<script>
import { sanitizeHtml } from "@bluemind/html-utils";
import linkifyHtml from "linkifyjs/html";
import IframeContainer from "./IframeContainer";
import MailViewerContentLoading from "../MailViewerContentLoading";

export default {
    name: "TextHtmlPartViewer",
    components: {
        IframeContainer,
        MailViewerContentLoading
    },
    props: {
        value: {
            type: String,
            required: false,
            default: undefined
        }
    },
    computed: {
        parsed() {
            const root = new DOMParser().parseFromString(this.value, "text/html");

            const styleNotInBody = extractStyleNotInBody(root);

            return {
                sanitizedBody: linkifyHtml(sanitizeHtml(root.body.innerHTML, true)),
                styles: styleNotInBody
            };
        }
    }
};

function extractStyleNotInBody(doc) {
    let result = "";

    let rootStyle = doc.documentElement.getAttribute("style");
    if (rootStyle) {
        result += " body { " + rootStyle + "} ";
    }

    let bodyStyle = doc.body.getAttribute("style");
    if (bodyStyle) {
        result += " body { " + bodyStyle + "} ";
    }

    const headStyle = [...doc.head.getElementsByTagName("style")].reduce(
        (all, current) => all + " " + current.innerText,
        ""
    );

    result += " " + headStyle;

    return result;
}
</script>
