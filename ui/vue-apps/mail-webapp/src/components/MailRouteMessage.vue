<template>
    <mail-message-panel class="mail-route-message" />
</template>

<script>
import MessagePathParam from "~/router/MessagePathParam";
import { isNewMessage } from "~/model/draft";
import { LoadingStatus } from "~/model/loading-status";
import { MY_MAILBOX, SELECTION_IS_EMPTY } from "~/getters";
import {
    RESET_PARTS_DATA,
    SET_ACTIVE_MESSAGE,
    SET_CURRENT_CONVERSATION,
    UNSELECT_ALL_CONVERSATIONS,
    RESET_ACTIVE_MESSAGE,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import { FETCH_MESSAGE_IF_NOT_LOADED, MARK_CONVERSATIONS_AS_READ } from "~/actions";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import { WaitForMixin, ComposerInitMixin } from "~/mixins";
import MailMessagePanel from "./MailThread/MailMessagePanel";

export default {
    name: "MailRouteMessage",
    components: { MailMessagePanel },
    mixins: [ComposerInitMixin, WaitForMixin],
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", { MY_MAILBOX, SELECTION_IS_EMPTY })
    },
    watch: {
        "$route.params.messagepath": {
            async handler(messagepath, oldMessagepath) {
                try {
                    if (oldMessagepath) {
                        const { internalId: oldInternalId } = MessagePathParam.parse(oldMessagepath, this.activeFolder);
                        if (isNewMessage({ remoteRef: { internalId: oldInternalId } })) {
                            // preserve composer state for 1st save (route is changed only to have a valid route)
                            return;
                        }
                    }

                    this.RESET_PARTS_DATA();
                    this.RESET_ACTIVE_MESSAGE();
                    this.UNSET_CURRENT_CONVERSATION();
                    if (!this.SELECTION_IS_EMPTY) {
                        this.UNSELECT_ALL_CONVERSATIONS();
                    }

                    let assert = mailbox => mailbox && mailbox.loading === LoadingStatus.LOADED;
                    await this.$waitFor(MY_MAILBOX, assert);
                    const { folderKey, internalId } = MessagePathParam.parse(messagepath, this.activeFolder);
                    let message;

                    if (isNewMessage({ remoteRef: { internalId } })) {
                        if (this.$route.query?.action && this.$route.query?.message) {
                            const { action, message: related } = this.$route.query;
                            message = await this.initRelatedMessage(action, MessagePathParam.parse(related));
                        } else {
                            message = this.initNewMessage();
                        }
                    } else {
                        message = await this.FETCH_MESSAGE_IF_NOT_LOADED({
                            internalId,
                            folder: this.folders[folderKey]
                        });
                    }
                    this.SET_ACTIVE_MESSAGE(message);
                } catch (e) {
                    this.$router.push({ name: "mail:home" });
                    throw e;
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapMutations("mail", {
            RESET_ACTIVE_MESSAGE,
            RESET_PARTS_DATA,
            SET_ACTIVE_MESSAGE,
            SET_CURRENT_CONVERSATION,
            UNSELECT_ALL_CONVERSATIONS,
            UNSET_CURRENT_CONVERSATION
        }),
        ...mapActions("mail", { FETCH_MESSAGE_IF_NOT_LOADED, MARK_CONVERSATIONS_AS_READ })
    }
};
</script>
