<template>
    <bm-extension
        id="webapp.mail"
        v-slot="context"
        class="preview-file"
        path="message.file"
        type="renderless"
        :file="file"
    >
        <preview-file-content :file="context.file" :message="message" />
    </bm-extension>
</template>

<script>
import { BmExtension } from "@bluemind/extensions.vue";
import { mapState } from "vuex";
import PreviewFileContent from "./PreviewFileContent";

export default {
    name: "PreviewFile",
    components: { BmExtension, PreviewFileContent },
    props: {
        message: {
            type: Object,
            required: true
        },
        file: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            alert: {
                alert: {
                    name: "mail.BLOCK_REMOTE_CONTENT",
                    uid: "BLOCK_REMOTE_CONTENT_PREVIEW",
                    payload: this.message
                },
                options: { area: "preview-right-panel", renderer: "BlockedRemoteContent" }
            }
        };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "preview-right-panel") })
    }
};
</script>
