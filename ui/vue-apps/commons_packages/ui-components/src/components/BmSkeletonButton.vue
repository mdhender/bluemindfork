<script>
import BmSkeleton from "./BmSkeleton";

export default {
    name: "BmSkeletonButton",
    extends: BmSkeleton,
    props: {
        type: {
            required: false,
            type: String,
            default: "button"
        }
    }
};
</script>
