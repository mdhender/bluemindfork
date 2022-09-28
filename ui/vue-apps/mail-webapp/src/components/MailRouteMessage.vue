<template>
    <mail-message-panel class="mail-route-message" />
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { draftUtils, loadingStatusUtils, messageUtils } from "@bluemind/mail";

import MessagePathParam from "~/router/MessagePathParam";
import { ACTIVE_MESSAGE, MY_MAILBOX, SELECTION_IS_EMPTY } from "~/getters";
import {
    RESET_PARTS_DATA,
    SET_ACTIVE_MESSAGE,
    SET_CURRENT_CONVERSATION,
    SET_MESSAGE_COMPOSING,
    UNSELECT_ALL_CONVERSATIONS,
    RESET_ACTIVE_MESSAGE,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import { FETCH_MESSAGE_IF_NOT_LOADED } from "~/actions";
import { WaitForMixin, ComposerInitMixin } from "~/mixins";
import MailMessagePanel from "./MailThread/MailMessagePanel";

const { isNewMessage } = draftUtils;
const { LoadingStatus } = loadingStatusUtils;
const { MessageCreationModes } = messageUtils;

export default {
    name: "MailRouteMessage",
    components: { MailMessagePanel },
    mixins: [ComposerInitMixin, WaitForMixin],
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapState("root-app", ["identities"]),
        ...mapGetters("mail", { ACTIVE_MESSAGE, MY_MAILBOX, SELECTION_IS_EMPTY })
    },
    watch: {
        "$route.params.messagepath": {
            async handler(messagepath) {
                try {
                    const { folderKey, internalId } = MessagePathParam.parse(messagepath, this.activeFolder);
                    this.UNSET_CURRENT_CONVERSATION();

                    if (
                        this.ACTIVE_MESSAGE?.remoteRef.internalId === internalId &&
                        this.ACTIVE_MESSAGE?.folderRef.key === folderKey
                    ) {
                        // Preserve state if ACTIVE_MESSAGE is already synced with current route
                        return;
                    }
                    this.RESET_PARTS_DATA();
                    this.RESET_ACTIVE_MESSAGE();
                    if (!this.SELECTION_IS_EMPTY) {
                        this.UNSELECT_ALL_CONVERSATIONS();
                    }

                    let assert = mailbox => mailbox && mailbox.loading === LoadingStatus.LOADED;
                    await this.$waitFor(MY_MAILBOX, assert);
                    let assertIdentities = identitiesCount => identitiesCount > 0;
                    await this.$waitFor(() => this.identities.length, assertIdentities);
                    let message;
                    const folder = this.folders[folderKey];
                    const { action, message: related } = this.$route.query || {};
                    if (isNewMessage({ remoteRef: { internalId } })) {
                        if (action && related) {
                            message = await this.initRelatedMessage(folder, action, MessagePathParam.parse(related));
                        } else {
                            message = await this.initNewMessage(folder);
                        }
                    } else {
                        message = await this.FETCH_MESSAGE_IF_NOT_LOADED({ internalId, folder });
                        if (action === MessageCreationModes.EDIT) {
                            this.SET_MESSAGE_COMPOSING({ messageKey: message.key, composing: true });
                        }
                    }
                    if (message) {
                        this.SET_ACTIVE_MESSAGE(message);
                    }
                } catch (e) {
                    this.$router.push({ name: "mail:home" });
                    throw e;
                }
            },
            immediate: true
        },
        "ACTIVE_MESSAGE.remoteRef.internalId"(value) {
            const { internalId } = MessagePathParam.parse(this.$route.params.messagepath, this.activeFolder);
            if (value !== internalId) {
                this.$router.navigate({ name: "v:mail:message", params: { message: this.ACTIVE_MESSAGE } });
            }
        }
    },
    methods: {
        ...mapMutations("mail", {
            RESET_ACTIVE_MESSAGE,
            RESET_PARTS_DATA,
            SET_ACTIVE_MESSAGE,
            SET_CURRENT_CONVERSATION,
            SET_MESSAGE_COMPOSING,
            UNSELECT_ALL_CONVERSATIONS,
            UNSET_CURRENT_CONVERSATION
        }),
        ...mapActions("mail", { FETCH_MESSAGE_IF_NOT_LOADED })
    }
};
</script>
