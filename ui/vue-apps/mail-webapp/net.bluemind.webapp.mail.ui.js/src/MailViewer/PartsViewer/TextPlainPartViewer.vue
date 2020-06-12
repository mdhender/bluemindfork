<template>
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div class="text-plain-part-viewer" v-html="toHtml" />
</template>

<script>
import { mailText2Html } from "@bluemind/email";

export default {
    name: "TextPlainPartViewer",
    props: {
        value: {
            type: String,
            required: true
        }
    },
    computed: {
        toHtml() {
            return mailText2Html(this.value);
        }
    }
};
</script>

<style lang="scss">
.text-plain-part-viewer {
    .reply {
        margin-left: 1rem;
        padding-left: 1rem;
        border-left: 2px solid black;
    }
    .forwarded {
        margin-left: 1rem;
        padding-left: 1rem;
        color: purple;
    }
    pre {
        white-space: pre-line;
    }

    font-family: "Montserrat", sans-serif;
    font-size: 0.75rem;
    font-weight: 400;
    color: #2f2f2f;
    margin: 0;
}
</style>
