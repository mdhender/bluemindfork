<template>
    <mail-open-in-popup-with-shift v-slot="action" :href="{ name: 'mail:popup:message', params: { messagepath } }">
        <bm-button
            variant="fill-accent"
            class="new-message text-nowrap d-none d-lg-inline-flex"
            :title="action.label()"
            :icon="action.icon('plus-document')"
            @click="action.execute(openComposer)"
        >
            {{ $t("mail.actions.new_template") }}
        </bm-button>
    </mail-open-in-popup-with-shift>
</template>
<script>
import { BmButton, BmClipping } from "@bluemind/ui-components";
import { mapGetters } from "vuex";
import { MY_TEMPLATES } from "~/getters";
import { MailRoutesMixin } from "~/mixins";
import MailOpenInPopupWithShift from "./MailOpenInPopupWithShift";

export default {
    name: "NewTemplate",
    components: {
        BmButton,
        MailOpenInPopupWithShift
    },
    directives: { BmClipping },
    mixins: [MailRoutesMixin],
    props: {
        mobile: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        ...mapGetters("mail", { MY_TEMPLATES }),
        messagepath() {
            return this.draftPath(this.MY_TEMPLATES);
        }
    },
    methods: {
        openComposer() {
            this.$router.navigate({ name: "mail:message", params: { messagepath: this.messagepath } });
        }
    }
};
</script>
