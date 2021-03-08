<template>
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div v-if="value" class="text-plain-part-viewer" v-html="toHtml" />
    <mail-viewer-content-loading v-else />
</template>

<script>
import { mapState } from "vuex";
import { mailText2Html } from "@bluemind/email";
import MailViewerContentLoading from "../MailViewerContentLoading";

export default {
    name: "TextPlainPartViewer",
    components: { MailViewerContentLoading },
    props: {
        value: {
            type: String,
            required: false,
            default: undefined
        }
    },
    computed: {
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        toHtml() {
            return mailText2Html(this.value, this.settings.lang);
        }
    }
};
</script>

<style lang="scss">
.text-plain-part-viewer {
    .reply {
        margin-left: 1rem;
        padding-left: 1rem;
        border-left: 2px solid black;
    }
    .forwarded {
        margin-left: 1rem;
        padding-left: 1rem;
        color: purple;
    }
    pre {
        white-space: pre-line;
        font-family: "Montserrat", sans-serif;
        font-size: 0.75rem;
        font-weight: 400;
        color: #2f2f2f;
    }

    margin: 0;
}
</style>
