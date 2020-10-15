<template>
    <iframe-container :body="parsed.sanitizedBody" :styles="parsed.styles" />
</template>

<script>
import { sanitizeHtml } from "@bluemind/html-utils";
import linkifyHtml from "linkifyjs/html";
import IframeContainer from "./IframeContainer";

export default {
    name: "TextHtmlPartViewer",
    components: {
        IframeContainer
    },
    props: {
        value: {
            type: String,
            required: true
        }
    },
    computed: {
        parsed() {
            const root = new DOMParser().parseFromString(this.value, "text/html");

            const rootStyle = root.documentElement.getAttribute("style") || "";
            const bodyStyle = root.body.getAttribute("style") || "";
            const headStyle = [...root.head.getElementsByTagName("style")].reduce(
                (all, current) => all + " " + current.innerText,
                ""
            );

            return {
                sanitizedBody: sanitizeHtml(linkifyHtml(root.body.innerHTML), true),
                styles: rootStyle + bodyStyle + headStyle
            };
        }
    }
};
</script>
