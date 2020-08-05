<template>
    <mail-thread v-if="selectedMessageKeys.length === 1" />
    <mail-multiple-selection-actions v-else-if="selectedMessageKeys.length > 1" />
    <mail-message-starter v-else />
</template>

<script>
import MailMessageStarter from "./MailMessageStarter";
import MailMultipleSelectionActions from "./MailMultipleSelectionActions";
import MailThread from "./MailThread/MailThread";
import { mapActions, mapState } from "vuex";

export default {
    name: "MailActionsPanel",
    components: {
        MailMessageStarter,
        MailMultipleSelectionActions,
        MailThread
    },
    computed: {
        ...mapState("mail-webapp", ["selectedMessageKeys"])
    },
    watch: {
        selectedMessageKeys: {
            handler: function() {
                if (this.selectedMessageKeys.length === 1) {
                    this.selectMessage(this.selectedMessageKeys[0]);
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["selectMessage"])
    }
};
</script>
