<template>
    <iframe-container :value="sanitized" />
</template>

<script>
import { sanitizeHtml } from "@bluemind/html-utils";
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
        sanitized() {
            return sanitizeHtml(this.value);
        }
    }
};
</script>
