<template>
    <text-html-part-viewer v-slot="content" v-bind="$props">
        <i-frame class="border-0">
            <template v-slot:head>
                <base target="_blank" />
            </template>
            <template v-slot:style>
                {{ IFRAME_STYLE }}
                {{ content.styles }}
            </template>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <main v-html="content.html"></main>
        </i-frame>
    </text-html-part-viewer>
</template>
<script>
import IFrame from "../../IFrame";
import PartViewerMixin from "./PartViewerMixin";
import TextHtmlPartViewer from "./TextHtmlPartViewer";

export default {
    name: "IframedTextHtmlPartViewer",
    components: { TextHtmlPartViewer, IFrame },
    mixins: [PartViewerMixin],
    data() {
        return { IFRAME_STYLE };
    }
};
const IFRAME_STYLE = `
        @import url('/webapp/css/montserrat/index.css');

        body {
            font-family: "Montserrat", sans-serif;
            font-size: 0.75rem;
            font-weight: 400;
            color: #1f1f1f;
            margin: 0;
            overflow-wrap: break-word !important;
        }

        `;
</script>
