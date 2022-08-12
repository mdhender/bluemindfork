<template>
    <bm-icon-button
        :href="url"
        target="_blank"
        :title="$t('banner.help')"
        class="align-self-center"
        variant="regular-on-fill-primary"
        size="sm"
        icon="interrogation"
    />
</template>

<script>
import { BmIconButton } from "@bluemind/styleguide";

export default {
    name: "BmBannerHelp",
    components: { BmIconButton },
    props: {
        url: {
            required: true,
            type: String
        }
    }
};
</script>
