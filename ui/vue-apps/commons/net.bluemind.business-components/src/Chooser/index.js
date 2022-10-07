import store from "@bluemind/store";
import { chooserStore } from "./store/store";
export { default as Chooser } from "./Chooser";

store.registerModule("chooser", chooserStore);
