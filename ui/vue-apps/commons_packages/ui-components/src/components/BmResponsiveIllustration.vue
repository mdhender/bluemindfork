<template>
    <div class="bm-responsive-illustration">
        <bm-illustration class="d-flex d-lg-none" v-bind="[$attrs, $props]" size="xxs" v-on="$listeners" />
        <bm-illustration class="d-none d-lg-flex mx-5" v-bind="[$attrs, $props]" size="xs" v-on="$listeners" />
    </div>
</template>

<script>
import omit from "lodash.omit";

import BmIllustration from "./BmIllustration";

export default {
    name: "BmResponsiveIllustration",
    components: { BmIllustration },
    props: omit(BmIllustration.props, "size")
};
</script>
