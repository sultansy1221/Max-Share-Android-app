const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const vm = require("node:vm");

const webUiPath = path.join(
  __dirname,
  "../app/src/main/java/com/portablefileserver/server/WebUi.kt",
);
const source = readFileSync(webUiPath, "utf8");
const inlineScript = source.match(/<script>\s*([\s\S]*?)<\/script>/);

function extractDeclaration(name) {
  const start = source.indexOf(`function ${name}(`);
  assert.notEqual(start, -1, `Missing ${name} declaration`);
  const openingBrace = source.indexOf("{", start);
  let depth = 0;
  for (let i = openingBrace; i < source.length; i += 1) {
    if (source[i] === "{") depth += 1;
    if (source[i] === "}") depth -= 1;
    if (depth === 0) return source.slice(start, i + 1);
  }
  throw new Error(`Unclosed ${name} declaration`);
}


function getFormatHelpers() {
  const formatMap = source.match(/var FILE_FORMATS=\{[\s\S]*?\n\};/);
  assert.ok(formatMap, "Missing file-format icon map");
  const code = [
    formatMap[0],
    extractDeclaration("ext"),
    extractDeclaration("formatInfo"),
    "({ formatInfo })",
  ].join("\n");
  return vm.runInNewContext(code);
}

function getGalleryHelpers() {
  const typeMap = source.match(/var T=\{[^\n]*\};/);
  assert.ok(typeMap, "Missing browser media type map");
  const code = [
    typeMap[0],
    extractDeclaration("ext"),
    extractDeclaration("kind"),
    extractDeclaration("mediaCategory"),
    extractDeclaration("matchesGalleryFilter"),
    "({ mediaCategory, matchesGalleryFilter })",
  ].join("\n");
  return vm.runInNewContext(code);
}

test("embedded browser JavaScript parses", () => {
  assert.ok(inlineScript, "Missing embedded browser script");
  assert.doesNotThrow(() => new vm.Script(inlineScript[1]));
});

test("browser page exposes list, gallery, and category controls", () => {
  assert.match(source, /id="btnList"/);
  assert.match(source, /id="btnGallery"/);
  for (const category of ["all", "image", "video", "audio", "app"]) {
    assert.match(source, new RegExp(`data-filter="${category}"`));
  }
});

test("media gallery categorizes media, APKs, folders, and other files", () => {
  const { mediaCategory } = getGalleryHelpers();
  assert.equal(mediaCategory({ name: "photo.JPG" }), "image");
  assert.equal(mediaCategory({ name: "clip.mkv" }), "video");
  assert.equal(mediaCategory({ name: "voice.ogg" }), "audio");
  assert.equal(mediaCategory({ name: "client.apk" }), "app");
  assert.equal(
    mediaCategory({ name: "backup.bin", applicationId: "com.example.app" }),
    "app",
  );
  assert.equal(mediaCategory({ name: "movie.mp4", dir: true }), "folder");
  assert.equal(mediaCategory({ name: "notes.pdf" }), "other");
});

test("gallery filters include only matching media and apps", () => {
  const { matchesGalleryFilter } = getGalleryHelpers();
  assert.equal(matchesGalleryFilter({ name: "photo.png" }, "all"), true);
  assert.equal(matchesGalleryFilter({ name: "clip.mp4" }, "all"), true);
  assert.equal(matchesGalleryFilter({ name: "voice.mp3" }, "all"), true);
  assert.equal(matchesGalleryFilter({ name: "tool.apk" }, "all"), true);
  assert.equal(matchesGalleryFilter({ name: "folder", dir: true }, "all"), false);
  assert.equal(matchesGalleryFilter({ name: "notes.pdf" }, "all"), false);
  assert.equal(matchesGalleryFilter({ name: "photo.png" }, "image"), true);
  assert.equal(matchesGalleryFilter({ name: "photo.png" }, "video"), false);
  assert.equal(matchesGalleryFilter({ name: "tool.apk" }, "app"), true);
});

test("file list assigns format icons and visible badges across common and unknown extensions", () => {
  const { formatInfo } = getFormatHelpers();
  const plain = (value) => JSON.parse(JSON.stringify(value));
  assert.deepEqual(plain(formatInfo("photo.JPG")), { path: "image", label: "صورة JPEG", badge: "JPG" });
  assert.deepEqual(plain(formatInfo("archive.zip")), { path: "archive", label: "أرشيف ZIP", badge: "ZIP" });
  assert.deepEqual(plain(formatInfo("page.html")), { path: "code", label: "ملف HTML", badge: "HTML" });
  assert.deepEqual(plain(formatInfo("bundle.js")), { path: "code", label: "ملف JavaScript", badge: "JS" });
  assert.deepEqual(plain(formatInfo("data.cas")), { path: "file", label: "ملف CAS", badge: "CAS" });
  assert.deepEqual(plain(formatInfo("unknown.abcxyz123")), { path: "file", label: "ملف ABCXYZ123", badge: "ABCXYZ" });
  assert.deepEqual(plain(formatInfo("README")), { path: "file", label: "ملف دون امتداد", badge: "" });
  assert.match(source, /el\("span","formatBadge",mark\.badge\)/);
});
