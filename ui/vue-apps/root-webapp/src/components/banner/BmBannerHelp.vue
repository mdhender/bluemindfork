<template>
    <a :href="url" target="_blank" :title="$t('banner.help')" class="align-self-center btn btn-inline-light">
        <bm-icon icon="interrogation" size="lg" />
    </a>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";

export default {
    name: "BmBannerHelp",
    components: { BmIcon },
    props: {
        url: {
            required: true,
            type: String
        }
    }
};
</script>
