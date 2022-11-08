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
    </contact>
</template>

<script>
import { mapGetters } from "vuex";
import { Contact } from "@bluemind/business-components";
import { MY_DRAFTS } from "~/getters";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "MailContact",
    components: { Contact },
    mixins: [MailRoutesMixin],
    computed: {
        ...mapGetters("mail", { MY_DRAFTS })
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
