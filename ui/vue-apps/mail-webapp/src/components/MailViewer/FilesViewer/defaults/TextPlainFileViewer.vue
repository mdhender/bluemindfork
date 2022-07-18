<template>
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div v-if="content" class="text-plain-file-viewer" v-html="toHtml" />
    <mail-viewer-content-loading v-else />
</template>

<script>
import { mapActions } from "vuex";
import { mailText2Html } from "@bluemind/email";
import MailViewerContentLoading from "../../MailViewerContentLoading";
import FileViewerMixin from "../FileViewerMixin";
import { FETCH_PART_DATA } from "~/actions";

export default {
    name: "TextPlainFileViewer",
    components: { MailViewerContentLoading },
    mixins: [FileViewerMixin],
    $capabilities: ["text/plain", "text/*"],
    data() {
        return {
            content: ""
        };
    },
    computed: {
        lang() {
            return this.$store.state.settings.lang;
        },
        toHtml() {
            return mailText2Html(this.content, this.lang);
        }
    },
    watch: {
        file: {
            async handler() {
                this.content = "";
                if (this.src) {
                    const res = await fetch(this.src);
                    const text = await res.text();
                    this.content = text;
                } else {
                    await this.FETCH_PART_DATA({
                        messageKey: this.message.key,
                        folderUid: this.message.folderRef.uid,
                        imapUid: this.message.remoteRef.imapUid,
                        parts: [this.file]
                    });
                    this.content = this.$store.state.mail.partsData.partsByMessageKey[this.message.key]?.[
                        this.file.address
                    ];
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_PART_DATA })
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.text-plain-file-viewer {
    .reply {
        margin-left: 1rem;
        padding-left: 1rem;
        border-left: 2px solid $highest;
    }
    .forwarded {
        margin-left: 1rem;
        padding-left: 1rem;
        color: $purple;
    }
    pre {
        white-space: pre-line;
        font-family: "Montserrat", sans-serif;
        font-size: 0.75rem;
        font-weight: 400;
        color: $neutral-fg-hi1;
    }

    margin: 0;
}
</style>
