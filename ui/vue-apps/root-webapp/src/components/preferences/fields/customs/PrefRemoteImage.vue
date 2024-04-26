<template>
    <div class="pref-remote-image">
        <bm-icon icon="info-circle" />
        <bm-read-more :href="readMore" :text="$t('preferences.mail.remote.images.explanations')" />
    </div>
</template>

<script>
import { BmIcon, BmReadMore } from "@bluemind/ui-components";
import { BaseField } from "@bluemind/preferences";

export default {
    name: "PrefRemoteImage",
    components: { BmIcon, BmReadMore },
    mixins: [BaseField],

    data() {
        return {
            readMore:
                "https://doc.bluemind.net/release/5.1/guide_de_l_utilisateur/la_messagerie/consulter_les_messages#g%C3%A9rer-les-images-distantes"
        };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-remote-image {
    display: flex;
    gap: $sp-5;
    color: $neutral-fg-lo1;
}
</style>
