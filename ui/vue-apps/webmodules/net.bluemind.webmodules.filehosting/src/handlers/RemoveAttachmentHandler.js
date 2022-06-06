import getContentWithLinks from "./getContentWithLinks";
export default async function ({ message }) {
    const newContent = getContentWithLinks(this, message);
    this.$store.commit("mail/SET_DRAFT_EDITOR_CONTENT", newContent);
}
