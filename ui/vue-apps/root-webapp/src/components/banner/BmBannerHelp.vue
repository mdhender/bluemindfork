<template>
    <a :href="url" target="_blank" :title="$t('banner.reach.help')" class="align-self-center mr-4 btn btn-inline-light">
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
