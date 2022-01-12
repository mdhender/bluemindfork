<template>
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div v-if="content !== undefined" class="text-plain-part-viewer" v-html="toHtml" />
    <mail-viewer-content-loading v-else />
</template>

<script>
import { mailText2Html } from "@bluemind/email";
import MailViewerContentLoading from "../MailViewerContentLoading";
import PartViewerMixin from "./PartViewerMixin";

export default {
    name: "TextPlainPartViewer",
    components: { MailViewerContentLoading },
    mixins: [PartViewerMixin],
    computed: {
        lang() {
            return this.$store.state.settings.lang;
        },
        content() {
            return this.$store.state.mail.partsData.partsByMessageKey[this.message.key]?.[this.part.address];
        },
        toHtml() {
            return mailText2Html(this.content, this.lang);
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
