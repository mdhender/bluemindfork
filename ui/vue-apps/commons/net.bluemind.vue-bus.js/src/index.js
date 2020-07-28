import Client from "./Client";
import { use } from "./use";
import { install } from "./install";

export default {
    use() {
        use.apply(this, arguments);
    },
    install() {
        install.apply(this, arguments);
    },
    Client
};
