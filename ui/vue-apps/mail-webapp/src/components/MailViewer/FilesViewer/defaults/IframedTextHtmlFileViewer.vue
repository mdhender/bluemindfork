<template>
    <text-html-file-viewer v-slot="content" v-bind="$props">
        <i-frame class="border-0">
            <template v-slot:head>
                <base href="/webapp/" />
                <link type="text/css" rel="stylesheet" href="css/montserrat/index.css" />
            </template>
            <template v-slot:style>
                {{ content.styles }}
                {{ IFRAME_STYLE }}
            </template>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <main v-html="content.html"></main>
        </i-frame>
    </text-html-file-viewer>
</template>
<script>
import IFrame from "../../../IFrame";
import FileViewerMixin from "../FileViewerMixin";
import TextHtmlFileViewer from "./../TextHtmlFileViewer";

export default {
    name: "IframedTextHtmlFileViewer",
    components: { TextHtmlFileViewer, IFrame },
    mixins: [FileViewerMixin],
    $capabilities: ["text/html"],
    props: { collapse: { type: Boolean, default: true } },
    data() {
        return { IFRAME_STYLE };
    }
};
const IFRAME_STYLE = `
		html {
            height: auto !important;
        }
        
        body {
            font-family: "Montserrat", sans-serif;
            font-size: 0.875rem;
            font-weight: 400;
            color: #1f1f1f;
            margin: 0;
            overflow-wrap: break-word !important;
            height: auto !important;
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
.text-html-file-viewer {
    .i-frame {
        min-width: 100%;
        max-width: 100%;
    }
}
</style>
