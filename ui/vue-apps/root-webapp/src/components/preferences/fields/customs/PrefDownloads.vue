<template>
    <div class="pref-downloads">
        <h1 v-if="downloads.length === 0">
            <em class="text-neutral">{{ $t("preferences.downloads.none") }}</em>
        </h1>
        <div v-else class="row">
            <pref-download v-for="(download, index) in downloads" :key="index" class="col-5 m-3" :download="download" />
        </div>
    </div>
</template>

<script>
import { mapExtensions } from "@bluemind/extensions";
import { inject } from "@bluemind/inject";
import PrefDownload from "./PrefDownload";
import BaseField from "../../mixins/BaseField";

export default {
    name: "PrefDownloads",
    components: { PrefDownload },
    mixins: [BaseField],
    data() {
        return {
            downloads: mapExtensions("net.bluemind.ui.settings.downloads", ["download"]).download.filter(hasRole)
        };
    }
};
function hasRole({ role }) {
    return !role || new RegExp(`\\b${role}\\b`, "i").test(inject("UserSession").roles);
}
</script>
