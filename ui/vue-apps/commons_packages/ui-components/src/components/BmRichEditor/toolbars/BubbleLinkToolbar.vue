<template>
    <bm-toolbar class="bm-rich-editor-toolbar-base bubble-link-toolbar">
        <bm-button variant="link" class="link" icon="popup" @click="open">
            {{ url }}
        </bm-button>

        <bm-button variant="text" icon="pencil" @click="$emit('open-link-modal')">
            {{ $t("common.edit") }}
        </bm-button>
        <bm-button variant="text" icon="unlink" @click.stop="removeLink(editor)">
            {{ $t("styleguide.rich_editor.link.unlink") }}
        </bm-button>
    </bm-toolbar>
</template>

<script>
import { removeLink } from "roosterjs-editor-api";
import { QueryScope } from "roosterjs-editor-types";

import BmButton from "../../buttons/BmButton";
import BmToolbar from "../../BmToolbar/BmToolbar";

export default {
    name: "BubbleLinkToolbar",
    components: { BmButton, BmToolbar },
    props: {
        editor: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            url: null
        };
    },
    mounted() {
        this.setUrl();
    },
    methods: {
        setUrl() {
            const link = this.editor.queryElements("a[href]", QueryScope.OnSelection)[0];
            if (link) {
                this.url = link.href;
            }
        },
        open() {
            window.open(this.url);
        },
        removeLink
    }
};
</script>

<style lang="scss">
@import "../../../css/utils/variables";

.bm-rich-editor-bubble-toolbar {
    white-space: nowrap;

    .bm-button {
        padding: $sp-5 $sp-3 !important;
    }

    .link {
        max-width: 20em;
        justify-content: start;
    }
}
</style>
