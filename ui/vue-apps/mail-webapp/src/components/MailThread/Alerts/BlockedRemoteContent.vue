<template>
    <div>
        {{ $t("mail.content.alert.images.blocked") }}
        &nbsp;
        <bm-button variant="link" @click="unblockImages()">{{ $t("mail.content.alert.images.show") }}</bm-button>
        <br />
        <bm-button variant="link" @click="trustSender()">
            {{ $t("mail.content.alert.images.trust.sender", { sender: payload.from.address }) }}
        </bm-button>
    </div>
</template>
<script>
import { mapActions, mapGetters, mapMutations } from "vuex";
import { AlertMixin, REMOVE } from "@bluemind/alert.store";
import { RecipientAdaptor, VCardAdaptor } from "@bluemind/contact";
import { inject } from "@bluemind/inject";
import { BmButton } from "@bluemind/ui-components";
import { SET_BLOCK_REMOTE_IMAGES } from "~/mutations";
import { ACTIVE_MESSAGE } from "~/getters";

export default {
    name: "BlockedRemoteContent",
    components: { BmButton },
    mixins: [AlertMixin],
    computed: {
        ...mapGetters("mail", { ACTIVE_MESSAGE })
    },
    methods: {
        ...mapMutations("mail", { SET_BLOCK_REMOTE_IMAGES }),
        ...mapActions("alert", { REMOVE }),
        unblockImages() {
            this.SET_BLOCK_REMOTE_IMAGES(false);
            this.remove();
        },
        trustSender() {
            this.SET_BLOCK_REMOTE_IMAGES();
            const contact = RecipientAdaptor.toContact(this.ACTIVE_MESSAGE.from);
            const collectedContactsUid = "book:CollectedContacts_" + inject("UserSession").userId;
            inject("AddressBookPersistence", collectedContactsUid).create(contact.uid, VCardAdaptor.toVCard(contact));
            this.remove();
        },
        remove() {
            this.REMOVE(this.alert);
        }
    }
};
</script>
