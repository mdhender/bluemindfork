<template>
    <div class="mail-viewer-from text-truncate">
        <mail-contact-card-slots
            :component="Contact"
            :contact="message.from"
            :no-avatar="noAvatar"
            avatar-size="md"
            class="text-truncate"
            transparent
            bold-dn
            enable-card
        />
        <i18n v-if="showSender" path="mail.content.sender_suffix" class="sender-suffix text-neutral">
            <template #name>
                <mail-contact-card-slots
                    :component="Contact"
                    :contact="sender"
                    no-avatar
                    class="text-truncate"
                    transparent
                    bold-dn
                    enable-card
                />
            </template>
        </i18n>
        <bm-extension id="webapp.mail" path="viewer.sender.suffix" :message="message" class="d-flex" />
    </div>
</template>

<script>
import { MessageBody } from "@bluemind/backend.mail.api";
import { Contact } from "@bluemind/business-components";
import { guessName } from "@bluemind/contact";
import { BmExtension } from "@bluemind/extensions.vue";
import MailContactCardSlots from "../MailContactCardSlots";

export default {
    name: "MailViewerFrom",
    components: { BmExtension, MailContactCardSlots },
    props: {
        message: { type: Object, required: true },
        noAvatar: { type: Boolean, default: false }
    },
    data() {
        return { Contact };
    },
    computed: {
        sender() {
            return {
                address: this.message.sender.address,
                dn: this.message.sender.dn || guessName(this.message.sender.address)
            };
        },
        showSender() {
            return this.message.sender?.address && this.message.sender.address != this.message.from.address;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-viewer-from {
    display: flex;
    align-items: center;
    .contact {
        margin-right: $sp-3;
    }
    > * {
        flex: 0 1 auto;
        min-width: 0;
    }
    .sender-suffix {
        display: flex;
        margin-right: $sp-5;
        .contact {
            margin-left: $sp-3;
            margin-right: 0;
        }
    }
}
</style>
