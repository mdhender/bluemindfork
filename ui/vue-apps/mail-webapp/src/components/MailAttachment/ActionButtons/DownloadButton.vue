<template>
    <bm-icon-button
        variant="compact"
        class="download-button"
        size="sm"
        icon="download"
        :title="$t('common.downloadAttachment')"
        :disabled="disabled"
        @click.stop="download"
    />
</template>

<script>
import { BmIconButton } from "@bluemind/ui-components";

export default {
    name: "DownloadButton",
    components: { BmIconButton },
    props: {
        disabled: {
            type: Boolean,
            default: false
        },
        file: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            href: ""
        };
    },
    watch: {
        "file.url"() {
            URL.revokeObjectURL(this.href);
            this.href = "";
        }
    },
    destroyed() {
        URL.revokeObjectURL(this.href);
    },
    methods: {
        // Force download to workaround for Chromium bug which does not pass through service worker when clicking on a HTML <a> link with a download attribute
        // https://bugs.chromium.org/p/chromium/issues/detail?id=468227,
        async download() {
            const res = await fetch(encodeURI(this.file.url));
            const blob = await res.blob();
            this.href = this.href ? this.href : URL.createObjectURL(blob);
            const link = createDownloadLink(this.href, this.file.name);
            link.click();
        }
    }
};

function createDownloadLink(href, name) {
    const link = document.createElement("a");
    link.setAttribute("download", name);
    link.setAttribute("href", href);
    link.setAttribute("target", "_blank");
    return link;
}
</script>
