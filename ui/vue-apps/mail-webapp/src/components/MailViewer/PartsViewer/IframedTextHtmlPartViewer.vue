<template>
    <text-html-part-viewer v-slot="content" v-bind="$props">
        <i-frame class="border-0">
            <template v-slot:head>
                <base target="_blank" />
                <link type="text/css" rel="stylesheet" href="css/montserrat/index.css" />
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
    props: { collapse: { type: Boolean, default: true } },
    data() {
        return { IFRAME_STYLE };
    }
};
const IFRAME_STYLE = `

        body {
            font-family: "Montserrat", sans-serif;
            font-size: 0.75rem;
            font-weight: 400;
            color: #1f1f1f;
            margin: 0;
            overflow-wrap: break-word !important;
        }

        main * {
            max-width: 100%;
        }
        pre {
            font-family: monospace;
            white-space: pre-wrap;
        }
        `;
</script>
<style lang="scss">
.text-html-part-viewer {
    .i-frame {
        min-width: 100%;
    }
}
</style>
