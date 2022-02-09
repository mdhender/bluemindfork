<template>
    <mail-viewer-content-loading v-if="content === undefined" />
    <div v-else class="text-html-part-viewer">
        <slot :html="html" :styles="styles">
            <inline-style>{{ styles }}</inline-style>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div v-html="html"></div>
        </slot>
        <bm-button
            v-if="isCollapseActive"
            variant="outline-dark"
            class="align-self-start ml-3 mb-2"
            @click="collapse_ = false"
        >
            <bm-icon icon="3dots" size="sm" />
        </bm-button>
    </div>
</template>

<script>
import linkifyHtml from "linkifyjs/html";
import { MimeType, InlineImageHelper } from "@bluemind/email";
import { sanitizeHtml, blockRemoteImages } from "@bluemind/html-utils";
import { BmButton, BmIcon } from "@bluemind/styleguide";

import brokenImageIcon from "~/../assets/brokenImageIcon.png";
import { QUOTE_NODES } from "~/getters";
import { isForward } from "~/model/message";
import { getPartsFromCapabilities, VIEWER_CAPABILITIES } from "~/model/part";
import QuoteHelper from "~/store/helpers/QuoteHelper";
import InlineStyle from "~/components/InlineStyle";

import MailViewerContentLoading from "../MailViewerContentLoading";
import PartViewerMixin from "./PartViewerMixin";

export default {
    name: "TextHtmlPartViewer",
    components: { BmButton, BmIcon, MailViewerContentLoading, InlineStyle },
    mixins: [PartViewerMixin],
    props: { collapse: { type: Boolean, default: true } },
    data() {
        return { collapse_: this.collapse && !isForward(this.message) };
    },
    computed: {
        quoteNodes() {
            return this.$store.getters[`mail/${QUOTE_NODES}`](this.message.key, this.part.address);
        },
        blockImages() {
            return this.$store.state.mail.consultPanel.remoteImages.mustBeBlocked;
        },
        content() {
            return this.$store.state.mail.partsData.partsByMessageKey[this.message.key]?.[this.part.address];
        },
        parsedContent() {
            const images = getPartsFromCapabilities(this.message, VIEWER_CAPABILITIES).filter(
                part => MimeType.isImage(part) && part.contentId
            );
            const insertionResult = InlineImageHelper.insertAsUrl(
                [this.content],
                images,
                this.message.folderRef.uid,
                this.message.remoteRef.imapUid
            );
            let html = insertionResult.contentsWithImageInserted[0];
            return new DOMParser().parseFromString(html, "text/html");
        },
        html() {
            let html = this.isCollapseActive
                ? QuoteHelper.removeQuotes(this.parsedContent.cloneNode(true), this.quoteNodes).body.innerHTML
                : this.parsedContent.body.innerHTML;
            html = linkifyHtml(sanitizeHtml(html, true));
            if (this.blockImages) {
                html = blockRemoteImages(html);
            }
            return html;
        },
        styles() {
            return extractStyleNotInBody(this.parsedContent) + BM_STYLE;
        },
        isCollapseActive() {
            return this.collapse_ && this.quoteNodes;
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

const BM_STYLE = `
        img.blocked-image {
            position: relative;
            min-height: 50px;
            min-width: 55px;
            display: inline-block;
            border: solid 1px #727272 !important;
            vertical-align: top;
        }
        .blocked-background { 
            background-image: url(${brokenImageIcon}); 
            background-position: 7px 7px; 
            background-repeat: no-repeat; 
            border: solid 1px #727272 !important; 
        }
        img.blocked-image:before {
            content: attr(alt);
            color: #2F2F2F;
            display: block;
            position: absolute;
            width: 100%;
            height: 100%;
            background: #fff;
            background-image: url(${brokenImageIcon});
            background-repeat: no-repeat;
            background-position: 7px 7px;
            padding: 9px 7px 7px 27px;
            box-sizing: border-box;
            overflow: hidden;
            text-overflow: ellipsis;
            text-align start;
            white-space: nowrap;
            font-family: Montserrat;
            font-style: normal;
            font-weight: normal;
            font-size: 12px;
        }

        a img.blocked-image:before {
            color: #00AAEB !important;
            text-decoration-line: underline;
        }

        blockquote {
            width: unset !important;
            margin-inline-end: unset !important;
        }`;
</script>
