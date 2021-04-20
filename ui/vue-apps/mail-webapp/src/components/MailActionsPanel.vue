<template>
    <mail-thread v-if="ONE_MESSAGE_SELECTED" />
    <mail-multiple-selection-actions v-else-if="MULTIPLE_MESSAGE_SELECTED" />
    <mail-message-starter v-else />
</template>

<script>
import MailMessageStarter from "./MailMessageStarter";
import MailMultipleSelectionActions from "./MailMultipleSelectionActions";
import MailThread from "./MailThread/MailThread";
import { mapGetters, mapMutations, mapState } from "vuex";
import { MULTIPLE_MESSAGE_SELECTED, ONE_MESSAGE_SELECTED } from "~getters";
import { RESET_ACTIVE_MESSAGE, SET_ACTIVE_MESSAGE } from "~mutations";

export default {
    name: "MailActionsPanel",
    components: {
        MailMessageStarter,
        MailMultipleSelectionActions,
        MailThread
    },
    computed: {
        ...mapState("mail", ["selection"]),
        ...mapGetters("mail", { ONE_MESSAGE_SELECTED, MULTIPLE_MESSAGE_SELECTED })
    },
    watch: {
        ONE_MESSAGE_SELECTED: {
            handler: function (value) {
                if (value) {
                    this.SET_ACTIVE_MESSAGE({ key: this.selection[0] });
                } else {
                    this.RESET_ACTIVE_MESSAGE();
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", { RESET_ACTIVE_MESSAGE, SET_ACTIVE_MESSAGE })
    }
};
</script>
