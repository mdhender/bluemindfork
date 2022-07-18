<template>
    <bm-extension
        id="webapp.mail"
        v-slot="context"
        class="preview-file"
        path="message.file"
        type="renderless"
        :file="file"
    >
        <preview-file-content :file="context.file" :message="message" @remote-content="setBlockRemote" />
    </bm-extension>
</template>

<script>
import { mapActions, mapMutations, mapState } from "vuex";
import { BmExtension } from "@bluemind/extensions.vue";
import { WARNING } from "@bluemind/alert.store";
import apiAddressbooks from "~/store/api/apiAddressbooks";
import { SET_BLOCK_REMOTE_IMAGES } from "~/mutations";
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
    },
    methods: {
        ...mapActions("alert", { WARNING }),
        ...mapMutations("mail", {
            SET_BLOCK_REMOTE_IMAGES
        }),
        async setBlockRemote() {
            if (this.$store.state.mail.consultPanel.remoteImages.mustBeBlocked) {
                const { total } = await apiAddressbooks.search(this.message.from.address);
                if (total === 0) {
                    this.WARNING(this.alert);
                } else {
                    this.SET_BLOCK_REMOTE_IMAGES(false);
                }
            }
        }
    }
};
</script>
