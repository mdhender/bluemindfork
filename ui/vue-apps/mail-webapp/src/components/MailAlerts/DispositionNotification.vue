<template>
    <div class="disposition-notification d-flex flex-fill justify-content-between">
        <div>
            <i18n path="alert.mail.disposition_notification.info" :title="payload.to.address">
                <template #sender>
                    <strong class="text-neutral">{{ payload.to.dn || payload.to.address }}</strong>
                </template>
            </i18n>
            <br />
            <span>{{ $t("alert.mail.disposition_notification.info.action") }}</span>
            <router-link
                class="d-none d-lg-inline-block"
                :to="`${$route.path}${prefPath}`"
                @click.native="$store.commit('preferences/TOGGLE_PREFERENCES')"
            >
                {{ $t("alert.mail.disposition_notification.info.pref") }}
            </router-link>
        </div>
        <div class="d-flex align-items-center justify-content-around">
            <bm-button variant="text" class="mx-4" @click="send">{{ $t("common.send") }}</bm-button>
            <bm-button variant="text" class="mx-4" @click="ignore">{{ $t("common.ignore") }}</bm-button>
            <bm-icon-button
                class="d-lg-none"
                icon="preferences"
                variant="compact"
                @click.native="
                    $router.push(`${$route.path}${prefPath}`);
                    $store.commit('preferences/TOGGLE_PREFERENCES');
                "
            />
        </div>
    </div>
</template>
<script>
import { AlertMixin, ERROR, REMOVE, SUCCESS } from "@bluemind/alert.store";
import { Flag } from "@bluemind/email";
import { BmButton, BmIconButton } from "@bluemind/ui-components";
import { ADD_FLAG } from "~/actions";
import sendTemplate from "~/utils/eml-templates/sendTemplate";
import MDNTemplate from "~/utils/eml-templates/templates/MDNTemplate";
import { messageUtils } from "@bluemind/mail";

export default {
    name: "DispositionNotification",
    components: { BmButton, BmIconButton },
    mixins: [AlertMixin],
    data() {
        return { prefPath: "#preferences-mail-main" };
    },
    methods: {
        async ignore() {
            await this.$store.dispatch(`mail/${ADD_FLAG}`, { messages: [this.payload.message], flag: Flag.MDN_SENT });
            this.$store.dispatch(`alert/${REMOVE}`, this.alert);
        },
        async send() {
            const alert = {
                name: "mail.mdn_sent",
                uid: "MDN_SENT_UID",
                payload: { recipient: this.payload.to.dn || this.payload.to.address }
            };
            try {
                await sendTemplate({
                    template: MDNTemplate,
                    parameters: {
                        subject: this.payload.message.subject,
                        date: this.payload.message.date.toLocaleString(),
                        from: this.payload.from.dn
                            ? this.payload.from.dn + " <" + this.payload.from.address + ">"
                            : this.payload.from.address,
                        to: this.payload.to.dn
                            ? this.payload.to.dn + " <" + this.payload.to.address + ">"
                            : this.payload.to.address,
                        toAddress: this.payload.to.address,
                        messageId: this.payload.message.messageId
                    },
                    from: this.payload.from,
                    to: this.payload.to,
                    outboxUid: this.payload.outbox.remoteRef.uid,
                    additionalHeaders: [
                        { name: messageUtils.MessageHeader.REFERENCES, values: [this.payload.message.messageId] }
                    ]
                });
                await this.ignore();
                this.$store.dispatch(`alert/${SUCCESS}`, { alert });
            } catch (e) {
                this.$store.dispatch(`alert/${ERROR}`, { alert });
                throw e;
            }
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/mixins/responsiveness";

.disposition-notification {
    @include until-lg {
        flex-direction: column;
    }
}
</style>
