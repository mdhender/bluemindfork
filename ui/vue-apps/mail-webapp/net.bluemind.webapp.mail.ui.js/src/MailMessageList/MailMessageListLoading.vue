<template functional>
    <bm-list-group-item class="mail-message-list-loading" active-class="active" />
</template>

<script>
import { BmListGroupItem } from "@bluemind/styleguide";

export default {
    name: "MailMessageListLoading",
    components: {
        BmListGroupItem
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.mail-message-list-loading {
    cursor: pointer;
    height: 4rem;
}

.mail-message-list-loading:focus {
    outline: none;
}
</style>
