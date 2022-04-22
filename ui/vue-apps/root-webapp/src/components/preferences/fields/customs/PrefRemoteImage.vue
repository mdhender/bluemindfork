<template>
    <div v-if="!collapsed" class="ml-4 text-secondary">
        <bm-icon icon="info-circle" class="mr-1 mt-1" />
        <pref-read-more-label :href="readMore" :text="$t('preferences.mail.remote.images.explanations')" />
    </div>
</template>

<script>
import BaseField from "../../mixins/BaseField";
import { BmIcon } from "@bluemind/styleguide";
import PrefReadMoreLabel from "../PrefReadMoreLabel";

export default {
    name: "PrefRemoteImage",
    components: { BmIcon, PrefReadMoreLabel },
    mixins: [BaseField],

    data() {
        return {
            readMore:
                "https://forge.bluemind.net/confluence/display/DA/.Consulter+les+messages+vBM-4#id-.ConsulterlesmessagesvBM4-Gestiondesimagesdistantes"
        };
    }
};
</script>
