<template>
    <bm-button :href="url" target="_blank" :title="$t('banner.help')" class="align-self-center" variant="inline-light">
        <bm-icon icon="interrogation" size="lg" />
    </bm-button>
</template>

<script>
import { BmButton, BmIcon } from "@bluemind/styleguide";

export default {
    name: "BmBannerHelp",
    components: { BmIcon, BmButton },
    props: {
        url: {
            required: true,
            type: String
        }
    }
};
</script>
