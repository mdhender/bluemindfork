<template>
    <mail-open-in-popup-with-shift v-slot="action" :href="{ name: 'mail:popup:message', params: { messagepath } }">
        <bm-floating-action-button
            v-if="mobile"
            class="new-message d-lg-none"
            icon="plus"
            :title="action.label()"
            @click="action.execute(openComposer)"
        />
        <bm-button
            v-else
            variant="contained-accent"
            class="new-message text-nowrap d-none d-lg-inline-flex"
            :size="size"
            :title="action.label()"
            :icon="action.icon('plus')"
            @click="action.execute(openComposer)"
        >
            {{ $t("mail.main.new") }}
        </bm-button>
    </mail-open-in-popup-with-shift>
</template>
<script>
import { BmButton, BmFloatingActionButton } from "@bluemind/styleguide";
import { mapGetters } from "vuex";
import { MY_DRAFTS } from "~/getters";
import { MailRoutesMixin } from "~/mixins";
import MailOpenInPopupWithShift from "./MailOpenInPopupWithShift";

export default {
    name: "NewMessage",
    components: {
        BmButton,
        BmFloatingActionButton,
        MailOpenInPopupWithShift
    },
    mixins: [MailRoutesMixin],
    props: {
        mobile: {
            type: Boolean,
            required: false,
            default: false
        },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["md", "lg"].includes(value);
            }
        }
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS }),
        messagepath() {
            return this.draftPath(this.MY_DRAFTS);
        }
    },
    methods: {
        openComposer() {
            this.$router.navigate({ name: "mail:message", params: { messagepath: this.messagepath } });
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.bm-floating-action-button.new-message {
    position: absolute;
    bottom: $sp-6;
    right: $sp-6;
    z-index: $zindex-fixed;
}
</style>
