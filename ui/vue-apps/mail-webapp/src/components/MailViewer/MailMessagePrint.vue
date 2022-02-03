<template>
    <main class="mail-message-print">
        <inline-style>{{ STYLES }}</inline-style>
        <mail-viewer-content :message="message" :expand-attachments="true" :expand-quotes="true">
            <template v-slot:text-html="scope">
                <text-html-part-viewer v-bind="scope" />
            </template>
        </mail-viewer-content>
    </main>
</template>
<script>
import brokenImageIcon from "~/../assets/brokenImageIcon.png";
import InlineStyle from "../InlineStyle.vue";
import MailViewerContent from "./MailViewerContent.vue";
import TextHtmlPartViewer from "./PartsViewer/TextHtmlPartViewer.vue";
export default {
    name: "MailMessagePrint",
    components: { MailViewerContent, InlineStyle, TextHtmlPartViewer },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data: () => ({ STYLES })
};

const STYLES = `
/* Iframe CSS */
body {
    font-family: "Montserrat", sans-serif;
    font-size: 0.75rem;
    font-weight: 400;
    color: #1f1f1f;
    margin: 0;
    overflow-wrap: break-word !important;
    padding: 5mm;
}
main * {
    max-width: 100%;
}
pre {
    font-family: monospace;
    white-space: pre-wrap;
}
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
    color: #2f2f2f;
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
    text-align: start;
    white-space: nowrap;
    font-family: Montserrat;
    font-style: normal;
    font-weight: normal;
    font-size: 12px;
}
a img.blocked-image:before {
    color: #00aaeb !important;
    text-decoration-line: underline;
}
blockquote {
    width: unset !important;
    margin-inline-end: unset !important;
}
/* Bootstrap CSS  */
.px-4 {
    margin-bottom: 1.25rem;
}
.d-flex {
    display: flex;
}
.col-8 {
    flex: 0 0 66.66667%;
    max-width: 66.66667%;
}
.text-secondary {
    color: #595959 !important;
}
.text-right {
    text-align: right !important;
}
.align-self-center {
    align-self: center !important;
}
.col-4 {
    flex: 0 0 33.33333%;
    max-width: 33.33333%;
}
.font-weight-bold {
    font-weight: 600 !important;
}
.bm-icon {
    width: 16px;
    margin-right: 8px;
}
/* MailViewer CSS  */
.mail-message-print {
    color: #2f2f2f;
}
.mail-viewer-splitter {
    display: none;
}
.body-viewer {
    display: flex;
    flex-direction: column;
}
h1.subject {
    font-size: 18px;
    font-weight: 300;
    margin: 1rem 0;
}
.bm-avatar {
    display: none;
}
.mail-attachments-block {
    margin-top: 4rem;
    order: 1;
}
.mail-attachments-block button {
    display: none;
}
.mail-attachments-block > div:first-child {
    border-bottom: 1px solid black;
    margin-bottom: 1rem;
    padding-bottom: 0.25rem;
}
.attachment-text br {
    display: none;
}
.attachment-text span {
    padding-right: 1rem;
}
.mail-attachment-item a {
    display: none;
}
.mail-attachment-item .row {
    display: flex;
    margin: 0.75rem 0;
}
.mail-attachment-item svg.preview-file-type {
    display: none;
}
`;
</script>

