<template>
    <div class="pref-section-icon">
        <bm-avatar v-if="section.id === 'my_account'" :alt="userDisplayName" :urn="urn" />
        <bm-app-icon v-else :icon-app="section.icon" class="text-secondary" />
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { BmAvatar } from "@bluemind/styleguide";
import BmAppIcon from "../BmAppIcon";

export default {
    name: "PrefSectionIcon",
    components: { BmAppIcon, BmAvatar },
    props: {
        section: {
            type: Object,
            required: true
        }
    },
    data() {
        const session = inject("UserSession");
        return {
            userDisplayName: session.formatedName,
            urn: session.userId + "@addressbook_" + session.domain
        };
    }
};
</script>

<style lang="scss">
@import "./_variables";
@import "~@bluemind/styleguide/css/_variables";

.pref-section-icon {
    width: $section-icon-size;
    height: $section-icon-size;

    .bm-app-icon svg {
        width: $section-icon-size;
        height: $section-icon-size;
    }
    .bm-avatar {
        margin: auto;
    }
}
</style>
