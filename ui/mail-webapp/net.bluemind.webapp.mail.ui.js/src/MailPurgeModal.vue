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
        BmModal
    },
    data() {
        return {
            messageKey: null
        };
    },
    computed: {
        ...mapState("mail-webapp", ["currentFolderUid", "currentMessageKey"]),
        ...mapGetters("mail-webapp", ["nextMessageKey"])
    },
    bus: {
        [SHOW_PURGE_MODAL]: function(optMessageKey) {
            this.messageKey = optMessageKey ? optMessageKey : this.currentMessageKey;
            if (this.messageKey) {
                this.$refs["purge-modal"].show();
            }
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["purge"]),
        closeModal() {
            this.$refs["purge-modal"].hide();
            this.messageKey = null;
        },
        deletionConfirmed() {
            if (this.currentMessageKey == this.messageKey) {
                this.$router.push("" + (this.nextMessageKey || ""));
            }
            this.purge(this.messageKey);
            this.closeModal();
        }
    }
};
</script>
