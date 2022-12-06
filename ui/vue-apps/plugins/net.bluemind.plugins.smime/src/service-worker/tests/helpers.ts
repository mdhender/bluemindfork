import fs from "fs";
import path from "path";

export function readFile(filename: string) {
    return fs.readFileSync(path.join(__dirname, `./data/${filename}`), "utf8");
}
