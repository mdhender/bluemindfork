<template>
    <mail-open-in-popup-with-shift v-slot="action" :href="routeToForwardEventMessage">
        <bm-toolbar-icon-dropdown
            icon="forward"
            variant="regular-accent"
            right
            split
            :size="size"
            :title="action.label($t('event.forward'))"
            @click.stop="action.execute(() => gotToForwarEventMessage())"
        >
            <slot />
        </bm-toolbar-icon-dropdown>
    </mail-open-in-popup-with-shift>
</template>

<script>
import { mapGetters } from "vuex";
import router from "@bluemind/router";
import { messageUtils } from "@bluemind/mail";
import { BmToolbarIconDropdown } from "@bluemind/ui-components";
import { MY_DRAFTS } from "~/getters";
import { MailRoutesMixin } from "~/mixins";
import MailOpenInPopupWithShift from "~/components/MailOpenInPopupWithShift";
import MessagePathParam from "~/router/MessagePathParam";
const { MessageCreationModes } = messageUtils;

export default {
    components: { MailOpenInPopupWithShift, BmToolbarIconDropdown },
    mixins: [MailRoutesMixin],
    props: {
        size: {
            type: String,
            default: "md"
        },
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS }),
        routeToForwardEventMessage() {
            const messagepath = this.draftPath(this.MY_DRAFTS);
            const query = {
                action: MessageCreationModes.FORWARD,
                message: MessagePathParam.build("", this.message),
                event: true
            };
            return router.relative({ name: "mail:message", params: { messagepath }, query });
        }
    },
    methods: {
        gotToForwarEventMessage() {
            router.push(this.routeToForwardEventMessage);
        }
    }
};
</script>
