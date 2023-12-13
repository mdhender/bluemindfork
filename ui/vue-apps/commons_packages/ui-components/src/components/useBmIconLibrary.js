export default function useBmIconLibrary() {
    let library = {};
    const iconFiles = require.context("../icons", false, /\.svg$/);
    iconFiles.keys().forEach(fileName => {
        const iconName = fileName.replace(/\.\/(.*)\.svg/, "$1");
        library[iconName] = iconFiles(fileName);
    });
    return library;
}
