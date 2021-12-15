<template>
    <div class="ml-4 text-secondary d-flex">
        <bm-icon icon="info-circle" class="mr-1 mt-1 align-items-start" />
        <span>
            {{ $t("preferences.mail.remote.images.explanations") }}
            <!-- <a href="https://forge.bluemind.net/confluence/display/BM4/La+messagerie" target="_blank">
                {{ $t("preferences.mail.remote.images.help") }}
            </a> -->
        </span>
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";

export default {
    name: "PrefRemoteImage",
    components: { BmIcon }
};
</script>
