import injector from "@bluemind/inject";

const env = injector.getProvider("Environment").get();
const firstDayOfWeek = (env && env.firstDayOfWeek) || 1;
const values = ["SU", "MO", "TU", "WE", "TH", "FR"];
export default values.slice(firstDayOfWeek - 1).concat(values.slice(0, firstDayOfWeek - 1));
