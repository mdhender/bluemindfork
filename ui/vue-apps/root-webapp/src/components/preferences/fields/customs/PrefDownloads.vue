<template>
    <div class="pref-downloads">
        <h1 v-if="options.downloads.length === 0">
            <em class="text-secondary">{{ $t("preferences.downloads.none") }}</em>
        </h1>
        <div v-for="twoDownloads in chunk(options.downloads, 2)" :key="twoDownloads[0].title" class="row">
            <pref-download
                v-for="(download, index) in twoDownloads"
                :key="download.title"
                class="col-6"
                :class="index % 2 === 0 ? 'pr-3' : 'pl-3'"
                :download="download"
            />
        </div>
    </div>
</template>

<script>
import chunk from "lodash.chunk";
import PrefFieldMixin from "../../mixins/PrefFieldMixin";
import PrefDownload from "./PrefDownload";

export default {
    name: "PrefDownloads",
    components: { PrefDownload },
    mixins: [PrefFieldMixin],
    methods: { chunk }
};
</script>
