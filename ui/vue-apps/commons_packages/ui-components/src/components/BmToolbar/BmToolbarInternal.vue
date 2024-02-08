<script>
import { provide } from "vue";
import { BButtonToolbar } from "bootstrap-vue";

export default {
    name: "BmToolbarInternal",
    extends: BButtonToolbar,
    setup() {
        provide("$context", "toolbar");
    }
};
</script>
