<script>
import { provide } from "vue";
import BmButtonToolbar from "../buttons/BmButtonToolbar";

export default {
    name: "BmToolbarInternal",
    extends: BmButtonToolbar,
    setup() {
        provide("$context", "toolbar");
    }
};
</script>
