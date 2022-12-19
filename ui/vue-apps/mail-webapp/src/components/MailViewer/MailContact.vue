<template>
    <contact v-bind="[$attrs, $props]" class="mail-contact">
        <template #email="slotProps">
            <router-link
                :to="
                    $router.relative({
                        name: 'mail:message',
                        params: { messagepath: draftPath(MY_DRAFTS, slotProps.email) },
                        query: { to: slotProps.email }
                    })
                "
            >
                {{ slotProps.email }}
            </router-link>
        </template>
        <template #actions="slotProps">
            <contact-action-show
                v-if="slotProps.contact.uid && slotProps.contact.containerUid"
                ref="action"
                :contact="slotProps.contact"
            />
            <bm-button
                v-else
                ref="action"
                variant="text-accent"
                icon="user-add"
                :title="$t('mail.actions.add_contact.tooltip')"
                :loading="loading"
                href="#"
                @click="addContact(slotProps.contact)"
            >
                {{ $t("mail.actions.add_contact") }}
            </bm-button>
        </template>
    </contact>
</template>

<script>
import { mapGetters } from "vuex";
import { Contact, ContactActionShow } from "@bluemind/business-components";
import { inject } from "@bluemind/inject";
import { BmButton } from "@bluemind/ui-components";
import UUIDGenerator from "@bluemind/uuid";
import { MY_DRAFTS } from "~/getters";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "MailContact",
    components: { BmButton, Contact, ContactActionShow },
    mixins: [MailRoutesMixin],
    data() {
        return { loading: false };
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS })
    },
    updated() {
        this.focusAction();
    },
    methods: {
        async addContact(contact) {
            this.loading = true;
            const uid = UUIDGenerator.generate();
            const containerUid = `book:Contacts_${inject("UserSession").userId}`;
            await inject("AddressBookPersistence", containerUid).create(uid, contact.value);
            this.loading = false;
            contact.uid = uid;
            contact.containerUid = containerUid;
            this.focusAction();
        },
        focusAction() {
            this.$nextTick(() => this.$refs["action"]?.$el.focus());
        }
    }
};
</script>

<style lang="scss">
.mail-contact {
    .bm-button .slot-wrapper {
        display: flex;
    }
}
</style>
