<template>
    <mail-open-in-popup-with-shift v-slot="action" :href="{ name: 'mail:popup:message', params: { messagepath } }">
        <bm-floating-action-button
            v-if="mobile"
            class="new-message d-lg-none"
            :icon="icon"
            :title="action.label()"
            @click="openComposer"
        />
        <bm-button
            v-else
            variant="fill-accent"
            class="new-message text-nowrap d-none d-lg-inline-flex"
            :size="full ? 'lg' : 'md'"
            :title="action.label()"
            :icon="action.icon(icon)"
            @click="action.execute(openComposer)"
        >
            {{ label }}
        </bm-button>
    </mail-open-in-popup-with-shift>
</template>

<script>
import { BmButton, BmFloatingActionButton } from "@bluemind/ui-components";
import { mapGetters } from "vuex";
import { MY_DRAFTS, MY_TEMPLATES } from "~/getters";
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
            default: false
        },
        template: {
            type: Boolean,
            default: false
        },
        full: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        ...mapGetters("mail", { MY_DRAFTS, MY_TEMPLATES }),
        messagepath() {
            return this.draftPath(this.template ? this.MY_TEMPLATES : this.MY_DRAFTS);
        },
        icon() {
            return this.template ? "plus-document" : "plus";
        },
        label() {
            if (this.template) {
                return this.$t(this.full ? "mail.actions.new_template_full" : "mail.actions.new_template");
            } else {
                return this.$t(this.full ? "mail.main.new_full" : "mail.main.new");
            }
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
@import "~@bluemind/ui-components/src/css/utils/variables";

.bm-button.btn-md.new-message {
    flex: 1 1 auto;
}

.bm-floating-action-button.new-message {
    position: fixed;
    bottom: $sp-6;
    right: $sp-6;
    z-index: 110;
}
</style>
