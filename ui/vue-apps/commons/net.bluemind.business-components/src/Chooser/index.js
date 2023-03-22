import store from "@bluemind/store";
import { chooserStore } from "./store/store";
export { default as Chooser } from "./Chooser";

if (!store.hasModule("chooser")) {
    store.registerModule("chooser", chooserStore);
}
