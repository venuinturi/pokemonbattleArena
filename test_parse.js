const fs = require('fs');
const html = fs.readFileSync('target/mapguide_dump.html', 'utf8');
if (html.includes('Route') || html.includes('Town') || html.includes('City')) {
    console.log("Found common map names in dump");
} else {
    console.log("No common map names found in dump. It's likely completely an image or iframe with obfuscated text.");
}
