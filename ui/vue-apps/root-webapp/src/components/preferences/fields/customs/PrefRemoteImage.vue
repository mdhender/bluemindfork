<template>
    <div v-if="!collapsed" class="pref-remote-image">
        <bm-icon icon="info-circle" />
        <pref-read-more :href="readMore" :text="$t('preferences.mail.remote.images.explanations')" />
    </div>
</template>

<script>
import BaseField from "../../mixins/BaseField";
import { BmIcon } from "@bluemind/styleguide";
import PrefReadMore from "../PrefReadMore";

export default {
    name: "PrefRemoteImage",
    components: { BmIcon, PrefReadMore },
    mixins: [BaseField],

    data() {
        return {
            readMore:
                "https://doc.bluemind.net/release/5.0/guide_de_l_utilisateur/la_messagerie/consulter_les_messages#g%C3%A9rer-les-images-distantes"
        };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-remote-image {
    display: flex;
    gap: $sp-5;
    color: $neutral-fg-lo1;
}
</style>
