<template>
    <component
        :is="component"
        v-bind="[$attrs, $props]"
        class="mail-contact-card-slots text-truncate"
        v-on="$listeners"
    >
        <template #default>
            <slot />
        </template>
        <template #email="slotProps">
            <router-link
                :to="
                    $router.relative({
                        name: 'mail:message',
                        params: { messagepath: draftPath(MY_DRAFTS, slotProps.email) },
                        query: { to: slotProps.email }
                    })
                "
                class="text-truncate"
                :title="$t('mail.actions.send_message.tooltip', { address: slotProps.email })"
            >
                <strong>{{ slotProps.email }}</strong>
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
    </component>
</template>

<script>
import { mapGetters } from "vuex";
import { ContactActionShow } from "@bluemind/business-components";
import { inject } from "@bluemind/inject";
import { BmButton } from "@bluemind/ui-components";
import UUIDGenerator from "@bluemind/uuid";
import { MY_DRAFTS } from "~/getters";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "MailContactCardSlots",
    components: { BmButton, ContactActionShow },
    mixins: [MailRoutesMixin],
    props: {
        component: { type: Object, required: true }
    },
    data() {
        return { loading: false };
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS })
    },
    methods: {
        async addContact(contact) {
            this.loading = true;
            const uid = UUIDGenerator.generate();
            const containerUid = `book:Contacts_${inject("UserSession").userId}`;
            createName(contact);
            await inject("AddressBookPersistence", containerUid).create(uid, contact.value);
            this.loading = false;
            contact.uid = uid;
            contact.containerUid = containerUid;
        }
    }
};

function createName(contact) {
    if (!contact.value.identification.name && contact.value.identification.formatedName.value) {
        const names = contact.value.identification.formatedName.value.split(/\s+/);
        contact.value.identification.name = {
            givenNames: names[0],
            familyNames: names.slice(1).join(" ")
        };
    }
}
</script>

<style lang="scss">
.mail-contact-card-slots {
    .bm-button .slot-wrapper {
        display: flex;
    }
}
</style>
