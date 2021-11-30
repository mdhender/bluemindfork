<template>
    <bm-avatar v-if="section.id === 'my_account'" :alt="userDisplayName" :urn="urn" class="pref-section-icon" />
    <bm-app-icon v-else :icon-app="section.icon" class="pref-section-icon text-primary" />
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

<style>
.pref-section-icon.bm-avatar {
    font-size: 1rem;
}
</style>
