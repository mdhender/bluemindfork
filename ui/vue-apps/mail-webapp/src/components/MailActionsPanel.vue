<template>
    <mail-thread v-if="ONE_MESSAGE_SELECTED" />
    <mail-multiple-selection-actions v-else-if="MULTIPLE_MESSAGE_SELECTED" />
    <mail-message-starter v-else />
</template>

<script>
import MailMessageStarter from "./MailMessageStarter";
import MailMultipleSelectionActions from "./MailMultipleSelectionActions";
import MailThread from "./MailThread/MailThread";
import { mapActions, mapGetters, mapState } from "vuex";
import { MULTIPLE_MESSAGE_SELECTED, ONE_MESSAGE_SELECTED } from "../store/types/getters";

export default {
    name: "MailActionsPanel",
    components: {
        MailMessageStarter,
        MailMultipleSelectionActions,
        MailThread
    },
    computed: {
        ...mapGetters("mail", { ONE_MESSAGE_SELECTED, MULTIPLE_MESSAGE_SELECTED }),
        ...mapState("mail", ["selection"])
    },
    watch: {
        selection: {
            handler: function () {
                if (this.ONE_MESSAGE_SELECTED) {
                    this.selectMessage(this.selection[0]);
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
