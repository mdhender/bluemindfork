<template>
    <div v-if="value !== undefined">
        <iframe-container :body="parsed.sanitizedBody" :styles="parsed.styles" :message="message" />
        <bm-button
            v-if="isCollapseActive"
            variant="outline-dark"
            class="align-self-start ml-3 mb-2"
            @click="quotedCollapsed = false"
        >
            <bm-icon icon="3dots" size="sm" />
        </bm-button>
    </div>
    <mail-viewer-content-loading v-else />
</template>

<script>
import { sanitizeHtml } from "@bluemind/html-utils";
import linkifyHtml from "linkifyjs/html";
import IframeContainer from "./IframeContainer";
import MailViewerContentLoading from "../MailViewerContentLoading";
import { BmButton, BmIcon } from "@bluemind/styleguide";
import { mapGetters } from "vuex";
import { QUOTE_NODES } from "~/getters";
import QuoteHelper from "../../../store/helpers/QuoteHelper";

export default {
    name: "TextHtmlPartViewer",
    components: { BmButton, BmIcon, IframeContainer, MailViewerContentLoading },
    props: {
        value: {
            type: String,
            required: false,
            default: undefined
        },
        message: {
            type: Object,
            required: true
        },
        partAddress: {
            type: [Number, String],
            required: true
        }
    },
    data: () => ({ quotedCollapsed: true }),
    computed: {
        ...mapGetters("mail", { QUOTE_NODES }),
        quoteNodes() {
            return this.QUOTE_NODES(this.message.key, this.partAddress);
        },
        parsed() {
            const root = new DOMParser().parseFromString(this.value, "text/html");
            const styleNotInBody = extractStyleNotInBody(root);
            const body = this.isCollapseActive
                ? QuoteHelper.removeQuotes(root, this.quoteNodes).body.innerHTML
                : root.body.innerHTML;
            const sanitizedBody = linkifyHtml(sanitizeHtml(body, true));
            return { sanitizedBody, styles: styleNotInBody };
        },
        isCollapseActive() {
            return this.quotedCollapsed && this.quoteNodes;
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
