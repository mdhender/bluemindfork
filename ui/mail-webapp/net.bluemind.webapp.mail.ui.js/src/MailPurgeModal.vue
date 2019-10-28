<template>
    <bm-modal 
        ref="purge-modal"
        centered
        :ok-title="$t('common.delete')"
        :cancel-title="$t('common.cancel')"
        :title="$t('mail.actions.purge.modal.title')"
        @ok="deletionConfirmed"
        @cancel="closeModal"
        @close="closeModal"
    >
        <div>{{ $t("mail.actions.purge.modal.content") }}</div>
    </bm-modal>
</template>

<script>
import { BmModal } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState } from "vuex";
import { SHOW_PURGE_MODAL } from "./VueBusEventTypes";

export default {
    name: "MailPurgeModal",
    components: {
        BmModal,
    },
    data() {
        return {
            messageId: null
        };
    },
    computed: {
        ...mapState("mail-webapp", ["currentFolderUid", "currentMessageId"]),
        ...mapGetters("mail-webapp", ["nextMessageId"])
    },
    bus: {
        [SHOW_PURGE_MODAL]: function(optMessageId) {
            this.messageId = optMessageId ? optMessageId : this.currentMessageId;
            if (this.messageId) {
                this.$refs["purge-modal"].show();
            }
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["purge"]),
        closeModal() {
            this.$refs["purge-modal"].hide();
            this.messageId = null;
        },
        deletionConfirmed() {
            if (this.currentMessageId == this.messageId) {
                this.$router.push("" + (this.nextMessageId || ""));
            }
            this.purge({ messageId: this.messageId, folderUid: this.currentFolderUid });
            this.closeModal();
        }
    }
};
</script>