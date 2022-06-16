<template>
    <bm-button
        v-bm-clipping="mobile ? 'hexagon' : undefined"
        variant="secondary"
        class="new-message"
        :class="
            mobile
                ? 'd-lg-none position-absolute new-message-responsive-btn z-index-110'
                : 'text-nowrap d-lg-inline-block d-none'
        "
        @click="openComposer()"
    >
        <bm-icon v-if="mobile" icon="plus" size="2x" />
        <bm-label-icon v-else icon="plus">{{ $t("mail.main.new") }}</bm-label-icon>
    </bm-button>
</template>
<script>
import { BmButton, BmClipping, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import { mapGetters } from "vuex";
import { MY_DRAFTS } from "~/getters";
import { MailRoutesMixin } from "~/mixins";

export default {
    name: "NewMessage",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon
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
        ...mapGetters("mail", { MY_DRAFTS }),
        messagepath() {
            return this.draftPath(this.MY_DRAFTS);
        }
    },
    methods: {
        openComposer() {
            const openInPopup = this.$store.state.settings.openInPopup || true;
            const params = { messagepath: this.messagepath };
            if (!openInPopup) {
                this.$router.navigate({ name: "mail:message", params });
            } else {
                const route = this.$router.resolve({ name: "mail:popup:message", params });
                const composer = window.open(route.href, "", getWindowFeature());
                composer.focus();
            }
        }
    }
};
function getWindowFeature() {
    const height = 800;
    const width = 1100;
    const left = screen.availWidth / 2 - width / 2 + window.screenLeft;
    const top = screen.availHeight / 2 - height / 2 + window.screenTop;
    return `popup=true,width=${width},height=${height},top=${top},left=${left}`;
}
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.new-message {
    &.new-message-responsive-btn {
        bottom: $sp-2;
        right: $sp-2;
        height: 4em;
        width: 4em;
    }
}
</style>
