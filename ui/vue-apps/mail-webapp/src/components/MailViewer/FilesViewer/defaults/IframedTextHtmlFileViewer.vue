<template>
    <text-html-file-viewer v-slot="content" v-bind="$props">
        <i-frame :key="IS_COMPUTED_THEME_DARK" class="border-0">
            <template v-slot:head>
                <base target="_blank" />
                <link type="text/css" rel="stylesheet" href="css/montserrat/index.css" />
            </template>
            <template v-slot:style>
                {{ IS_COMPUTED_THEME_DARK ? darkifyCss(content.styles, darkifyingBaseLvalue()) : content.styles }}
                {{ IFRAME_STYLE }}
            </template>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <main v-bm-darkify="IS_COMPUTED_THEME_DARK" v-html="content.html"></main>
        </i-frame>
    </text-html-file-viewer>
</template>
<script>
import { mapGetters } from "vuex";
import { BmDarkify, darkifyCss, darkifyingBaseLvalue } from "@bluemind/ui-components";
import IFrame from "../../../IFrame";
import FileViewerMixin from "../FileViewerMixin";
import TextHtmlFileViewer from "./../TextHtmlFileViewer";

export default {
    name: "IframedTextHtmlFileViewer",
    components: { TextHtmlFileViewer, IFrame },
    directives: { BmDarkify },
    mixins: [FileViewerMixin],
    $capabilities: ["text/html"],
    props: { collapse: { type: Boolean, default: true } },
    data() {
        return { IFRAME_STYLE };
    },
    computed: {
        ...mapGetters("settings", ["IS_COMPUTED_THEME_DARK"])
    },
    methods: {
        darkifyCss,
        darkifyingBaseLvalue
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
            color: var(--neutral-fg-hi1);
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
