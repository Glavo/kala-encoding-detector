# Test Data Catalog

This repository contains character encoding test data for the
[chardet](https://github.com/chardet/chardet) Python library. Each
subdirectory is named `{encoding}` or `{encoding}-{language}` and
contains files encoded in that encoding.

**2521 files** across **729 directories** covering **88 encodings**.

## Sources

Files in this repository come from the following sources, identified by
filename prefix, git history, or content:

| Source | Prefix/Pattern | Files | Description |
|--------|---------------|------:|-------------|
| [CulturaX](https://huggingface.co/datasets/uonlp/CulturaX) | `culturax_` | 1,949 | Multilingual web text from the CulturaX dataset (built on mC4 and OSCAR Common Crawl snapshots). Row indices are preserved in filenames (e.g., `culturax_mC4_84511.txt`, `culturax_OSCAR-2301_58265.txt`). Many files are transcoded copies of the same source text across multiple encodings. |
| [Mark Pilgrim's chardet](https://github.com/puzzlet/chardet/tree/MarkPilgrim/tests) | `*.xml` (domain names) | 314 | Web-scraped RSS/Atom feeds from the original chardet test suite by Mark Pilgrim. Imported by Puzzlet Chung in 2012. Each filename is the source website's domain. |
| [Ude](http://code.google.com/p/ude/) (Universal Detector Engine) | `_ude_` | 96 | Test files from the Ude charset detection library (a C# port of Mozilla's universal charset detector). |
| [charset-normalizer](https://github.com/Ousret/charset_normalizer) ([char-dataset](https://github.com/Ousret/char-dataset)) | various | ~17 | Test data from the charset-normalizer test dataset by Ahmed TAHRI. Iris CSV/JSON datasets originally from [Capital One DataProfiler](https://github.com/capitalone/DataProfiler). UTF-8 `.md`/`.rst` files are READMEs from urllib3 and charset-normalizer. `anzeige-value-stars.html` from charset-normalizer [issue #104](https://github.com/Ousret/charset_normalizer/issues/104). ASCII JSON files (books, parchments, etc.) added to avoid false positives on structured data. `dummy-1.pem` added after [certbot #8964](https://github.com/certbot/certbot/issues/8964). Binary samples ensure non-text is correctly rejected. |
| [ENCA](https://cihar.com/software/enca/) | `_enca_` | 32 | Test files from the ENCA (Extremely Naive Charset Analyser) library. |
| [uchardet](https://www.freedesktop.org/wiki/Software/uchardet/) | `_uchardet_` | 32 | Test files from the uchardet encoding detection library. |
| [Chromium](https://chromium.googlesource.com/chromium/src/) | `_chromium_` | 15 | Test files from the Chromium browser's encoding detection test suite. |
| [Mozilla](https://hg.mozilla.org/mozilla-central/) | `_mozilla_` | 11 | Test files from Mozilla's charset detection test suite, including regression tests for specific bugs (bug numbers in filenames). |
| [Web Archive](https://web.archive.org/) | `archive_` | 11 | Test files sourced from the Wayback Machine web archive. |
| Contributed | various | ~44 | Community contributions: Turkish test files by queeup, CP932 tests by hashy, Johab Korean texts (hlpro-readme, iyagi-readme, mdir-doc), UTF-16/32 plane 1 tests by Jason Zavaglia. |

## Binary Test Files (`None-None/`)

These files are used to test that the detector correctly identifies
binary/non-text content and returns `None`.

| File | Format | Size |
|------|--------|-----:|
| `sample-1.gif` | GIF image | 43 |
| `sample-1.jpg` | JPEG image | 32,436 |
| `sample-1.mp4` | MP4 video | 1,570,024 |
| `sample-1.png` | PNG image | 7,983 |
| `sample-1.webp` | WebP image | 2,938 |
| `sample-1.xlsx` | Excel spreadsheet | 42,669 |
| `sample-2.png` | PNG image | 6,146 |
| `sample-3.png` | PNG image | 14,661 |

## ASCII Test Files

Pure ASCII files for baseline testing.

### `ascii-cy/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78728.txt` | CulturaX | 2,066 |  |

### `ascii-en/` — 12 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_iso-8859-1_with_no_encoding_specified.html` | Chromium | 472 | Chromium encoding detection test (pure ASCII despite ISO-8859-1 label) |
| `_ude_1.txt` | Ude | 2,343 | Ude test vector |
| `book-stats.json` | charset-normalizer | 51 | JSON with book statistics |
| `books.json` | charset-normalizer | 59,650 | Large JSON array of book data |
| `culturax_mC4_84511.txt` | CulturaX | 1,640 |  |
| `culturax_mC4_84513.txt` | CulturaX | 2,533 |  |
| `dummy-1.pem` | charset-normalizer | 3,884 | PEM-encoded certificate |
| `empty.json` | charset-normalizer | 2 | Empty JSON object |
| `howto.diveintomark.org.xml` | chardet | 3,419 | RSS feed from Mark Pilgrim's diveintomark.org (pure ASCII content) |
| `parchments.json` | charset-normalizer | 142 | JSON with parchment data |
| `simple.json` | charset-normalizer | 359 | Simple JSON data |
| `slice.hpp` | unknown | 18,462 |  |

### `ascii-id/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114890.txt` | CulturaX | 2,687 |  |
| `culturax_mC4_114892.txt` | CulturaX | 1,530 |  |

### `ascii-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 227 |  |
| `culturax_00001.txt` | CulturaX | 540 |  |
| `culturax_00002.txt` | CulturaX | 4,066 |  |

## Encoding Directories

Each encoding directory contains files transcoded into that encoding.
Many source texts appear across multiple encoding directories — the same
content transcoded to test detection across encodings.

### Unicode (1356 files in 441 directories)

#### `utf-16-ar/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 4,846 |  |
| `culturax_mC4_98635.txt` | CulturaX | 2,252 |  |
| `culturax_mC4_98638.txt` | CulturaX | 5,944 |  |

#### `utf-16-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77015.txt` | CulturaX | 5,868 |  |
| `culturax_mC4_77016.txt` | CulturaX | 2,378 |  |
| `culturax_mC4_77017.txt` | CulturaX | 4,584 |  |

#### `utf-16-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 2,620 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 2,514 |  |
| `culturax_mC4_84187.txt` | CulturaX | 4,742 |  |

#### `utf-16-br/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_43762.txt` | CulturaX | 1,092 |  |
| `culturax_OSCAR-2019_43764.txt` | CulturaX | 1,258 |  |

#### `utf-16-cs/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 2,726 |  |
| `culturax_mC4_98820.txt` | CulturaX | 2,850 |  |
| `culturax_mC4_98822.txt` | CulturaX | 5,872 |  |

#### `utf-16-cy/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78726.txt` | CulturaX | 5,428 |  |
| `culturax_mC4_78727.txt` | CulturaX | 5,770 |  |
| `culturax_mC4_78728.txt` | CulturaX | 4,134 |  |

#### `utf-16-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 3,838 |  |
| `culturax_mC4_83467.txt` | CulturaX | 5,856 |  |
| `culturax_mC4_83468.txt` | CulturaX | 4,040 |  |

#### `utf-16-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 5,154 |  |
| `culturax_mC4_83755.txt` | CulturaX | 4,316 |  |
| `culturax_mC4_83756.txt` | CulturaX | 4,520 |  |

#### `utf-16-el/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 5,492 |  |
| `culturax_mC4_103810.txt` | CulturaX | 4,264 |  |
| `culturax_mC4_103811.txt` | CulturaX | 2,220 |  |

#### `utf-16-en/` — 9 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 7,048 |  |
| `_ude_2.txt` | Ude | 7,048 |  |
| `bom-utf-16-be.srt` | unknown | 1,714 | BOM detection test subtitle |
| `bom-utf-16-le.srt` | unknown | 1,714 | BOM detection test subtitle |
| `culturax_mC4_84511.txt` | CulturaX | 3,282 |  |
| `culturax_mC4_84512.txt` | CulturaX | 1,700 |  |
| `culturax_mC4_84513.txt` | CulturaX | 5,068 |  |
| `iris-utf-16.csv` | unknown | 10,226 | Iris dataset, originally from Capital One DataProfiler |
| `iris-utf-16.json` | unknown | 38,296 | Iris dataset, originally from Capital One DataProfiler |

#### `utf-16-eo/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_40441.txt` | CulturaX | 5,856 |  |
| `culturax_mC4_40442.txt` | CulturaX | 2,640 |  |
| `culturax_mC4_40443.txt` | CulturaX | 5,266 |  |

#### `utf-16-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 2,928 |  |
| `culturax_mC4_87070.txt` | CulturaX | 5,942 |  |
| `culturax_mC4_87071.txt` | CulturaX | 5,558 |  |

#### `utf-16-et/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66818.txt` | CulturaX | 5,162 |  |
| `culturax_mC4_66819.txt` | CulturaX | 5,562 |  |
| `culturax_mC4_66820.txt` | CulturaX | 2,052 |  |

#### `utf-16-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_104835.txt` | CulturaX | 1,984 |  |
| `culturax_mC4_104836.txt` | CulturaX | 5,054 |  |
| `culturax_mC4_104837.txt` | CulturaX | 3,916 |  |

#### `utf-16-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80361.txt` | CulturaX | 1,734 |  |
| `culturax_mC4_80362.txt` | CulturaX | 6,002 |  |
| `culturax_mC4_80363.txt` | CulturaX | 5,610 |  |

#### `utf-16-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 5,744 |  |
| `culturax_OSCAR-2301_88370.txt` | CulturaX | 1,468 |  |
| `culturax_mC4_88369.txt` | CulturaX | 5,722 |  |

#### `utf-16-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63468.txt` | CulturaX | 5,846 |  |
| `culturax_mC4_63469.txt` | CulturaX | 5,796 |  |
| `culturax_mC4_63470.txt` | CulturaX | 2,446 |  |

#### `utf-16-gd/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 2,018 |  |
| `culturax_00001.txt` | CulturaX | 5,780 |  |
| `culturax_00002.txt` | CulturaX | 13,102 |  |

#### `utf-16-he/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_58265.txt` | CulturaX | 5,976 |  |
| `culturax_OSCAR-2301_58266.txt` | CulturaX | 5,960 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 5,976 |  |

#### `utf-16-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 210 |  |
| `culturax_00001.txt` | CulturaX | 284 |  |
| `culturax_00002.txt` | CulturaX | 1,154 |  |

#### `utf-16-hu/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 3,146 |  |
| `culturax_OSCAR-2301_82420.txt` | CulturaX | 3,166 |  |
| `culturax_mC4_82418.txt` | CulturaX | 1,256 |  |

#### `utf-16-id/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 5,146 |  |
| `culturax_mC4_114890.txt` | CulturaX | 5,376 |  |
| `culturax_mC4_114892.txt` | CulturaX | 3,062 |  |

#### `utf-16-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 5,764 |  |
| `culturax_mC4_77488.txt` | CulturaX | 3,014 |  |
| `culturax_mC4_77489.txt` | CulturaX | 5,618 |  |

#### `utf-16-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 4,758 |  |
| `culturax_mC4_92390.txt` | CulturaX | 2,558 |  |
| `culturax_mC4_92391.txt` | CulturaX | 2,892 |  |

#### `utf-16-ja/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_6.txt` | CulturaX | 1,626 |  |
| `culturax_mC4_4.txt` | CulturaX | 6,002 |  |
| `culturax_mC4_5.txt` | CulturaX | 5,862 |  |

#### `utf-16-kk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 3,910 |  |
| `culturax_mC4_73161.txt` | CulturaX | 1,310 |  |
| `culturax_mC4_73162.txt` | CulturaX | 5,844 |  |

#### `utf-16-ko/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_0.txt` | CulturaX | 3,568 |  |
| `culturax_mC4_1.txt` | CulturaX | 5,794 |  |
| `culturax_mC4_2.txt` | CulturaX | 1,184 |  |

#### `utf-16-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 5,366 |  |
| `culturax_mC4_73446.txt` | CulturaX | 5,368 |  |
| `culturax_mC4_73447.txt` | CulturaX | 6,002 |  |

#### `utf-16-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 5,938 |  |
| `culturax_mC4_71629.txt` | CulturaX | 2,610 |  |
| `culturax_mC4_71630.txt` | CulturaX | 1,534 |  |

#### `utf-16-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 4,728 |  |
| `culturax_mC4_102726.txt` | CulturaX | 2,412 |  |
| `culturax_mC4_102727.txt` | CulturaX | 6,002 |  |

#### `utf-16-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 456 |  |
| `culturax_00001.txt` | CulturaX | 1,082 |  |
| `culturax_00002.txt` | CulturaX | 8,134 |  |

#### `utf-16-mt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_51488.txt` | CulturaX | 3,576 |  |
| `culturax_mC4_51489.txt` | CulturaX | 6,002 |  |
| `culturax_mC4_51490.txt` | CulturaX | 2,196 |  |

#### `utf-16-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_107677.txt` | CulturaX | 2,726 |  |
| `culturax_mC4_107675.txt` | CulturaX | 4,912 |  |
| `culturax_mC4_107676.txt` | CulturaX | 2,088 |  |

#### `utf-16-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 5,006 |  |
| `culturax_mC4_66763.txt` | CulturaX | 6,002 |  |
| `culturax_mC4_66764.txt` | CulturaX | 6,002 |  |

#### `utf-16-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 3,632 |  |
| `culturax_mC4_97060.txt` | CulturaX | 2,602 |  |
| `culturax_mC4_97061.txt` | CulturaX | 4,768 |  |

#### `utf-16-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 1,906 |  |
| `culturax_mC4_101817.txt` | CulturaX | 5,752 |  |
| `culturax_mC4_101818.txt` | CulturaX | 5,676 |  |

#### `utf-16-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_78977.txt` | CulturaX | 1,544 |  |
| `culturax_mC4_78976.txt` | CulturaX | 5,268 |  |
| `culturax_mC4_78978.txt` | CulturaX | 5,456 |  |

#### `utf-16-ru/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 6,002 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 5,664 |  |
| `culturax_mC4_85056.txt` | CulturaX | 2,654 |  |

#### `utf-16-sk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95224.txt` | CulturaX | 2,892 |  |
| `culturax_mC4_95226.txt` | CulturaX | 4,010 |  |
| `culturax_mC4_95227.txt` | CulturaX | 5,738 |  |

#### `utf-16-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 5,786 |  |
| `culturax_mC4_66689.txt` | CulturaX | 5,312 |  |
| `culturax_mC4_66690.txt` | CulturaX | 2,378 |  |

#### `utf-16-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66918.txt` | CulturaX | 2,342 |  |
| `culturax_mC4_66920.txt` | CulturaX | 2,248 |  |
| `culturax_mC4_66921.txt` | CulturaX | 5,262 |  |

#### `utf-16-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 5,468 |  |
| `culturax_mC4_96486.txt` | CulturaX | 5,068 |  |
| `culturax_mC4_96487.txt` | CulturaX | 4,224 |  |

#### `utf-16-tg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_74865.txt` | CulturaX | 3,490 |  |
| `culturax_mC4_74866.txt` | CulturaX | 5,640 |  |
| `culturax_mC4_74867.txt` | CulturaX | 5,708 |  |

#### `utf-16-th/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_109134.txt` | CulturaX | 5,498 |  |
| `culturax_mC4_109133.txt` | CulturaX | 5,980 |  |
| `culturax_mC4_109136.txt` | CulturaX | 2,660 |  |

#### `utf-16-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 2,324 |  |
| `culturax_mC4_107849.txt` | CulturaX | 1,460 |  |
| `culturax_mC4_107850.txt` | CulturaX | 2,680 |  |

#### `utf-16-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95019.txt` | CulturaX | 2,162 |  |
| `culturax_mC4_95020.txt` | CulturaX | 2,744 |  |
| `culturax_mC4_95021.txt` | CulturaX | 5,644 |  |

#### `utf-16-ur/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_82296.txt` | CulturaX | 5,780 |  |
| `culturax_mC4_82297.txt` | CulturaX | 5,886 |  |
| `culturax_mC4_82298.txt` | CulturaX | 2,452 |  |

#### `utf-16-vi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_85695.txt` | CulturaX | 1,342 |  |
| `culturax_mC4_85693.txt` | CulturaX | 5,338 |  |
| `culturax_mC4_85694.txt` | CulturaX | 5,738 |  |

#### `utf-16-zh/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_3.txt` | CulturaX | 4,192 |  |
| `culturax_mC4_5.txt` | CulturaX | 1,290 |  |
| `culturax_mC4_7.txt` | CulturaX | 2,148 |  |

#### `utf-16be-ar/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 4,844 |  |
| `culturax_mC4_98635.txt` | CulturaX | 2,250 |  |
| `culturax_mC4_98638.txt` | CulturaX | 5,942 |  |

#### `utf-16be-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77015.txt` | CulturaX | 5,866 |  |
| `culturax_mC4_77016.txt` | CulturaX | 2,376 |  |
| `culturax_mC4_77017.txt` | CulturaX | 4,582 |  |

#### `utf-16be-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 2,618 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 2,512 |  |
| `culturax_mC4_84187.txt` | CulturaX | 4,740 |  |

#### `utf-16be-br/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_43762.txt` | CulturaX | 1,090 |  |
| `culturax_OSCAR-2019_43764.txt` | CulturaX | 1,256 |  |

#### `utf-16be-cs/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 2,724 |  |
| `culturax_mC4_98820.txt` | CulturaX | 2,848 |  |
| `culturax_mC4_98822.txt` | CulturaX | 5,870 |  |

#### `utf-16be-cy/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78726.txt` | CulturaX | 5,426 |  |
| `culturax_mC4_78727.txt` | CulturaX | 5,768 |  |
| `culturax_mC4_78728.txt` | CulturaX | 4,132 |  |

#### `utf-16be-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 3,836 |  |
| `culturax_mC4_83467.txt` | CulturaX | 5,854 |  |
| `culturax_mC4_83468.txt` | CulturaX | 4,038 |  |

#### `utf-16be-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 5,152 |  |
| `culturax_mC4_83755.txt` | CulturaX | 4,314 |  |
| `culturax_mC4_83756.txt` | CulturaX | 4,518 |  |

#### `utf-16be-el/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 5,490 |  |
| `culturax_mC4_103810.txt` | CulturaX | 4,262 |  |
| `culturax_mC4_103811.txt` | CulturaX | 2,218 |  |

#### `utf-16be-en/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_84511.txt` | CulturaX | 3,280 |  |
| `culturax_mC4_84512.txt` | CulturaX | 1,698 |  |
| `culturax_mC4_84513.txt` | CulturaX | 5,066 |  |
| `nobom-utf16be.txt` | unknown | 1,588 | No-BOM encoding test |
| `plane1-utf-16be.html` | Contributed | 12,504 | Unicode Plane 1 (supplementary) test |

#### `utf-16be-eo/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_40441.txt` | CulturaX | 5,854 |  |
| `culturax_mC4_40442.txt` | CulturaX | 2,638 |  |
| `culturax_mC4_40443.txt` | CulturaX | 5,264 |  |

#### `utf-16be-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 2,926 |  |
| `culturax_mC4_87070.txt` | CulturaX | 5,940 |  |
| `culturax_mC4_87071.txt` | CulturaX | 5,556 |  |

#### `utf-16be-et/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66818.txt` | CulturaX | 5,160 |  |
| `culturax_mC4_66819.txt` | CulturaX | 5,560 |  |
| `culturax_mC4_66820.txt` | CulturaX | 2,050 |  |

#### `utf-16be-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_104835.txt` | CulturaX | 1,982 |  |
| `culturax_mC4_104836.txt` | CulturaX | 5,052 |  |
| `culturax_mC4_104837.txt` | CulturaX | 3,914 |  |

#### `utf-16be-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80361.txt` | CulturaX | 1,732 |  |
| `culturax_mC4_80362.txt` | CulturaX | 6,000 |  |
| `culturax_mC4_80363.txt` | CulturaX | 5,608 |  |

#### `utf-16be-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 5,742 |  |
| `culturax_OSCAR-2301_88370.txt` | CulturaX | 1,466 |  |
| `culturax_mC4_88369.txt` | CulturaX | 5,720 |  |

#### `utf-16be-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63468.txt` | CulturaX | 5,844 |  |
| `culturax_mC4_63469.txt` | CulturaX | 5,794 |  |
| `culturax_mC4_63470.txt` | CulturaX | 2,444 |  |

#### `utf-16be-gd/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 2,016 |  |
| `culturax_00001.txt` | CulturaX | 5,778 |  |
| `culturax_00002.txt` | CulturaX | 13,100 |  |

#### `utf-16be-he/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_58265.txt` | CulturaX | 5,974 |  |
| `culturax_OSCAR-2301_58266.txt` | CulturaX | 5,958 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 5,974 |  |

#### `utf-16be-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 208 |  |
| `culturax_00001.txt` | CulturaX | 282 |  |
| `culturax_00002.txt` | CulturaX | 1,152 |  |

#### `utf-16be-hu/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 3,144 |  |
| `culturax_OSCAR-2301_82420.txt` | CulturaX | 3,164 |  |
| `culturax_mC4_82418.txt` | CulturaX | 1,254 |  |

#### `utf-16be-id/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 5,144 |  |
| `culturax_mC4_114890.txt` | CulturaX | 5,374 |  |
| `culturax_mC4_114892.txt` | CulturaX | 3,060 |  |

#### `utf-16be-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 5,762 |  |
| `culturax_mC4_77488.txt` | CulturaX | 3,012 |  |
| `culturax_mC4_77489.txt` | CulturaX | 5,616 |  |

#### `utf-16be-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 4,756 |  |
| `culturax_mC4_92390.txt` | CulturaX | 2,556 |  |
| `culturax_mC4_92391.txt` | CulturaX | 2,890 |  |

#### `utf-16be-ja/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_6.txt` | CulturaX | 1,624 |  |
| `culturax_mC4_4.txt` | CulturaX | 6,000 |  |
| `culturax_mC4_5.txt` | CulturaX | 5,860 |  |

#### `utf-16be-kk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 3,908 |  |
| `culturax_mC4_73161.txt` | CulturaX | 1,308 |  |
| `culturax_mC4_73162.txt` | CulturaX | 5,842 |  |

#### `utf-16be-ko/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_0.txt` | CulturaX | 3,566 |  |
| `culturax_mC4_1.txt` | CulturaX | 5,792 |  |
| `culturax_mC4_2.txt` | CulturaX | 1,182 |  |

#### `utf-16be-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 5,364 |  |
| `culturax_mC4_73446.txt` | CulturaX | 5,366 |  |
| `culturax_mC4_73447.txt` | CulturaX | 6,000 |  |

#### `utf-16be-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 5,936 |  |
| `culturax_mC4_71629.txt` | CulturaX | 2,608 |  |
| `culturax_mC4_71630.txt` | CulturaX | 1,532 |  |

#### `utf-16be-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 4,726 |  |
| `culturax_mC4_102726.txt` | CulturaX | 2,410 |  |
| `culturax_mC4_102727.txt` | CulturaX | 6,000 |  |

#### `utf-16be-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 454 |  |
| `culturax_00001.txt` | CulturaX | 1,080 |  |
| `culturax_00002.txt` | CulturaX | 8,132 |  |

#### `utf-16be-mt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_51488.txt` | CulturaX | 3,574 |  |
| `culturax_mC4_51489.txt` | CulturaX | 6,000 |  |
| `culturax_mC4_51490.txt` | CulturaX | 2,194 |  |

#### `utf-16be-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_107677.txt` | CulturaX | 2,724 |  |
| `culturax_mC4_107675.txt` | CulturaX | 4,910 |  |
| `culturax_mC4_107676.txt` | CulturaX | 2,086 |  |

#### `utf-16be-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 5,004 |  |
| `culturax_mC4_66763.txt` | CulturaX | 6,000 |  |
| `culturax_mC4_66764.txt` | CulturaX | 6,000 |  |

#### `utf-16be-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 3,630 |  |
| `culturax_mC4_97060.txt` | CulturaX | 2,600 |  |
| `culturax_mC4_97061.txt` | CulturaX | 4,766 |  |

#### `utf-16be-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 1,904 |  |
| `culturax_mC4_101817.txt` | CulturaX | 5,750 |  |
| `culturax_mC4_101818.txt` | CulturaX | 5,674 |  |

#### `utf-16be-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_78977.txt` | CulturaX | 1,542 |  |
| `culturax_mC4_78976.txt` | CulturaX | 5,266 |  |
| `culturax_mC4_78978.txt` | CulturaX | 5,454 |  |

#### `utf-16be-ru/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 6,000 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 5,662 |  |
| `culturax_mC4_85056.txt` | CulturaX | 2,652 |  |

#### `utf-16be-sk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95224.txt` | CulturaX | 2,890 |  |
| `culturax_mC4_95226.txt` | CulturaX | 4,008 |  |
| `culturax_mC4_95227.txt` | CulturaX | 5,736 |  |

#### `utf-16be-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 5,784 |  |
| `culturax_mC4_66689.txt` | CulturaX | 5,310 |  |
| `culturax_mC4_66690.txt` | CulturaX | 2,376 |  |

#### `utf-16be-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66918.txt` | CulturaX | 2,340 |  |
| `culturax_mC4_66920.txt` | CulturaX | 2,246 |  |
| `culturax_mC4_66921.txt` | CulturaX | 5,260 |  |

#### `utf-16be-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 5,466 |  |
| `culturax_mC4_96486.txt` | CulturaX | 5,066 |  |
| `culturax_mC4_96487.txt` | CulturaX | 4,222 |  |

#### `utf-16be-tg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_74865.txt` | CulturaX | 3,488 |  |
| `culturax_mC4_74866.txt` | CulturaX | 5,638 |  |
| `culturax_mC4_74867.txt` | CulturaX | 5,706 |  |

#### `utf-16be-th/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_109134.txt` | CulturaX | 5,496 |  |
| `culturax_mC4_109133.txt` | CulturaX | 5,978 |  |
| `culturax_mC4_109136.txt` | CulturaX | 2,658 |  |

#### `utf-16be-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 2,322 |  |
| `culturax_mC4_107849.txt` | CulturaX | 1,458 |  |
| `culturax_mC4_107850.txt` | CulturaX | 2,678 |  |

#### `utf-16be-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95019.txt` | CulturaX | 2,160 |  |
| `culturax_mC4_95020.txt` | CulturaX | 2,742 |  |
| `culturax_mC4_95021.txt` | CulturaX | 5,642 |  |

#### `utf-16be-ur/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_82296.txt` | CulturaX | 5,778 |  |
| `culturax_mC4_82297.txt` | CulturaX | 5,884 |  |
| `culturax_mC4_82298.txt` | CulturaX | 2,450 |  |

#### `utf-16be-vi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_85695.txt` | CulturaX | 1,340 |  |
| `culturax_mC4_85693.txt` | CulturaX | 5,336 |  |
| `culturax_mC4_85694.txt` | CulturaX | 5,736 |  |

#### `utf-16be-zh/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_3.txt` | CulturaX | 4,190 |  |
| `culturax_mC4_5.txt` | CulturaX | 1,288 |  |
| `culturax_mC4_7.txt` | CulturaX | 2,146 |  |
| `sample_chinese_no_bom.txt` | charset-normalizer | 110 | BOM-less Chinese text (mixed simplified and traditional) |

#### `utf-16le-ar/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 4,844 |  |
| `culturax_mC4_98635.txt` | CulturaX | 2,250 |  |
| `culturax_mC4_98638.txt` | CulturaX | 5,942 |  |

#### `utf-16le-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77015.txt` | CulturaX | 5,866 |  |
| `culturax_mC4_77016.txt` | CulturaX | 2,376 |  |
| `culturax_mC4_77017.txt` | CulturaX | 4,582 |  |

#### `utf-16le-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 2,618 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 2,512 |  |
| `culturax_mC4_84187.txt` | CulturaX | 4,740 |  |

#### `utf-16le-br/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_43762.txt` | CulturaX | 1,090 |  |
| `culturax_OSCAR-2019_43764.txt` | CulturaX | 1,256 |  |

#### `utf-16le-cs/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 2,724 |  |
| `culturax_mC4_98820.txt` | CulturaX | 2,848 |  |
| `culturax_mC4_98822.txt` | CulturaX | 5,870 |  |

#### `utf-16le-cy/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78726.txt` | CulturaX | 5,426 |  |
| `culturax_mC4_78727.txt` | CulturaX | 5,768 |  |
| `culturax_mC4_78728.txt` | CulturaX | 4,132 |  |

#### `utf-16le-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 3,836 |  |
| `culturax_mC4_83467.txt` | CulturaX | 5,854 |  |
| `culturax_mC4_83468.txt` | CulturaX | 4,038 |  |

#### `utf-16le-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 5,152 |  |
| `culturax_mC4_83755.txt` | CulturaX | 4,314 |  |
| `culturax_mC4_83756.txt` | CulturaX | 4,518 |  |

#### `utf-16le-el/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 5,490 |  |
| `culturax_mC4_103810.txt` | CulturaX | 4,262 |  |
| `culturax_mC4_103811.txt` | CulturaX | 2,218 |  |

#### `utf-16le-en/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_84511.txt` | CulturaX | 3,280 |  |
| `culturax_mC4_84512.txt` | CulturaX | 1,698 |  |
| `culturax_mC4_84513.txt` | CulturaX | 5,066 |  |
| `nobom-utf16le.txt` | unknown | 1,588 | No-BOM encoding test |
| `plane1-utf-16le.html` | Contributed | 12,504 | Unicode Plane 1 (supplementary) test |

#### `utf-16le-eo/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_40441.txt` | CulturaX | 5,854 |  |
| `culturax_mC4_40442.txt` | CulturaX | 2,638 |  |
| `culturax_mC4_40443.txt` | CulturaX | 5,264 |  |

#### `utf-16le-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 2,926 |  |
| `culturax_mC4_87070.txt` | CulturaX | 5,940 |  |
| `culturax_mC4_87071.txt` | CulturaX | 5,556 |  |

#### `utf-16le-et/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66818.txt` | CulturaX | 5,160 |  |
| `culturax_mC4_66819.txt` | CulturaX | 5,560 |  |
| `culturax_mC4_66820.txt` | CulturaX | 2,050 |  |

#### `utf-16le-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_104835.txt` | CulturaX | 1,982 |  |
| `culturax_mC4_104836.txt` | CulturaX | 5,052 |  |
| `culturax_mC4_104837.txt` | CulturaX | 3,914 |  |

#### `utf-16le-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80361.txt` | CulturaX | 1,732 |  |
| `culturax_mC4_80362.txt` | CulturaX | 6,000 |  |
| `culturax_mC4_80363.txt` | CulturaX | 5,608 |  |

#### `utf-16le-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 5,742 |  |
| `culturax_OSCAR-2301_88370.txt` | CulturaX | 1,466 |  |
| `culturax_mC4_88369.txt` | CulturaX | 5,720 |  |

#### `utf-16le-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63468.txt` | CulturaX | 5,844 |  |
| `culturax_mC4_63469.txt` | CulturaX | 5,794 |  |
| `culturax_mC4_63470.txt` | CulturaX | 2,444 |  |

#### `utf-16le-gd/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 2,016 |  |
| `culturax_00001.txt` | CulturaX | 5,778 |  |
| `culturax_00002.txt` | CulturaX | 13,100 |  |

#### `utf-16le-he/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_58265.txt` | CulturaX | 5,974 |  |
| `culturax_OSCAR-2301_58266.txt` | CulturaX | 5,958 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 5,974 |  |

#### `utf-16le-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 208 |  |
| `culturax_00001.txt` | CulturaX | 282 |  |
| `culturax_00002.txt` | CulturaX | 1,152 |  |

#### `utf-16le-hu/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 3,144 |  |
| `culturax_OSCAR-2301_82420.txt` | CulturaX | 3,164 |  |
| `culturax_mC4_82418.txt` | CulturaX | 1,254 |  |

#### `utf-16le-id/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 5,144 |  |
| `culturax_mC4_114890.txt` | CulturaX | 5,374 |  |
| `culturax_mC4_114892.txt` | CulturaX | 3,060 |  |

#### `utf-16le-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 5,762 |  |
| `culturax_mC4_77488.txt` | CulturaX | 3,012 |  |
| `culturax_mC4_77489.txt` | CulturaX | 5,616 |  |

#### `utf-16le-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 4,756 |  |
| `culturax_mC4_92390.txt` | CulturaX | 2,556 |  |
| `culturax_mC4_92391.txt` | CulturaX | 2,890 |  |

#### `utf-16le-ja/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_6.txt` | CulturaX | 1,624 |  |
| `culturax_mC4_4.txt` | CulturaX | 6,000 |  |
| `culturax_mC4_5.txt` | CulturaX | 5,860 |  |

#### `utf-16le-kk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 3,908 |  |
| `culturax_mC4_73161.txt` | CulturaX | 1,308 |  |
| `culturax_mC4_73162.txt` | CulturaX | 5,842 |  |

#### `utf-16le-ko/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_0.txt` | CulturaX | 3,566 |  |
| `culturax_mC4_1.txt` | CulturaX | 5,792 |  |
| `culturax_mC4_2.txt` | CulturaX | 1,182 |  |

#### `utf-16le-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 5,364 |  |
| `culturax_mC4_73446.txt` | CulturaX | 5,366 |  |
| `culturax_mC4_73447.txt` | CulturaX | 6,000 |  |

#### `utf-16le-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 5,936 |  |
| `culturax_mC4_71629.txt` | CulturaX | 2,608 |  |
| `culturax_mC4_71630.txt` | CulturaX | 1,532 |  |

#### `utf-16le-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 4,726 |  |
| `culturax_mC4_102726.txt` | CulturaX | 2,410 |  |
| `culturax_mC4_102727.txt` | CulturaX | 6,000 |  |

#### `utf-16le-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 454 |  |
| `culturax_00001.txt` | CulturaX | 1,080 |  |
| `culturax_00002.txt` | CulturaX | 8,132 |  |

#### `utf-16le-mt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_51488.txt` | CulturaX | 3,574 |  |
| `culturax_mC4_51489.txt` | CulturaX | 6,000 |  |
| `culturax_mC4_51490.txt` | CulturaX | 2,194 |  |

#### `utf-16le-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_107677.txt` | CulturaX | 2,724 |  |
| `culturax_mC4_107675.txt` | CulturaX | 4,910 |  |
| `culturax_mC4_107676.txt` | CulturaX | 2,086 |  |

#### `utf-16le-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 5,004 |  |
| `culturax_mC4_66763.txt` | CulturaX | 6,000 |  |
| `culturax_mC4_66764.txt` | CulturaX | 6,000 |  |

#### `utf-16le-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 3,630 |  |
| `culturax_mC4_97060.txt` | CulturaX | 2,600 |  |
| `culturax_mC4_97061.txt` | CulturaX | 4,766 |  |

#### `utf-16le-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 1,904 |  |
| `culturax_mC4_101817.txt` | CulturaX | 5,750 |  |
| `culturax_mC4_101818.txt` | CulturaX | 5,674 |  |

#### `utf-16le-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_78977.txt` | CulturaX | 1,542 |  |
| `culturax_mC4_78976.txt` | CulturaX | 5,266 |  |
| `culturax_mC4_78978.txt` | CulturaX | 5,454 |  |

#### `utf-16le-ru/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 6,000 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 5,662 |  |
| `culturax_mC4_85056.txt` | CulturaX | 2,652 |  |

#### `utf-16le-sk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95224.txt` | CulturaX | 2,890 |  |
| `culturax_mC4_95226.txt` | CulturaX | 4,008 |  |
| `culturax_mC4_95227.txt` | CulturaX | 5,736 |  |

#### `utf-16le-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 5,784 |  |
| `culturax_mC4_66689.txt` | CulturaX | 5,310 |  |
| `culturax_mC4_66690.txt` | CulturaX | 2,376 |  |

#### `utf-16le-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66918.txt` | CulturaX | 2,340 |  |
| `culturax_mC4_66920.txt` | CulturaX | 2,246 |  |
| `culturax_mC4_66921.txt` | CulturaX | 5,260 |  |

#### `utf-16le-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 5,466 |  |
| `culturax_mC4_96486.txt` | CulturaX | 5,066 |  |
| `culturax_mC4_96487.txt` | CulturaX | 4,222 |  |

#### `utf-16le-tg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_74865.txt` | CulturaX | 3,488 |  |
| `culturax_mC4_74866.txt` | CulturaX | 5,638 |  |
| `culturax_mC4_74867.txt` | CulturaX | 5,706 |  |

#### `utf-16le-th/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_109134.txt` | CulturaX | 5,496 |  |
| `culturax_mC4_109133.txt` | CulturaX | 5,978 |  |
| `culturax_mC4_109136.txt` | CulturaX | 2,658 |  |

#### `utf-16le-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 2,322 |  |
| `culturax_mC4_107849.txt` | CulturaX | 1,458 |  |
| `culturax_mC4_107850.txt` | CulturaX | 2,678 |  |

#### `utf-16le-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95019.txt` | CulturaX | 2,160 |  |
| `culturax_mC4_95020.txt` | CulturaX | 2,742 |  |
| `culturax_mC4_95021.txt` | CulturaX | 5,642 |  |

#### `utf-16le-ur/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_82296.txt` | CulturaX | 5,778 |  |
| `culturax_mC4_82297.txt` | CulturaX | 5,884 |  |
| `culturax_mC4_82298.txt` | CulturaX | 2,450 |  |

#### `utf-16le-vi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_85695.txt` | CulturaX | 1,340 |  |
| `culturax_mC4_85693.txt` | CulturaX | 5,336 |  |
| `culturax_mC4_85694.txt` | CulturaX | 5,736 |  |

#### `utf-16le-zh/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_3.txt` | CulturaX | 4,190 |  |
| `culturax_mC4_5.txt` | CulturaX | 1,288 |  |
| `culturax_mC4_7.txt` | CulturaX | 2,146 |  |
| `sample_chinese_no_bom.txt` | charset-normalizer | 110 | BOM-less Chinese text (mixed simplified and traditional) |

#### `utf-32-ar/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 9,692 |  |
| `culturax_mC4_98635.txt` | CulturaX | 4,504 |  |
| `culturax_mC4_98638.txt` | CulturaX | 11,888 |  |

#### `utf-32-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77015.txt` | CulturaX | 11,736 |  |
| `culturax_mC4_77016.txt` | CulturaX | 4,756 |  |
| `culturax_mC4_77017.txt` | CulturaX | 9,168 |  |

#### `utf-32-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 5,240 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 5,028 |  |
| `culturax_mC4_84187.txt` | CulturaX | 9,484 |  |

#### `utf-32-br/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_43762.txt` | CulturaX | 2,184 |  |
| `culturax_OSCAR-2019_43764.txt` | CulturaX | 2,516 |  |

#### `utf-32-cs/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 5,452 |  |
| `culturax_mC4_98820.txt` | CulturaX | 5,700 |  |
| `culturax_mC4_98822.txt` | CulturaX | 11,744 |  |

#### `utf-32-cy/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78726.txt` | CulturaX | 10,856 |  |
| `culturax_mC4_78727.txt` | CulturaX | 11,540 |  |
| `culturax_mC4_78728.txt` | CulturaX | 8,268 |  |

#### `utf-32-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 7,676 |  |
| `culturax_mC4_83467.txt` | CulturaX | 11,712 |  |
| `culturax_mC4_83468.txt` | CulturaX | 8,080 |  |

#### `utf-32-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 10,308 |  |
| `culturax_mC4_83755.txt` | CulturaX | 8,632 |  |
| `culturax_mC4_83756.txt` | CulturaX | 9,040 |  |

#### `utf-32-el/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 10,984 |  |
| `culturax_mC4_103810.txt` | CulturaX | 8,528 |  |
| `culturax_mC4_103811.txt` | CulturaX | 4,440 |  |

#### `utf-32-en/` — 7 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `bom-utf-32-be.srt` | unknown | 3,428 | BOM detection test subtitle |
| `bom-utf-32-le.srt` | unknown | 3,428 | BOM detection test subtitle |
| `culturax_mC4_84511.txt` | CulturaX | 6,564 |  |
| `culturax_mC4_84512.txt` | CulturaX | 3,400 |  |
| `culturax_mC4_84513.txt` | CulturaX | 10,136 |  |
| `iris-utf-32.csv` | unknown | 20,452 | Iris dataset, originally from Capital One DataProfiler |
| `iris-utf-32.json` | unknown | 76,592 | Iris dataset, originally from Capital One DataProfiler |

#### `utf-32-eo/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_40441.txt` | CulturaX | 11,712 |  |
| `culturax_mC4_40442.txt` | CulturaX | 5,280 |  |
| `culturax_mC4_40443.txt` | CulturaX | 10,532 |  |

#### `utf-32-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 5,856 |  |
| `culturax_mC4_87070.txt` | CulturaX | 11,884 |  |
| `culturax_mC4_87071.txt` | CulturaX | 11,116 |  |

#### `utf-32-et/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66818.txt` | CulturaX | 10,324 |  |
| `culturax_mC4_66819.txt` | CulturaX | 11,124 |  |
| `culturax_mC4_66820.txt` | CulturaX | 4,104 |  |

#### `utf-32-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_104835.txt` | CulturaX | 3,968 |  |
| `culturax_mC4_104836.txt` | CulturaX | 10,108 |  |
| `culturax_mC4_104837.txt` | CulturaX | 7,832 |  |

#### `utf-32-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80361.txt` | CulturaX | 3,468 |  |
| `culturax_mC4_80362.txt` | CulturaX | 12,004 |  |
| `culturax_mC4_80363.txt` | CulturaX | 11,220 |  |

#### `utf-32-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 11,488 |  |
| `culturax_OSCAR-2301_88370.txt` | CulturaX | 2,936 |  |
| `culturax_mC4_88369.txt` | CulturaX | 11,440 |  |

#### `utf-32-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63468.txt` | CulturaX | 11,692 |  |
| `culturax_mC4_63469.txt` | CulturaX | 11,592 |  |
| `culturax_mC4_63470.txt` | CulturaX | 4,892 |  |

#### `utf-32-gd/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 4,036 |  |
| `culturax_00001.txt` | CulturaX | 11,560 |  |
| `culturax_00002.txt` | CulturaX | 26,204 |  |

#### `utf-32-he/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_58265.txt` | CulturaX | 11,952 |  |
| `culturax_OSCAR-2301_58266.txt` | CulturaX | 11,920 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 11,952 |  |

#### `utf-32-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 420 |  |
| `culturax_00001.txt` | CulturaX | 568 |  |
| `culturax_00002.txt` | CulturaX | 2,308 |  |

#### `utf-32-hu/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 6,292 |  |
| `culturax_OSCAR-2301_82420.txt` | CulturaX | 6,332 |  |
| `culturax_mC4_82418.txt` | CulturaX | 2,512 |  |

#### `utf-32-id/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 10,292 |  |
| `culturax_mC4_114890.txt` | CulturaX | 10,752 |  |
| `culturax_mC4_114892.txt` | CulturaX | 6,124 |  |

#### `utf-32-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 11,528 |  |
| `culturax_mC4_77488.txt` | CulturaX | 6,028 |  |
| `culturax_mC4_77489.txt` | CulturaX | 11,236 |  |

#### `utf-32-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 9,516 |  |
| `culturax_mC4_92390.txt` | CulturaX | 5,116 |  |
| `culturax_mC4_92391.txt` | CulturaX | 5,784 |  |

#### `utf-32-ja/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_6.txt` | CulturaX | 3,252 |  |
| `culturax_mC4_4.txt` | CulturaX | 12,004 |  |
| `culturax_mC4_5.txt` | CulturaX | 11,724 |  |

#### `utf-32-kk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 7,820 |  |
| `culturax_mC4_73161.txt` | CulturaX | 2,620 |  |
| `culturax_mC4_73162.txt` | CulturaX | 11,688 |  |

#### `utf-32-ko/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_0.txt` | CulturaX | 7,136 |  |
| `culturax_mC4_1.txt` | CulturaX | 11,588 |  |
| `culturax_mC4_2.txt` | CulturaX | 2,368 |  |

#### `utf-32-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 10,732 |  |
| `culturax_mC4_73446.txt` | CulturaX | 10,736 |  |
| `culturax_mC4_73447.txt` | CulturaX | 12,004 |  |

#### `utf-32-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 11,876 |  |
| `culturax_mC4_71629.txt` | CulturaX | 5,220 |  |
| `culturax_mC4_71630.txt` | CulturaX | 3,068 |  |

#### `utf-32-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 9,456 |  |
| `culturax_mC4_102726.txt` | CulturaX | 4,824 |  |
| `culturax_mC4_102727.txt` | CulturaX | 12,004 |  |

#### `utf-32-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 912 |  |
| `culturax_00001.txt` | CulturaX | 2,164 |  |
| `culturax_00002.txt` | CulturaX | 16,268 |  |

#### `utf-32-mt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_51488.txt` | CulturaX | 7,152 |  |
| `culturax_mC4_51489.txt` | CulturaX | 12,004 |  |
| `culturax_mC4_51490.txt` | CulturaX | 4,392 |  |

#### `utf-32-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_107677.txt` | CulturaX | 5,452 |  |
| `culturax_mC4_107675.txt` | CulturaX | 9,824 |  |
| `culturax_mC4_107676.txt` | CulturaX | 4,176 |  |

#### `utf-32-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 10,012 |  |
| `culturax_mC4_66763.txt` | CulturaX | 12,004 |  |
| `culturax_mC4_66764.txt` | CulturaX | 12,004 |  |

#### `utf-32-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 7,264 |  |
| `culturax_mC4_97060.txt` | CulturaX | 5,204 |  |
| `culturax_mC4_97061.txt` | CulturaX | 9,536 |  |

#### `utf-32-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 3,812 |  |
| `culturax_mC4_101817.txt` | CulturaX | 11,504 |  |
| `culturax_mC4_101818.txt` | CulturaX | 11,352 |  |

#### `utf-32-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_78977.txt` | CulturaX | 3,088 |  |
| `culturax_mC4_78976.txt` | CulturaX | 10,536 |  |
| `culturax_mC4_78978.txt` | CulturaX | 10,912 |  |

#### `utf-32-ru/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 12,004 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 11,328 |  |
| `culturax_mC4_85056.txt` | CulturaX | 5,308 |  |

#### `utf-32-sk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95224.txt` | CulturaX | 5,784 |  |
| `culturax_mC4_95226.txt` | CulturaX | 8,020 |  |
| `culturax_mC4_95227.txt` | CulturaX | 11,476 |  |

#### `utf-32-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 11,572 |  |
| `culturax_mC4_66689.txt` | CulturaX | 10,624 |  |
| `culturax_mC4_66690.txt` | CulturaX | 4,756 |  |

#### `utf-32-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66918.txt` | CulturaX | 4,684 |  |
| `culturax_mC4_66920.txt` | CulturaX | 4,496 |  |
| `culturax_mC4_66921.txt` | CulturaX | 10,524 |  |

#### `utf-32-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 10,936 |  |
| `culturax_mC4_96486.txt` | CulturaX | 10,136 |  |
| `culturax_mC4_96487.txt` | CulturaX | 8,448 |  |

#### `utf-32-tg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_74865.txt` | CulturaX | 6,980 |  |
| `culturax_mC4_74866.txt` | CulturaX | 11,280 |  |
| `culturax_mC4_74867.txt` | CulturaX | 11,416 |  |

#### `utf-32-th/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_109134.txt` | CulturaX | 10,996 |  |
| `culturax_mC4_109133.txt` | CulturaX | 11,912 |  |
| `culturax_mC4_109136.txt` | CulturaX | 5,320 |  |

#### `utf-32-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 4,648 |  |
| `culturax_mC4_107849.txt` | CulturaX | 2,920 |  |
| `culturax_mC4_107850.txt` | CulturaX | 5,360 |  |

#### `utf-32-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95019.txt` | CulturaX | 4,324 |  |
| `culturax_mC4_95020.txt` | CulturaX | 5,488 |  |
| `culturax_mC4_95021.txt` | CulturaX | 11,288 |  |

#### `utf-32-ur/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_82296.txt` | CulturaX | 11,560 |  |
| `culturax_mC4_82297.txt` | CulturaX | 11,772 |  |
| `culturax_mC4_82298.txt` | CulturaX | 4,904 |  |

#### `utf-32-vi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_85695.txt` | CulturaX | 2,684 |  |
| `culturax_mC4_85693.txt` | CulturaX | 10,676 |  |
| `culturax_mC4_85694.txt` | CulturaX | 11,476 |  |

#### `utf-32-zh/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_3.txt` | CulturaX | 8,384 |  |
| `culturax_mC4_5.txt` | CulturaX | 2,580 |  |
| `culturax_mC4_7.txt` | CulturaX | 4,296 |  |

#### `utf-32be-ar/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 9,688 |  |
| `culturax_mC4_98635.txt` | CulturaX | 4,500 |  |
| `culturax_mC4_98638.txt` | CulturaX | 11,884 |  |

#### `utf-32be-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77015.txt` | CulturaX | 11,732 |  |
| `culturax_mC4_77016.txt` | CulturaX | 4,752 |  |
| `culturax_mC4_77017.txt` | CulturaX | 9,164 |  |

#### `utf-32be-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 5,236 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 5,024 |  |
| `culturax_mC4_84187.txt` | CulturaX | 9,480 |  |

#### `utf-32be-br/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_43762.txt` | CulturaX | 2,180 |  |
| `culturax_OSCAR-2019_43764.txt` | CulturaX | 2,512 |  |

#### `utf-32be-cs/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 5,448 |  |
| `culturax_mC4_98820.txt` | CulturaX | 5,696 |  |
| `culturax_mC4_98822.txt` | CulturaX | 11,740 |  |

#### `utf-32be-cy/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78726.txt` | CulturaX | 10,852 |  |
| `culturax_mC4_78727.txt` | CulturaX | 11,536 |  |
| `culturax_mC4_78728.txt` | CulturaX | 8,264 |  |

#### `utf-32be-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 7,672 |  |
| `culturax_mC4_83467.txt` | CulturaX | 11,708 |  |
| `culturax_mC4_83468.txt` | CulturaX | 8,076 |  |

#### `utf-32be-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 10,304 |  |
| `culturax_mC4_83755.txt` | CulturaX | 8,628 |  |
| `culturax_mC4_83756.txt` | CulturaX | 9,036 |  |

#### `utf-32be-el/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 10,980 |  |
| `culturax_mC4_103810.txt` | CulturaX | 8,524 |  |
| `culturax_mC4_103811.txt` | CulturaX | 4,436 |  |

#### `utf-32be-en/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_84511.txt` | CulturaX | 6,560 |  |
| `culturax_mC4_84512.txt` | CulturaX | 3,396 |  |
| `culturax_mC4_84513.txt` | CulturaX | 10,132 |  |
| `nobom-utf32be.txt` | unknown | 3,176 | No-BOM encoding test |
| `plane1-utf-32be.html` | Contributed | 24,500 | Unicode Plane 1 (supplementary) test |

#### `utf-32be-eo/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_40441.txt` | CulturaX | 11,708 |  |
| `culturax_mC4_40442.txt` | CulturaX | 5,276 |  |
| `culturax_mC4_40443.txt` | CulturaX | 10,528 |  |

#### `utf-32be-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 5,852 |  |
| `culturax_mC4_87070.txt` | CulturaX | 11,880 |  |
| `culturax_mC4_87071.txt` | CulturaX | 11,112 |  |

#### `utf-32be-et/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66818.txt` | CulturaX | 10,320 |  |
| `culturax_mC4_66819.txt` | CulturaX | 11,120 |  |
| `culturax_mC4_66820.txt` | CulturaX | 4,100 |  |

#### `utf-32be-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_104835.txt` | CulturaX | 3,964 |  |
| `culturax_mC4_104836.txt` | CulturaX | 10,104 |  |
| `culturax_mC4_104837.txt` | CulturaX | 7,828 |  |

#### `utf-32be-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80361.txt` | CulturaX | 3,464 |  |
| `culturax_mC4_80362.txt` | CulturaX | 12,000 |  |
| `culturax_mC4_80363.txt` | CulturaX | 11,216 |  |

#### `utf-32be-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 11,484 |  |
| `culturax_OSCAR-2301_88370.txt` | CulturaX | 2,932 |  |
| `culturax_mC4_88369.txt` | CulturaX | 11,436 |  |

#### `utf-32be-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63468.txt` | CulturaX | 11,688 |  |
| `culturax_mC4_63469.txt` | CulturaX | 11,588 |  |
| `culturax_mC4_63470.txt` | CulturaX | 4,888 |  |

#### `utf-32be-gd/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 4,032 |  |
| `culturax_00001.txt` | CulturaX | 11,556 |  |
| `culturax_00002.txt` | CulturaX | 26,200 |  |

#### `utf-32be-he/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_58265.txt` | CulturaX | 11,948 |  |
| `culturax_OSCAR-2301_58266.txt` | CulturaX | 11,916 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 11,948 |  |

#### `utf-32be-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 416 |  |
| `culturax_00001.txt` | CulturaX | 564 |  |
| `culturax_00002.txt` | CulturaX | 2,304 |  |

#### `utf-32be-hu/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 6,288 |  |
| `culturax_OSCAR-2301_82420.txt` | CulturaX | 6,328 |  |
| `culturax_mC4_82418.txt` | CulturaX | 2,508 |  |

#### `utf-32be-id/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 10,288 |  |
| `culturax_mC4_114890.txt` | CulturaX | 10,748 |  |
| `culturax_mC4_114892.txt` | CulturaX | 6,120 |  |

#### `utf-32be-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 11,524 |  |
| `culturax_mC4_77488.txt` | CulturaX | 6,024 |  |
| `culturax_mC4_77489.txt` | CulturaX | 11,232 |  |

#### `utf-32be-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 9,512 |  |
| `culturax_mC4_92390.txt` | CulturaX | 5,112 |  |
| `culturax_mC4_92391.txt` | CulturaX | 5,780 |  |

#### `utf-32be-ja/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_6.txt` | CulturaX | 3,248 |  |
| `culturax_mC4_4.txt` | CulturaX | 12,000 |  |
| `culturax_mC4_5.txt` | CulturaX | 11,720 |  |

#### `utf-32be-kk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 7,816 |  |
| `culturax_mC4_73161.txt` | CulturaX | 2,616 |  |
| `culturax_mC4_73162.txt` | CulturaX | 11,684 |  |

#### `utf-32be-ko/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_0.txt` | CulturaX | 7,132 |  |
| `culturax_mC4_1.txt` | CulturaX | 11,584 |  |
| `culturax_mC4_2.txt` | CulturaX | 2,364 |  |

#### `utf-32be-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 10,728 |  |
| `culturax_mC4_73446.txt` | CulturaX | 10,732 |  |
| `culturax_mC4_73447.txt` | CulturaX | 12,000 |  |

#### `utf-32be-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 11,872 |  |
| `culturax_mC4_71629.txt` | CulturaX | 5,216 |  |
| `culturax_mC4_71630.txt` | CulturaX | 3,064 |  |

#### `utf-32be-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 9,452 |  |
| `culturax_mC4_102726.txt` | CulturaX | 4,820 |  |
| `culturax_mC4_102727.txt` | CulturaX | 12,000 |  |

#### `utf-32be-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 908 |  |
| `culturax_00001.txt` | CulturaX | 2,160 |  |
| `culturax_00002.txt` | CulturaX | 16,264 |  |

#### `utf-32be-mt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_51488.txt` | CulturaX | 7,148 |  |
| `culturax_mC4_51489.txt` | CulturaX | 12,000 |  |
| `culturax_mC4_51490.txt` | CulturaX | 4,388 |  |

#### `utf-32be-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_107677.txt` | CulturaX | 5,448 |  |
| `culturax_mC4_107675.txt` | CulturaX | 9,820 |  |
| `culturax_mC4_107676.txt` | CulturaX | 4,172 |  |

#### `utf-32be-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 10,008 |  |
| `culturax_mC4_66763.txt` | CulturaX | 12,000 |  |
| `culturax_mC4_66764.txt` | CulturaX | 12,000 |  |

#### `utf-32be-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 7,260 |  |
| `culturax_mC4_97060.txt` | CulturaX | 5,200 |  |
| `culturax_mC4_97061.txt` | CulturaX | 9,532 |  |

#### `utf-32be-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 3,808 |  |
| `culturax_mC4_101817.txt` | CulturaX | 11,500 |  |
| `culturax_mC4_101818.txt` | CulturaX | 11,348 |  |

#### `utf-32be-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_78977.txt` | CulturaX | 3,084 |  |
| `culturax_mC4_78976.txt` | CulturaX | 10,532 |  |
| `culturax_mC4_78978.txt` | CulturaX | 10,908 |  |

#### `utf-32be-ru/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 12,000 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 11,324 |  |
| `culturax_mC4_85056.txt` | CulturaX | 5,304 |  |

#### `utf-32be-sk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95224.txt` | CulturaX | 5,780 |  |
| `culturax_mC4_95226.txt` | CulturaX | 8,016 |  |
| `culturax_mC4_95227.txt` | CulturaX | 11,472 |  |

#### `utf-32be-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 11,568 |  |
| `culturax_mC4_66689.txt` | CulturaX | 10,620 |  |
| `culturax_mC4_66690.txt` | CulturaX | 4,752 |  |

#### `utf-32be-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66918.txt` | CulturaX | 4,680 |  |
| `culturax_mC4_66920.txt` | CulturaX | 4,492 |  |
| `culturax_mC4_66921.txt` | CulturaX | 10,520 |  |

#### `utf-32be-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 10,932 |  |
| `culturax_mC4_96486.txt` | CulturaX | 10,132 |  |
| `culturax_mC4_96487.txt` | CulturaX | 8,444 |  |

#### `utf-32be-tg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_74865.txt` | CulturaX | 6,976 |  |
| `culturax_mC4_74866.txt` | CulturaX | 11,276 |  |
| `culturax_mC4_74867.txt` | CulturaX | 11,412 |  |

#### `utf-32be-th/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_109134.txt` | CulturaX | 10,992 |  |
| `culturax_mC4_109133.txt` | CulturaX | 11,908 |  |
| `culturax_mC4_109136.txt` | CulturaX | 5,316 |  |

#### `utf-32be-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 4,644 |  |
| `culturax_mC4_107849.txt` | CulturaX | 2,916 |  |
| `culturax_mC4_107850.txt` | CulturaX | 5,356 |  |

#### `utf-32be-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95019.txt` | CulturaX | 4,320 |  |
| `culturax_mC4_95020.txt` | CulturaX | 5,484 |  |
| `culturax_mC4_95021.txt` | CulturaX | 11,284 |  |

#### `utf-32be-ur/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_82296.txt` | CulturaX | 11,556 |  |
| `culturax_mC4_82297.txt` | CulturaX | 11,768 |  |
| `culturax_mC4_82298.txt` | CulturaX | 4,900 |  |

#### `utf-32be-vi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_85695.txt` | CulturaX | 2,680 |  |
| `culturax_mC4_85693.txt` | CulturaX | 10,672 |  |
| `culturax_mC4_85694.txt` | CulturaX | 11,472 |  |

#### `utf-32be-zh/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_3.txt` | CulturaX | 8,380 |  |
| `culturax_mC4_5.txt` | CulturaX | 2,576 |  |
| `culturax_mC4_7.txt` | CulturaX | 4,292 |  |
| `sample_chinese_no_bom.txt` | charset-normalizer | 220 | BOM-less Chinese text (mixed simplified and traditional) |

#### `utf-32le-ar/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 9,688 |  |
| `culturax_mC4_98635.txt` | CulturaX | 4,500 |  |
| `culturax_mC4_98638.txt` | CulturaX | 11,884 |  |

#### `utf-32le-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77015.txt` | CulturaX | 11,732 |  |
| `culturax_mC4_77016.txt` | CulturaX | 4,752 |  |
| `culturax_mC4_77017.txt` | CulturaX | 9,164 |  |

#### `utf-32le-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 5,236 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 5,024 |  |
| `culturax_mC4_84187.txt` | CulturaX | 9,480 |  |

#### `utf-32le-br/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_43762.txt` | CulturaX | 2,180 |  |
| `culturax_OSCAR-2019_43764.txt` | CulturaX | 2,512 |  |

#### `utf-32le-cs/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 5,448 |  |
| `culturax_mC4_98820.txt` | CulturaX | 5,696 |  |
| `culturax_mC4_98822.txt` | CulturaX | 11,740 |  |

#### `utf-32le-cy/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78726.txt` | CulturaX | 10,852 |  |
| `culturax_mC4_78727.txt` | CulturaX | 11,536 |  |
| `culturax_mC4_78728.txt` | CulturaX | 8,264 |  |

#### `utf-32le-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 7,672 |  |
| `culturax_mC4_83467.txt` | CulturaX | 11,708 |  |
| `culturax_mC4_83468.txt` | CulturaX | 8,076 |  |

#### `utf-32le-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 10,304 |  |
| `culturax_mC4_83755.txt` | CulturaX | 8,628 |  |
| `culturax_mC4_83756.txt` | CulturaX | 9,036 |  |

#### `utf-32le-el/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 10,980 |  |
| `culturax_mC4_103810.txt` | CulturaX | 8,524 |  |
| `culturax_mC4_103811.txt` | CulturaX | 4,436 |  |

#### `utf-32le-en/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_84511.txt` | CulturaX | 6,560 |  |
| `culturax_mC4_84512.txt` | CulturaX | 3,396 |  |
| `culturax_mC4_84513.txt` | CulturaX | 10,132 |  |
| `nobom-utf32le.txt` | unknown | 3,176 | No-BOM encoding test |
| `plane1-utf-32le.html` | Contributed | 24,500 | Unicode Plane 1 (supplementary) test |

#### `utf-32le-eo/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_40441.txt` | CulturaX | 11,708 |  |
| `culturax_mC4_40442.txt` | CulturaX | 5,276 |  |
| `culturax_mC4_40443.txt` | CulturaX | 10,528 |  |

#### `utf-32le-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 5,852 |  |
| `culturax_mC4_87070.txt` | CulturaX | 11,880 |  |
| `culturax_mC4_87071.txt` | CulturaX | 11,112 |  |

#### `utf-32le-et/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66818.txt` | CulturaX | 10,320 |  |
| `culturax_mC4_66819.txt` | CulturaX | 11,120 |  |
| `culturax_mC4_66820.txt` | CulturaX | 4,100 |  |

#### `utf-32le-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_104835.txt` | CulturaX | 3,964 |  |
| `culturax_mC4_104836.txt` | CulturaX | 10,104 |  |
| `culturax_mC4_104837.txt` | CulturaX | 7,828 |  |

#### `utf-32le-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80361.txt` | CulturaX | 3,464 |  |
| `culturax_mC4_80362.txt` | CulturaX | 12,000 |  |
| `culturax_mC4_80363.txt` | CulturaX | 11,216 |  |

#### `utf-32le-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 11,484 |  |
| `culturax_OSCAR-2301_88370.txt` | CulturaX | 2,932 |  |
| `culturax_mC4_88369.txt` | CulturaX | 11,436 |  |

#### `utf-32le-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63468.txt` | CulturaX | 11,688 |  |
| `culturax_mC4_63469.txt` | CulturaX | 11,588 |  |
| `culturax_mC4_63470.txt` | CulturaX | 4,888 |  |

#### `utf-32le-gd/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 4,032 |  |
| `culturax_00001.txt` | CulturaX | 11,556 |  |
| `culturax_00002.txt` | CulturaX | 26,200 |  |

#### `utf-32le-he/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_58265.txt` | CulturaX | 11,948 |  |
| `culturax_OSCAR-2301_58266.txt` | CulturaX | 11,916 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 11,948 |  |

#### `utf-32le-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 416 |  |
| `culturax_00001.txt` | CulturaX | 564 |  |
| `culturax_00002.txt` | CulturaX | 2,304 |  |

#### `utf-32le-hu/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 6,288 |  |
| `culturax_OSCAR-2301_82420.txt` | CulturaX | 6,328 |  |
| `culturax_mC4_82418.txt` | CulturaX | 2,508 |  |

#### `utf-32le-id/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 10,288 |  |
| `culturax_mC4_114890.txt` | CulturaX | 10,748 |  |
| `culturax_mC4_114892.txt` | CulturaX | 6,120 |  |

#### `utf-32le-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 11,524 |  |
| `culturax_mC4_77488.txt` | CulturaX | 6,024 |  |
| `culturax_mC4_77489.txt` | CulturaX | 11,232 |  |

#### `utf-32le-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 9,512 |  |
| `culturax_mC4_92390.txt` | CulturaX | 5,112 |  |
| `culturax_mC4_92391.txt` | CulturaX | 5,780 |  |

#### `utf-32le-ja/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_6.txt` | CulturaX | 3,248 |  |
| `culturax_mC4_4.txt` | CulturaX | 12,000 |  |
| `culturax_mC4_5.txt` | CulturaX | 11,720 |  |

#### `utf-32le-kk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 7,816 |  |
| `culturax_mC4_73161.txt` | CulturaX | 2,616 |  |
| `culturax_mC4_73162.txt` | CulturaX | 11,684 |  |

#### `utf-32le-ko/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_0.txt` | CulturaX | 7,132 |  |
| `culturax_mC4_1.txt` | CulturaX | 11,584 |  |
| `culturax_mC4_2.txt` | CulturaX | 2,364 |  |

#### `utf-32le-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 10,728 |  |
| `culturax_mC4_73446.txt` | CulturaX | 10,732 |  |
| `culturax_mC4_73447.txt` | CulturaX | 12,000 |  |

#### `utf-32le-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 11,872 |  |
| `culturax_mC4_71629.txt` | CulturaX | 5,216 |  |
| `culturax_mC4_71630.txt` | CulturaX | 3,064 |  |

#### `utf-32le-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 9,452 |  |
| `culturax_mC4_102726.txt` | CulturaX | 4,820 |  |
| `culturax_mC4_102727.txt` | CulturaX | 12,000 |  |

#### `utf-32le-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 908 |  |
| `culturax_00001.txt` | CulturaX | 2,160 |  |
| `culturax_00002.txt` | CulturaX | 16,264 |  |

#### `utf-32le-mt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_51488.txt` | CulturaX | 7,148 |  |
| `culturax_mC4_51489.txt` | CulturaX | 12,000 |  |
| `culturax_mC4_51490.txt` | CulturaX | 4,388 |  |

#### `utf-32le-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_107677.txt` | CulturaX | 5,448 |  |
| `culturax_mC4_107675.txt` | CulturaX | 9,820 |  |
| `culturax_mC4_107676.txt` | CulturaX | 4,172 |  |

#### `utf-32le-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 10,008 |  |
| `culturax_mC4_66763.txt` | CulturaX | 12,000 |  |
| `culturax_mC4_66764.txt` | CulturaX | 12,000 |  |

#### `utf-32le-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 7,260 |  |
| `culturax_mC4_97060.txt` | CulturaX | 5,200 |  |
| `culturax_mC4_97061.txt` | CulturaX | 9,532 |  |

#### `utf-32le-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 3,808 |  |
| `culturax_mC4_101817.txt` | CulturaX | 11,500 |  |
| `culturax_mC4_101818.txt` | CulturaX | 11,348 |  |

#### `utf-32le-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_78977.txt` | CulturaX | 3,084 |  |
| `culturax_mC4_78976.txt` | CulturaX | 10,532 |  |
| `culturax_mC4_78978.txt` | CulturaX | 10,908 |  |

#### `utf-32le-ru/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 12,000 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 11,324 |  |
| `culturax_mC4_85056.txt` | CulturaX | 5,304 |  |

#### `utf-32le-sk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95224.txt` | CulturaX | 5,780 |  |
| `culturax_mC4_95226.txt` | CulturaX | 8,016 |  |
| `culturax_mC4_95227.txt` | CulturaX | 11,472 |  |

#### `utf-32le-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 11,568 |  |
| `culturax_mC4_66689.txt` | CulturaX | 10,620 |  |
| `culturax_mC4_66690.txt` | CulturaX | 4,752 |  |

#### `utf-32le-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66918.txt` | CulturaX | 4,680 |  |
| `culturax_mC4_66920.txt` | CulturaX | 4,492 |  |
| `culturax_mC4_66921.txt` | CulturaX | 10,520 |  |

#### `utf-32le-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 10,932 |  |
| `culturax_mC4_96486.txt` | CulturaX | 10,132 |  |
| `culturax_mC4_96487.txt` | CulturaX | 8,444 |  |

#### `utf-32le-tg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_74865.txt` | CulturaX | 6,976 |  |
| `culturax_mC4_74866.txt` | CulturaX | 11,276 |  |
| `culturax_mC4_74867.txt` | CulturaX | 11,412 |  |

#### `utf-32le-th/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_109134.txt` | CulturaX | 10,992 |  |
| `culturax_mC4_109133.txt` | CulturaX | 11,908 |  |
| `culturax_mC4_109136.txt` | CulturaX | 5,316 |  |

#### `utf-32le-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 4,644 |  |
| `culturax_mC4_107849.txt` | CulturaX | 2,916 |  |
| `culturax_mC4_107850.txt` | CulturaX | 5,356 |  |

#### `utf-32le-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95019.txt` | CulturaX | 4,320 |  |
| `culturax_mC4_95020.txt` | CulturaX | 5,484 |  |
| `culturax_mC4_95021.txt` | CulturaX | 11,284 |  |

#### `utf-32le-ur/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_82296.txt` | CulturaX | 11,556 |  |
| `culturax_mC4_82297.txt` | CulturaX | 11,768 |  |
| `culturax_mC4_82298.txt` | CulturaX | 4,900 |  |

#### `utf-32le-vi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_85695.txt` | CulturaX | 2,680 |  |
| `culturax_mC4_85693.txt` | CulturaX | 10,672 |  |
| `culturax_mC4_85694.txt` | CulturaX | 11,472 |  |

#### `utf-32le-zh/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_3.txt` | CulturaX | 8,380 |  |
| `culturax_mC4_5.txt` | CulturaX | 2,576 |  |
| `culturax_mC4_7.txt` | CulturaX | 4,292 |  |
| `sample_chinese_no_bom.txt` | charset-normalizer | 220 | BOM-less Chinese text (mixed simplified and traditional) |

#### `utf-7-ar/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 6,068 |  |
| `culturax_mC4_98635.txt` | CulturaX | 2,882 |  |
| `culturax_mC4_98638.txt` | CulturaX | 7,452 |  |

#### `utf-7-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77015.txt` | CulturaX | 6,884 |  |
| `culturax_mC4_77016.txt` | CulturaX | 3,077 |  |
| `culturax_mC4_77017.txt` | CulturaX | 5,676 |  |

#### `utf-7-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 3,391 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 3,164 |  |
| `culturax_mC4_84187.txt` | CulturaX | 5,933 |  |

#### `utf-7-br/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_43762.txt` | CulturaX | 593 |  |
| `culturax_OSCAR-2019_43764.txt` | CulturaX | 651 |  |

#### `utf-7-cs/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 1,715 |  |
| `culturax_mC4_98820.txt` | CulturaX | 1,895 |  |
| `culturax_mC4_98822.txt` | CulturaX | 3,931 |  |

#### `utf-7-cy/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78726.txt` | CulturaX | 2,783 |  |
| `culturax_mC4_78727.txt` | CulturaX | 2,911 |  |

#### `utf-7-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 2,107 |  |
| `culturax_mC4_83467.txt` | CulturaX | 3,240 |  |
| `culturax_mC4_83468.txt` | CulturaX | 2,129 |  |

#### `utf-7-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 2,694 |  |
| `culturax_mC4_83755.txt` | CulturaX | 2,241 |  |
| `culturax_mC4_83756.txt` | CulturaX | 2,375 |  |

#### `utf-7-el/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_greek.txt` | Ude | 1,462 |  |
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 7,056 |  |
| `culturax_mC4_103810.txt` | CulturaX | 5,177 |  |

#### `utf-7-en/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_3.txt` | Ude | 68 | Very small (68 bytes) |
| `reddit_wsb.csv` | unknown | 17,152,721 |  |

#### `utf-7-eo/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_40441.txt` | CulturaX | 3,093 |  |
| `culturax_mC4_40442.txt` | CulturaX | 1,416 |  |
| `culturax_mC4_40443.txt` | CulturaX | 2,717 |  |

#### `utf-7-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 1,569 |  |
| `culturax_mC4_87070.txt` | CulturaX | 3,175 |  |
| `culturax_mC4_87071.txt` | CulturaX | 2,948 |  |

#### `utf-7-et/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66818.txt` | CulturaX | 2,752 |  |
| `culturax_mC4_66819.txt` | CulturaX | 3,132 |  |
| `culturax_mC4_66820.txt` | CulturaX | 1,105 |  |

#### `utf-7-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_104835.txt` | CulturaX | 2,574 |  |
| `culturax_mC4_104836.txt` | CulturaX | 5,923 |  |
| `culturax_mC4_104837.txt` | CulturaX | 4,704 |  |

#### `utf-7-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80361.txt` | CulturaX | 1,025 |  |
| `culturax_mC4_80362.txt` | CulturaX | 3,316 |  |
| `culturax_mC4_80363.txt` | CulturaX | 3,192 |  |

#### `utf-7-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 3,142 |  |
| `culturax_OSCAR-2301_88370.txt` | CulturaX | 851 |  |
| `culturax_mC4_88369.txt` | CulturaX | 3,137 |  |

#### `utf-7-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63468.txt` | CulturaX | 3,509 |  |
| `culturax_mC4_63469.txt` | CulturaX | 3,679 |  |
| `culturax_mC4_63470.txt` | CulturaX | 1,484 |  |

#### `utf-7-gd/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 1,115 |  |
| `culturax_00001.txt` | CulturaX | 3,178 |  |
| `culturax_00002.txt` | CulturaX | 7,143 |  |

#### `utf-7-he/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_he3.txt` | Ude | 865 |  |
| `culturax_OSCAR-2301_58266.txt` | CulturaX | 7,113 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 7,592 |  |

#### `utf-7-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 113 |  |
| `culturax_00001.txt` | CulturaX | 172 |  |
| `culturax_00002.txt` | CulturaX | 619 |  |

#### `utf-7-hu/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 1,899 |  |
| `culturax_OSCAR-2301_82420.txt` | CulturaX | 2,278 |  |
| `culturax_mC4_82418.txt` | CulturaX | 918 |  |
| `weblabor.hu.xml` | chardet | 11,014 |  |

#### `utf-7-id/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 2,584 |  |

#### `utf-7-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 3,746 |  |
| `culturax_mC4_77488.txt` | CulturaX | 1,967 |  |
| `culturax_mC4_77489.txt` | CulturaX | 3,669 |  |

#### `utf-7-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 2,393 |  |
| `culturax_mC4_92390.txt` | CulturaX | 1,339 |  |
| `culturax_mC4_92391.txt` | CulturaX | 1,472 |  |

#### `utf-7-ja/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_mozilla_bug426271_text-utf-8.html` | Mozilla | 941 |  |
| `culturax_mC4_4.txt` | CulturaX | 7,207 |  |
| `culturax_mC4_5.txt` | CulturaX | 7,759 |  |

#### `utf-7-kk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 5,011 |  |
| `culturax_mC4_73161.txt` | CulturaX | 1,668 |  |
| `culturax_mC4_73162.txt` | CulturaX | 7,504 |  |

#### `utf-7-ko/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 568 |  |
| `_ude_2.txt` | Ude | 1,673 |  |
| `culturax_mC4_1.txt` | CulturaX | 7,183 |  |

#### `utf-7-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 3,239 |  |
| `culturax_mC4_73446.txt` | CulturaX | 3,175 |  |
| `culturax_mC4_73447.txt` | CulturaX | 3,562 |  |

#### `utf-7-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 3,927 |  |
| `culturax_mC4_71629.txt` | CulturaX | 1,744 |  |
| `culturax_mC4_71630.txt` | CulturaX | 992 |  |

#### `utf-7-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 5,974 |  |
| `culturax_mC4_102726.txt` | CulturaX | 3,075 |  |
| `culturax_mC4_102727.txt` | CulturaX | 7,528 |  |

#### `utf-7-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 270 |  |
| `culturax_00001.txt` | CulturaX | 502 |  |
| `culturax_00002.txt` | CulturaX | 1,683 |  |

#### `utf-7-mt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_51488.txt` | CulturaX | 2,109 |  |
| `culturax_mC4_51489.txt` | CulturaX | 3,622 |  |
| `culturax_mC4_51490.txt` | CulturaX | 1,240 |  |

#### `utf-7-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_107677.txt` | CulturaX | 1,366 |  |
| `culturax_mC4_107675.txt` | CulturaX | 2,467 |  |
| `culturax_mC4_107676.txt` | CulturaX | 1,051 |  |

#### `utf-7-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 2,676 |  |
| `culturax_mC4_66763.txt` | CulturaX | 3,251 |  |
| `culturax_mC4_66764.txt` | CulturaX | 3,242 |  |

#### `utf-7-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 2,201 |  |
| `culturax_mC4_97060.txt` | CulturaX | 1,477 |  |
| `culturax_mC4_97061.txt` | CulturaX | 2,894 |  |

#### `utf-7-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 972 |  |
| `culturax_mC4_101817.txt` | CulturaX | 3,185 |  |
| `culturax_mC4_101818.txt` | CulturaX | 3,059 |  |

#### `utf-7-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_78977.txt` | CulturaX | 923 |  |
| `culturax_mC4_78976.txt` | CulturaX | 3,177 |  |
| `culturax_mC4_78978.txt` | CulturaX | 3,083 |  |

#### `utf-7-ru/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_russian.txt` | Ude | 3,105 |  |
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 7,615 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 7,125 |  |

#### `utf-7-sk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95224.txt` | CulturaX | 1,871 |  |
| `culturax_mC4_95226.txt` | CulturaX | 2,701 |  |
| `culturax_mC4_95227.txt` | CulturaX | 3,672 |  |

#### `utf-7-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 3,159 |  |
| `culturax_mC4_66689.txt` | CulturaX | 2,953 |  |
| `culturax_mC4_66690.txt` | CulturaX | 1,285 |  |

#### `utf-7-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66918.txt` | CulturaX | 2,835 |  |
| `culturax_mC4_66920.txt` | CulturaX | 2,806 |  |
| `culturax_mC4_66921.txt` | CulturaX | 6,653 |  |

#### `utf-7-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 3,122 |  |
| `culturax_mC4_96486.txt` | CulturaX | 2,947 |  |
| `culturax_mC4_96487.txt` | CulturaX | 2,473 |  |

#### `utf-7-tg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_74865.txt` | CulturaX | 4,397 |  |
| `culturax_mC4_74866.txt` | CulturaX | 7,285 |  |
| `culturax_mC4_74867.txt` | CulturaX | 7,155 |  |

#### `utf-7-th/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_109134.txt` | CulturaX | 6,482 |  |
| `culturax_mC4_109133.txt` | CulturaX | 6,984 |  |
| `culturax_mC4_109136.txt` | CulturaX | 2,615 |  |

#### `utf-7-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 1,567 |  |
| `culturax_mC4_107849.txt` | CulturaX | 890 |  |
| `culturax_mC4_107850.txt` | CulturaX | 1,624 |  |

#### `utf-7-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95019.txt` | CulturaX | 2,661 |  |
| `culturax_mC4_95020.txt` | CulturaX | 3,410 |  |
| `culturax_mC4_95021.txt` | CulturaX | 7,187 |  |

#### `utf-7-ur/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_82296.txt` | CulturaX | 7,454 |  |
| `culturax_mC4_82297.txt` | CulturaX | 7,323 |  |
| `culturax_mC4_82298.txt` | CulturaX | 3,149 |  |

#### `utf-7-vi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_85695.txt` | CulturaX | 1,114 |  |
| `culturax_mC4_85693.txt` | CulturaX | 4,652 |  |
| `culturax_mC4_85694.txt` | CulturaX | 5,093 |  |

#### `utf-7-zh/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_5.txt` | Ude | 374 |  |
| `culturax_mC4_3.txt` | CulturaX | 5,335 |  |
| `culturax_mC4_5.txt` | CulturaX | 1,680 |  |

#### `utf-8-ar/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 4,292 |  |
| `culturax_mC4_98635.txt` | CulturaX | 2,029 |  |
| `culturax_mC4_98638.txt` | CulturaX | 5,266 |  |

#### `utf-8-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77015.txt` | CulturaX | 5,017 |  |
| `culturax_mC4_77016.txt` | CulturaX | 2,202 |  |
| `culturax_mC4_77017.txt` | CulturaX | 4,074 |  |

#### `utf-8-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 2,379 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 2,248 |  |
| `culturax_mC4_84187.txt` | CulturaX | 4,209 |  |

#### `utf-8-br/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_43762.txt` | CulturaX | 560 |  |
| `culturax_OSCAR-2019_43764.txt` | CulturaX | 635 |  |

#### `utf-8-cs/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 1,460 |  |
| `culturax_mC4_98820.txt` | CulturaX | 1,554 |  |
| `culturax_mC4_98822.txt` | CulturaX | 3,230 |  |

#### `utf-8-cy/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78726.txt` | CulturaX | 2,731 |  |
| `culturax_mC4_78727.txt` | CulturaX | 2,892 |  |

#### `utf-8-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 1,967 |  |
| `culturax_mC4_83467.txt` | CulturaX | 3,008 |  |
| `culturax_mC4_83468.txt` | CulturaX | 2,049 |  |

#### `utf-8-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 2,604 |  |
| `culturax_mC4_83755.txt` | CulturaX | 2,178 |  |
| `culturax_mC4_83756.txt` | CulturaX | 2,290 |  |

#### `utf-8-el/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_greek.txt` | Ude | 1,039 |  |
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 5,022 |  |
| `culturax_mC4_103810.txt` | CulturaX | 3,696 |  |
| `culturax_mC4_103811.txt` | CulturaX | 2,021 |  |

#### `utf-8-en/` — 15 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `CHANGELOG.md` | charset-normalizer | 10,072 | charset-normalizer CHANGELOG (near-ASCII, only 2 non-ASCII bytes) |
| `_mozilla_bug306272_text.html` | Mozilla | 227 | High markup ratio (72% tags) |
| `_ude_1.md` | charset-normalizer | 10,109 | charset-normalizer README (near-ASCII, 0.7% non-ASCII) |
| `_ude_1.rst` | charset-normalizer | 4,460 | urllib3 README (near-ASCII, 0.2% non-ASCII) |
| `_ude_3.txt` | Ude | 49 | Very small (49 bytes) |
| `_ude_6.txt` | Ude | 3,524 | English Wikipedia article about the English language |
| `anitabee.blogspot.com.xml` | chardet | 37,858 |  |
| `boobooo.blogspot.com.xml` | chardet | 12,982 | High markup ratio (62% tags) |
| `culturax_mC4_84512.txt` | CulturaX | 850 |  |
| `finnish-utf-8-latin-1-confusion.html` | unknown | 5,703 | Very high markup ratio (90% tags) |
| `iris-utf-8.csv` | unknown | 5,118 | Iris dataset, originally from Capital One DataProfiler |
| `iris-utf-8.json` | unknown | 19,153 | Iris dataset, originally from Capital One DataProfiler |
| `playlist.m3u` | unknown | 2,967 | M3U playlist file |
| `reddit_wsb.csv` | unknown | 16,984,308 | Large file (16,984,308 bytes); Reddit WallStreetBets data |
| `safari_misdetected.html` | unknown | 6,751 |  |

#### `utf-8-eo/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_40441.txt` | CulturaX | 2,970 |  |
| `culturax_mC4_40442.txt` | CulturaX | 1,345 |  |
| `culturax_mC4_40443.txt` | CulturaX | 2,655 |  |

#### `utf-8-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 1,491 |  |
| `culturax_mC4_87070.txt` | CulturaX | 3,026 |  |
| `culturax_mC4_87071.txt` | CulturaX | 2,822 |  |

#### `utf-8-et/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66818.txt` | CulturaX | 2,623 |  |
| `culturax_mC4_66819.txt` | CulturaX | 2,876 |  |
| `culturax_mC4_66820.txt` | CulturaX | 1,045 |  |

#### `utf-8-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_104835.txt` | CulturaX | 1,793 |  |
| `culturax_mC4_104836.txt` | CulturaX | 4,243 |  |
| `culturax_mC4_104837.txt` | CulturaX | 3,328 |  |

#### `utf-8-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80361.txt` | CulturaX | 911 |  |
| `culturax_mC4_80362.txt` | CulturaX | 3,094 |  |
| `culturax_mC4_80363.txt` | CulturaX | 2,920 |  |

#### `utf-8-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 2,945 |  |
| `culturax_OSCAR-2301_88370.txt` | CulturaX | 764 |  |
| `culturax_mC4_88369.txt` | CulturaX | 2,936 |  |

#### `utf-8-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63468.txt` | CulturaX | 3,080 |  |
| `culturax_mC4_63469.txt` | CulturaX | 3,106 |  |
| `culturax_mC4_63470.txt` | CulturaX | 1,294 |  |

#### `utf-8-gd/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 1,035 |  |
| `culturax_00001.txt` | CulturaX | 2,962 |  |
| `culturax_00002.txt` | CulturaX | 6,699 |  |

#### `utf-8-he/` — 6 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_he1.txt` | Ude | 1,187 |  |
| `_ude_he2.txt` | Ude | 2,893 |  |
| `_ude_he3.txt` | Ude | 612 |  |
| `culturax_OSCAR-2301_58265.txt` | CulturaX | 5,271 |  |
| `culturax_OSCAR-2301_58266.txt` | CulturaX | 5,111 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 5,301 |  |

#### `utf-8-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 107 |  |
| `culturax_00001.txt` | CulturaX | 149 |  |
| `culturax_00002.txt` | CulturaX | 587 |  |

#### `utf-8-hu/` — 8 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `balatonblog.typepad.com.xml` | chardet | 42,993 |  |
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 1,659 |  |
| `culturax_OSCAR-2301_82420.txt` | CulturaX | 1,757 |  |
| `culturax_mC4_82418.txt` | CulturaX | 702 |  |
| `linuxbox.hu.xml` | chardet | 14,178 |  |
| `pihgy.hu.xml` | chardet | 16,479 |  |
| `weblabor.hu.2.xml` | chardet | 12,234 |  |
| `weblabor.hu.xml` | chardet | 10,054 |  |

#### `utf-8-id/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 2,575 |  |

#### `utf-8-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 3,112 |  |
| `culturax_mC4_77488.txt` | CulturaX | 1,634 |  |
| `culturax_mC4_77489.txt` | CulturaX | 3,055 |  |

#### `utf-8-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 2,383 |  |
| `culturax_mC4_92390.txt` | CulturaX | 1,297 |  |
| `culturax_mC4_92391.txt` | CulturaX | 1,454 |  |

#### `utf-8-ja/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_mozilla_bug426271_text-utf-8.html` | Mozilla | 1,027 |  |
| `culturax_OSCAR-2301_6.txt` | CulturaX | 2,152 |  |
| `culturax_mC4_4.txt` | CulturaX | 7,544 |  |
| `culturax_mC4_5.txt` | CulturaX | 8,524 |  |

#### `utf-8-kk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 3,609 |  |
| `culturax_mC4_73161.txt` | CulturaX | 1,207 |  |
| `culturax_mC4_73162.txt` | CulturaX | 5,375 |  |

#### `utf-8-ko/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 549 |  |
| `_ude_2.txt` | Ude | 1,628 |  |
| `culturax_mC4_0.txt` | CulturaX | 4,201 |  |
| `culturax_mC4_1.txt` | CulturaX | 6,961 |  |
| `culturax_mC4_2.txt` | CulturaX | 1,479 |  |

#### `utf-8-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 2,844 |  |
| `culturax_mC4_73446.txt` | CulturaX | 2,818 |  |
| `culturax_mC4_73447.txt` | CulturaX | 3,156 |  |

#### `utf-8-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 3,225 |  |
| `culturax_mC4_71629.txt` | CulturaX | 1,426 |  |
| `culturax_mC4_71630.txt` | CulturaX | 828 |  |

#### `utf-8-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 4,234 |  |
| `culturax_mC4_102726.txt` | CulturaX | 2,169 |  |
| `culturax_mC4_102727.txt` | CulturaX | 5,376 |  |

#### `utf-8-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 267 |  |
| `culturax_00001.txt` | CulturaX | 503 |  |
| `culturax_00002.txt` | CulturaX | 1,684 |  |

#### `utf-8-mt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_51488.txt` | CulturaX | 1,876 |  |
| `culturax_mC4_51489.txt` | CulturaX | 3,153 |  |
| `culturax_mC4_51490.txt` | CulturaX | 1,134 |  |

#### `utf-8-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_107677.txt` | CulturaX | 1,363 |  |
| `culturax_mC4_107675.txt` | CulturaX | 2,459 |  |
| `culturax_mC4_107676.txt` | CulturaX | 1,045 |  |

#### `utf-8-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 2,549 |  |
| `culturax_mC4_66763.txt` | CulturaX | 3,067 |  |
| `culturax_mC4_66764.txt` | CulturaX | 3,066 |  |

#### `utf-8-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 1,921 |  |
| `culturax_mC4_97060.txt` | CulturaX | 1,349 |  |
| `culturax_mC4_97061.txt` | CulturaX | 2,528 |  |

#### `utf-8-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 957 |  |
| `culturax_mC4_101817.txt` | CulturaX | 2,965 |  |
| `culturax_mC4_101818.txt` | CulturaX | 2,899 |  |

#### `utf-8-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_78977.txt` | CulturaX | 811 |  |
| `culturax_mC4_78976.txt` | CulturaX | 2,782 |  |
| `culturax_mC4_78978.txt` | CulturaX | 2,829 |  |

#### `utf-8-ru/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_russian.txt` | Ude | 2,209 |  |
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 5,446 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 5,127 |  |
| `culturax_mC4_85056.txt` | CulturaX | 2,434 |  |

#### `utf-8-sig-ar/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 4,295 |  |
| `culturax_mC4_98635.txt` | CulturaX | 2,032 |  |
| `culturax_mC4_98638.txt` | CulturaX | 5,269 |  |

#### `utf-8-sig-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77015.txt` | CulturaX | 5,020 |  |
| `culturax_mC4_77016.txt` | CulturaX | 2,205 |  |
| `culturax_mC4_77017.txt` | CulturaX | 4,077 |  |

#### `utf-8-sig-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 2,382 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 2,251 |  |
| `culturax_mC4_84187.txt` | CulturaX | 4,212 |  |

#### `utf-8-sig-br/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_43762.txt` | CulturaX | 563 |  |
| `culturax_OSCAR-2019_43764.txt` | CulturaX | 638 |  |

#### `utf-8-sig-cs/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 1,463 |  |
| `culturax_mC4_98820.txt` | CulturaX | 1,557 |  |
| `culturax_mC4_98822.txt` | CulturaX | 3,233 |  |

#### `utf-8-sig-cy/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78726.txt` | CulturaX | 2,734 |  |
| `culturax_mC4_78727.txt` | CulturaX | 2,895 |  |
| `culturax_mC4_78728.txt` | CulturaX | 2,069 |  |

#### `utf-8-sig-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 1,970 |  |
| `culturax_mC4_83467.txt` | CulturaX | 3,011 |  |
| `culturax_mC4_83468.txt` | CulturaX | 2,052 |  |

#### `utf-8-sig-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 2,607 |  |
| `culturax_mC4_83755.txt` | CulturaX | 2,181 |  |
| `culturax_mC4_83756.txt` | CulturaX | 2,293 |  |

#### `utf-8-sig-el/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_greek.txt` | Ude | 1,042 |  |
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 5,025 |  |
| `culturax_mC4_103810.txt` | CulturaX | 3,699 |  |

#### `utf-8-sig-en/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `bom-utf-8.srt` | unknown | 859 | BOM detection test subtitle |

#### `utf-8-sig-eo/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_40441.txt` | CulturaX | 2,973 |  |
| `culturax_mC4_40442.txt` | CulturaX | 1,348 |  |
| `culturax_mC4_40443.txt` | CulturaX | 2,658 |  |

#### `utf-8-sig-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 1,494 |  |
| `culturax_mC4_87070.txt` | CulturaX | 3,029 |  |
| `culturax_mC4_87071.txt` | CulturaX | 2,825 |  |

#### `utf-8-sig-et/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66818.txt` | CulturaX | 2,626 |  |
| `culturax_mC4_66819.txt` | CulturaX | 2,879 |  |
| `culturax_mC4_66820.txt` | CulturaX | 1,048 |  |

#### `utf-8-sig-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_104835.txt` | CulturaX | 1,796 |  |
| `culturax_mC4_104836.txt` | CulturaX | 4,246 |  |
| `culturax_mC4_104837.txt` | CulturaX | 3,331 |  |

#### `utf-8-sig-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80361.txt` | CulturaX | 914 |  |
| `culturax_mC4_80362.txt` | CulturaX | 3,097 |  |
| `culturax_mC4_80363.txt` | CulturaX | 2,923 |  |

#### `utf-8-sig-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 2,948 |  |
| `culturax_OSCAR-2301_88370.txt` | CulturaX | 767 |  |
| `culturax_mC4_88369.txt` | CulturaX | 2,939 |  |

#### `utf-8-sig-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63468.txt` | CulturaX | 3,083 |  |
| `culturax_mC4_63469.txt` | CulturaX | 3,109 |  |
| `culturax_mC4_63470.txt` | CulturaX | 1,297 |  |

#### `utf-8-sig-gd/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 1,038 |  |
| `culturax_00001.txt` | CulturaX | 2,965 |  |
| `culturax_00002.txt` | CulturaX | 6,702 |  |

#### `utf-8-sig-he/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_he3.txt` | Ude | 615 |  |
| `culturax_OSCAR-2301_58266.txt` | CulturaX | 5,114 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 5,304 |  |

#### `utf-8-sig-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 110 |  |
| `culturax_00001.txt` | CulturaX | 152 |  |
| `culturax_00002.txt` | CulturaX | 590 |  |

#### `utf-8-sig-hu/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 1,662 |  |
| `culturax_OSCAR-2301_82420.txt` | CulturaX | 1,760 |  |
| `culturax_mC4_82418.txt` | CulturaX | 705 |  |

#### `utf-8-sig-id/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 2,578 |  |
| `culturax_mC4_114890.txt` | CulturaX | 2,690 |  |
| `culturax_mC4_114892.txt` | CulturaX | 1,533 |  |

#### `utf-8-sig-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 3,115 |  |
| `culturax_mC4_77488.txt` | CulturaX | 1,637 |  |
| `culturax_mC4_77489.txt` | CulturaX | 3,058 |  |

#### `utf-8-sig-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 2,386 |  |
| `culturax_mC4_92390.txt` | CulturaX | 1,300 |  |
| `culturax_mC4_92391.txt` | CulturaX | 1,457 |  |

#### `utf-8-sig-ja/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_mozilla_bug426271_text-utf-8.html` | Mozilla | 1,030 |  |
| `_ude_4.txt` | Ude | 1,729 |  |
| `culturax_mC4_4.txt` | CulturaX | 7,547 |  |
| `culturax_mC4_5.txt` | CulturaX | 8,527 |  |

#### `utf-8-sig-kk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 3,612 |  |
| `culturax_mC4_73161.txt` | CulturaX | 1,210 |  |
| `culturax_mC4_73162.txt` | CulturaX | 5,378 |  |

#### `utf-8-sig-ko/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 552 |  |
| `_ude_2.txt` | Ude | 1,631 |  |
| `culturax_mC4_1.txt` | CulturaX | 6,964 |  |

#### `utf-8-sig-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 2,847 |  |
| `culturax_mC4_73446.txt` | CulturaX | 2,821 |  |
| `culturax_mC4_73447.txt` | CulturaX | 3,159 |  |

#### `utf-8-sig-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 3,228 |  |
| `culturax_mC4_71629.txt` | CulturaX | 1,429 |  |
| `culturax_mC4_71630.txt` | CulturaX | 831 |  |

#### `utf-8-sig-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 4,237 |  |
| `culturax_mC4_102726.txt` | CulturaX | 2,172 |  |
| `culturax_mC4_102727.txt` | CulturaX | 5,379 |  |

#### `utf-8-sig-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 230 |  |
| `culturax_00001.txt` | CulturaX | 543 |  |
| `culturax_00002.txt` | CulturaX | 4,069 |  |

#### `utf-8-sig-mt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_51488.txt` | CulturaX | 1,879 |  |
| `culturax_mC4_51489.txt` | CulturaX | 3,156 |  |
| `culturax_mC4_51490.txt` | CulturaX | 1,137 |  |

#### `utf-8-sig-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_107677.txt` | CulturaX | 1,366 |  |
| `culturax_mC4_107675.txt` | CulturaX | 2,462 |  |
| `culturax_mC4_107676.txt` | CulturaX | 1,048 |  |

#### `utf-8-sig-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 2,552 |  |
| `culturax_mC4_66763.txt` | CulturaX | 3,070 |  |
| `culturax_mC4_66764.txt` | CulturaX | 3,069 |  |

#### `utf-8-sig-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 1,924 |  |
| `culturax_mC4_97060.txt` | CulturaX | 1,352 |  |
| `culturax_mC4_97061.txt` | CulturaX | 2,531 |  |

#### `utf-8-sig-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 960 |  |
| `culturax_mC4_101817.txt` | CulturaX | 2,968 |  |
| `culturax_mC4_101818.txt` | CulturaX | 2,902 |  |

#### `utf-8-sig-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_78977.txt` | CulturaX | 814 |  |
| `culturax_mC4_78976.txt` | CulturaX | 2,785 |  |
| `culturax_mC4_78978.txt` | CulturaX | 2,832 |  |

#### `utf-8-sig-ru/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_russian.txt` | Ude | 2,212 |  |
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 5,449 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 5,130 |  |

#### `utf-8-sig-sk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95224.txt` | CulturaX | 1,566 |  |
| `culturax_mC4_95226.txt` | CulturaX | 2,204 |  |
| `culturax_mC4_95227.txt` | CulturaX | 3,101 |  |

#### `utf-8-sig-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 2,963 |  |
| `culturax_mC4_66689.txt` | CulturaX | 2,733 |  |
| `culturax_mC4_66690.txt` | CulturaX | 1,217 |  |

#### `utf-8-sig-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66918.txt` | CulturaX | 2,040 |  |
| `culturax_mC4_66920.txt` | CulturaX | 2,010 |  |
| `culturax_mC4_66921.txt` | CulturaX | 4,721 |  |

#### `utf-8-sig-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 2,836 |  |
| `culturax_mC4_96486.txt` | CulturaX | 2,647 |  |
| `culturax_mC4_96487.txt` | CulturaX | 2,206 |  |

#### `utf-8-sig-tg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_74865.txt` | CulturaX | 3,142 |  |
| `culturax_mC4_74866.txt` | CulturaX | 5,150 |  |
| `culturax_mC4_74867.txt` | CulturaX | 5,100 |  |

#### `utf-8-sig-th/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_109134.txt` | CulturaX | 7,035 |  |
| `culturax_mC4_109133.txt` | CulturaX | 7,524 |  |
| `culturax_mC4_109136.txt` | CulturaX | 2,786 |  |

#### `utf-8-sig-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 1,277 |  |
| `culturax_mC4_107849.txt` | CulturaX | 778 |  |
| `culturax_mC4_107850.txt` | CulturaX | 1,423 |  |

#### `utf-8-sig-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95019.txt` | CulturaX | 1,923 |  |
| `culturax_mC4_95020.txt` | CulturaX | 2,458 |  |
| `culturax_mC4_95021.txt` | CulturaX | 5,177 |  |

#### `utf-8-sig-ur/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_82296.txt` | CulturaX | 5,142 |  |
| `culturax_mC4_82297.txt` | CulturaX | 5,087 |  |
| `culturax_mC4_82298.txt` | CulturaX | 2,174 |  |

#### `utf-8-sig-vi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_85695.txt` | CulturaX | 815 |  |
| `culturax_mC4_85693.txt` | CulturaX | 3,311 |  |
| `culturax_mC4_85694.txt` | CulturaX | 3,582 |  |

#### `utf-8-sig-zh/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_5.txt` | Ude | 410 |  |
| `culturax_mC4_3.txt` | CulturaX | 5,595 |  |
| `culturax_mC4_5.txt` | CulturaX | 1,813 |  |

#### `utf-8-sk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95224.txt` | CulturaX | 1,563 |  |
| `culturax_mC4_95226.txt` | CulturaX | 2,201 |  |
| `culturax_mC4_95227.txt` | CulturaX | 3,098 |  |

#### `utf-8-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 2,960 |  |
| `culturax_mC4_66689.txt` | CulturaX | 2,730 |  |
| `culturax_mC4_66690.txt` | CulturaX | 1,214 |  |

#### `utf-8-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66918.txt` | CulturaX | 2,037 |  |
| `culturax_mC4_66920.txt` | CulturaX | 2,007 |  |
| `culturax_mC4_66921.txt` | CulturaX | 4,718 |  |

#### `utf-8-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 2,833 |  |
| `culturax_mC4_96486.txt` | CulturaX | 2,644 |  |
| `culturax_mC4_96487.txt` | CulturaX | 2,203 |  |

#### `utf-8-tg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_74865.txt` | CulturaX | 3,139 |  |
| `culturax_mC4_74866.txt` | CulturaX | 5,147 |  |
| `culturax_mC4_74867.txt` | CulturaX | 5,097 |  |

#### `utf-8-th/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_109134.txt` | CulturaX | 7,032 |  |
| `culturax_mC4_109133.txt` | CulturaX | 7,521 |  |
| `culturax_mC4_109136.txt` | CulturaX | 2,783 |  |

#### `utf-8-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 1,274 |  |
| `culturax_mC4_107849.txt` | CulturaX | 775 |  |
| `culturax_mC4_107850.txt` | CulturaX | 1,420 |  |

#### `utf-8-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95019.txt` | CulturaX | 1,920 |  |
| `culturax_mC4_95020.txt` | CulturaX | 2,455 |  |
| `culturax_mC4_95021.txt` | CulturaX | 5,174 |  |

#### `utf-8-ur/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_82296.txt` | CulturaX | 5,139 |  |
| `culturax_mC4_82297.txt` | CulturaX | 5,084 |  |
| `culturax_mC4_82298.txt` | CulturaX | 2,171 |  |

#### `utf-8-vi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_85695.txt` | CulturaX | 812 |  |
| `culturax_mC4_85693.txt` | CulturaX | 3,308 |  |
| `culturax_mC4_85694.txt` | CulturaX | 3,579 |  |

#### `utf-8-zh/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_UTF-8_with_no_encoding_specified.html` | Chromium | 811 |  |
| `_ude_5.txt` | Ude | 407 |  |
| `culturax_mC4_3.txt` | CulturaX | 5,592 |  |
| `culturax_mC4_5.txt` | CulturaX | 1,810 |  |
| `culturax_mC4_7.txt` | CulturaX | 3,029 |  |

### ISO 8859 (281 files in 68 directories)

#### `iso-8859-1-da/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_iso_8859_1.txt` | uchardet | 529 |  |

#### `iso-8859-1-de/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_iso_8859_1.txt` | uchardet | 765 |  |

#### `iso-8859-1-en/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_mozilla_bug421271_text.html` | Mozilla | 671 |  |
| `ioreg_output.txt` | unknown | 748,505 | Large file (748,505 bytes); macOS ioreg command output, added for MacRoman prober testing |

#### `iso-8859-1-es/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_iso_8859_1.txt` | uchardet | 377 |  |
| `_ude_5.txt` | Ude | 1,639 |  |

#### `iso-8859-1-fi/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_6.txt` | Ude | 2,189 |  |
| `culturax_mC4_80364.txt` | CulturaX | 1,790 |  |

#### `iso-8859-1-fr/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_iso_8859_1.txt` | uchardet | 1,163 |  |
| `_ude_2.txt` | Ude | 2,010 |  |
| `archive_www_lefigaro_fr_20020601.txt` | Web Archive | 3,475 |  |
| `archive_www_lemonde_fr_20020601.txt` | Web Archive | 8,192 |  |
| `culturax_mC4_88375.txt` | CulturaX | 2,588 |  |

#### `iso-8859-1-id/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 2,693 |  |

#### `iso-8859-1-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 554 |  |
| `culturax_00001.txt` | CulturaX | 2,170 |  |
| `culturax_00002.txt` | CulturaX | 22,441 |  |

#### `iso-8859-1-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_3.txt` | Ude | 1,495 |  |
| `_ude_4.txt` | Ude | 1,222 |  |
| `archive_www_repubblica_it_20030601.txt` | Web Archive | 5,276 |  |

#### `iso-8859-1-ms/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 266 |  |

#### `iso-8859-1-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 887 |  |
| `culturax_00001.txt` | CulturaX | 3,566 |  |
| `culturax_00002.txt` | CulturaX | 7,922 |  |

#### `iso-8859-1-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 827 |  |
| `culturax_00001.txt` | CulturaX | 2,857 |  |
| `culturax_00002.txt` | CulturaX | 34,353 |  |

#### `iso-8859-1-pt/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 1,648 |  |
| `culturax_00000.txt` | CulturaX | 180 |  |
| `culturax_00001.txt` | CulturaX | 1,806 |  |
| `culturax_00002.txt` | CulturaX | 8,646 |  |

#### `iso-8859-1-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 209 |  |
| `culturax_00001.txt` | CulturaX | 1,395 |  |
| `culturax_00002.txt` | CulturaX | 4,993 |  |

#### `iso-8859-10-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 180 |  |
| `culturax_00001.txt` | CulturaX | 2,041 |  |
| `culturax_00002.txt` | CulturaX | 9,322 |  |

#### `iso-8859-10-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 500 |  |
| `culturax_00001.txt` | CulturaX | 1,930 |  |
| `culturax_00002.txt` | CulturaX | 5,304 |  |

#### `iso-8859-13-et/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 424 |  |
| `culturax_00001.txt` | CulturaX | 2,281 |  |
| `culturax_00002.txt` | CulturaX | 11,750 |  |

#### `iso-8859-13-lt/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_iso885913_lt.txt` | ENCA | 77 | Very small (77 bytes) |
| `culturax_mC4_73445.txt` | CulturaX | 2,682 |  |
| `culturax_mC4_73446.txt` | CulturaX | 2,683 |  |
| `culturax_mC4_73447.txt` | CulturaX | 3,000 |  |

#### `iso-8859-13-lv/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_iso885913_lv.txt` | ENCA | 71 | Very small (71 bytes) |
| `culturax_mC4_71628.txt` | CulturaX | 2,968 |  |
| `culturax_mC4_71629.txt` | CulturaX | 1,304 |  |
| `culturax_mC4_71630.txt` | CulturaX | 766 |  |

#### `iso-8859-14-br/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 215 |  |
| `culturax_00001.txt` | CulturaX | 253 |  |
| `culturax_00002.txt` | CulturaX | 86,172 |  |

#### `iso-8859-14-cy/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78730.txt` | CulturaX | 2,790 |  |

#### `iso-8859-14-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 611 |  |
| `culturax_00001.txt` | CulturaX | 3,112 |  |
| `culturax_00002.txt` | CulturaX | 7,210 |  |

#### `iso-8859-14-gd/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 1,008 |  |
| `culturax_00001.txt` | CulturaX | 2,889 |  |
| `culturax_00002.txt` | CulturaX | 6,550 |  |

#### `iso-8859-15-da/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_iso_8859_15.txt` | uchardet | 615 |  |

#### `iso-8859-15-de/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `archive_www_spiegel_de_20020601.txt` | Web Archive | 8,192 |  |

#### `iso-8859-15-en/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 2,042 |  |
| `culturax_00001.txt` | CulturaX | 2,871 |  |
| `culturax_00002.txt` | CulturaX | 3,372 |  |

#### `iso-8859-15-es/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_iso_8859_15.txt` | uchardet | 371 |  |

#### `iso-8859-15-fi/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80362.txt` | CulturaX | 3,000 |  |

#### `iso-8859-15-fr/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_iso_8859_15.txt` | uchardet | 976 |  |
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 2,871 |  |

#### `iso-8859-15-ga/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63469.txt` | CulturaX | 2,897 |  |

#### `iso-8859-15-id/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 1,000 |  |

#### `iso-8859-15-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 250 |  |
| `culturax_00001.txt` | CulturaX | 1,431 |  |
| `culturax_00002.txt` | CulturaX | 4,973 |  |

#### `iso-8859-15-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 570 |  |
| `culturax_00001.txt` | CulturaX | 3,135 |  |
| `culturax_00002.txt` | CulturaX | 28,207 |  |

#### `iso-8859-15-ms/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 250 |  |

#### `iso-8859-15-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 969 |  |
| `culturax_00001.txt` | CulturaX | 1,661 |  |
| `culturax_00002.txt` | CulturaX | 7,928 |  |

#### `iso-8859-15-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 500 |  |
| `culturax_00001.txt` | CulturaX | 2,328 |  |
| `culturax_00002.txt` | CulturaX | 10,616 |  |

#### `iso-8859-15-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 461 |  |
| `culturax_00001.txt` | CulturaX | 1,154 |  |
| `culturax_00002.txt` | CulturaX | 3,475 |  |

#### `iso-8859-15-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 219 |  |
| `culturax_00001.txt` | CulturaX | 1,145 |  |
| `culturax_00002.txt` | CulturaX | 4,565 |  |

#### `iso-8859-16-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 104 |  |
| `culturax_00001.txt` | CulturaX | 141 |  |
| `culturax_00002.txt` | CulturaX | 576 |  |

#### `iso-8859-16-hu/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_82421.txt` | CulturaX | 2,837 |  |
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 1,572 |  |

#### `iso-8859-16-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 1,815 |  |
| `culturax_mC4_97060.txt` | CulturaX | 1,300 |  |
| `culturax_mC4_97061.txt` | CulturaX | 2,383 |  |

#### `iso-8859-16-ro/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 3,964 |  |
| `culturax_OSCAR-2301_78981.txt` | CulturaX | 2,842 |  |
| `culturax_mC4_78979.txt` | CulturaX | 2,872 |  |
| `culturax_mC4_78980.txt` | CulturaX | 2,753 |  |

#### `iso-8859-16-sk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 186 |  |
| `culturax_00001.txt` | CulturaX | 2,100 |  |
| `culturax_00002.txt` | CulturaX | 16,421 |  |

#### `iso-8859-16-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 2,892 |  |
| `culturax_mC4_66689.txt` | CulturaX | 2,655 |  |
| `culturax_mC4_66690.txt` | CulturaX | 1,188 |  |

#### `iso-8859-2-cs/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 2,154 |  |
| `_ude_2.txt` | Ude | 1,646 |  |
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 1,362 |  |
| `culturax_mC4_98820.txt` | CulturaX | 1,424 |  |
| `culturax_mC4_98823.txt` | CulturaX | 1,627 |  |

#### `iso-8859-2-hr/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_iso88592_hr.txt` | ENCA | 127 |  |
| `_ude_1.txt` | Ude | 5,976 |  |

#### `iso-8859-2-hu/` — 19 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_iso88592_hu.txt` | ENCA | 65 | Very small (65 bytes) |
| `_uchardet_iso_8859_2.txt` | uchardet | 768 |  |
| `_ude_1.txt` | Ude | 2,696 |  |
| `_ude_2.txt` | Ude | 1,409 |  |
| `auto-apro.hu.xml` | chardet | 20,435 |  |
| `cigartower.hu.xml` | chardet | 5,447 |  |
| `culturax_OSCAR-2019_82421.txt` | CulturaX | 2,837 |  |
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 1,572 |  |
| `escience.hu.xml` | chardet | 13,696 |  |
| `hirtv.hu.xml` | chardet | 3,510 |  |
| `honositomuhely.hu.xml` | chardet | 4,275 |  |
| `objektivhir.hu.xml` | chardet | 13,417 |  |
| `saraspatak.hu.xml` | chardet | 7,095 |  |
| `shamalt.uw.hu.mk.xml` | chardet | 11,632 |  |
| `shamalt.uw.hu.mr.xml` | chardet | 4,950 |  |
| `shamalt.uw.hu.mv.xml` | chardet | 8,202 |  |
| `shamalt.uw.hu.xml` | chardet | 12,464 |  |
| `torokorszag.blogspot.com.xml` | chardet | 596,838 | Large file (596,838 bytes) |
| `ugyanmar.blogspot.com.xml` | chardet | 17,772 | High markup ratio (66% tags) |

#### `iso-8859-2-pl/` — 7 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_iso88592_pl.txt` | ENCA | 105 |  |
| `_ude_1.txt` | Ude | 3,413 |  |
| `archive_www_onet_pl_20030601.txt` | Web Archive | 4,810 |  |
| `archive_www_wp_pl_20030601.txt` | Web Archive | 5,126 |  |
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 1,815 |  |
| `culturax_mC4_97061.txt` | CulturaX | 2,383 |  |
| `culturax_mC4_97063.txt` | CulturaX | 501 |  |

#### `iso-8859-2-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 500 |  |
| `culturax_00001.txt` | CulturaX | 1,771 |  |
| `culturax_00002.txt` | CulturaX | 4,809 |  |

#### `iso-8859-2-sk/` — 6 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_iso88592_sk.txt` | ENCA | 135 |  |
| `_ude_1.txt` | Ude | 3,201 |  |
| `_ude_2.txt` | Ude | 1,136 |  |
| `culturax_mC4_95224.txt` | CulturaX | 1,445 |  |
| `culturax_mC4_95227.txt` | CulturaX | 2,868 |  |
| `culturax_mC4_95230.txt` | CulturaX | 2,928 |  |

#### `iso-8859-2-sl/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 3,861 |  |
| `culturax_mC4_66688.txt` | CulturaX | 2,892 |  |
| `culturax_mC4_66689.txt` | CulturaX | 2,655 |  |
| `culturax_mC4_66690.txt` | CulturaX | 1,188 |  |

#### `iso-8859-3-eo/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_iso_8859_3.txt` | uchardet | 532 |  |
| `culturax_mC4_40441.txt` | CulturaX | 2,927 |  |
| `culturax_mC4_40442.txt` | CulturaX | 1,319 |  |
| `culturax_mC4_40443.txt` | CulturaX | 2,632 |  |

#### `iso-8859-3-mt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_51488.txt` | CulturaX | 1,787 |  |
| `culturax_mC4_51489.txt` | CulturaX | 3,000 |  |
| `culturax_mC4_51490.txt` | CulturaX | 1,097 |  |

#### `iso-8859-3-tr/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_iso_8859_3.txt` | uchardet | 958 |  |
| `culturax_mC4_107848.txt` | CulturaX | 1,161 |  |
| `culturax_mC4_107849.txt` | CulturaX | 729 |  |
| `culturax_mC4_107851.txt` | CulturaX | 2,309 |  |

#### `iso-8859-4-et/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 2,894 |  |

#### `iso-8859-4-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 2,682 |  |
| `culturax_mC4_73446.txt` | CulturaX | 2,683 |  |
| `culturax_mC4_73448.txt` | CulturaX | 2,819 |  |

#### `iso-8859-4-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 2,968 |  |
| `culturax_mC4_71629.txt` | CulturaX | 1,304 |  |
| `culturax_mC4_71630.txt` | CulturaX | 766 |  |

#### `iso-8859-5-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77016.txt` | CulturaX | 1,188 |  |
| `culturax_mC4_77018.txt` | CulturaX | 1,326 |  |
| `culturax_mC4_77019.txt` | CulturaX | 1,915 |  |

#### `iso-8859-5-bg/` — 16 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `aero-bg.com.xml` | chardet | 11,527 | High markup ratio (76% tags) |
| `bbc.co.uk.popshow.xml` | chardet | 15,247 |  |
| `bpm.cult.bg.2.xml` | chardet | 12,151 |  |
| `bpm.cult.bg.4.xml` | chardet | 5,584 |  |
| `bpm.cult.bg.9.xml` | chardet | 12,433 |  |
| `bpm.cult.bg.medusa.4.xml` | chardet | 5,591 |  |
| `bpm.cult.bg.xml` | chardet | 2,932 | Very high markup ratio (84% tags) |
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 1,309 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 1,256 |  |
| `culturax_mC4_84187.txt` | CulturaX | 2,370 |  |
| `debian.gabrovo.com.news.xml` | chardet | 2,102 |  |
| `debian.gabrovo.com.xml` | chardet | 1,184 |  |
| `doncho.net.comments.xml` | chardet | 4,190 |  |
| `ecloga.cult.bg.xml` | chardet | 13,120 |  |
| `ide.li.xml` | chardet | 2,721 |  |
| `linux-bg.org.xml` | chardet | 3,009 |  |

#### `iso-8859-5-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 2,363 |  |
| `culturax_mC4_102726.txt` | CulturaX | 1,205 |  |
| `culturax_mC4_102727.txt` | CulturaX | 3,000 |  |

#### `iso-8859-5-ru/` — 23 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_ISO-8859-5_with_no_encoding_specified.html` | Chromium | 587 |  |
| `_enca_iso88595_ru.txt` | ENCA | 35 | Very small (35 bytes) |
| `_uchardet_iso_8859_5.txt` | uchardet | 245 |  |
| `aif.ru.health.xml` | chardet | 7,823 |  |
| `aug32.hole.ru.xml` | chardet | 629 |  |
| `aviaport.ru.xml` | chardet | 44,668 |  |
| `blog.mlmaster.com.xml` | chardet | 6,457 | High markup ratio (61% tags) |
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 3,000 |  |
| `culturax_OSCAR-2301_85060.txt` | CulturaX | 2,406 |  |
| `culturax_mC4_85059.txt` | CulturaX | 929 |  |
| `forum.template-toolkit.ru.1.xml` | chardet | 24,871 |  |
| `forum.template-toolkit.ru.4.xml` | chardet | 10,738 |  |
| `forum.template-toolkit.ru.6.xml` | chardet | 32,208 |  |
| `forum.template-toolkit.ru.8.xml` | chardet | 17,752 |  |
| `forum.template-toolkit.ru.9.xml` | chardet | 2,976 |  |
| `greek.ru.xml` | chardet | 2,061 |  |
| `intertat.ru.xml` | chardet | 583 |  |
| `janulalife.blogspot.com.xml` | chardet | 18,817 |  |
| `kapranoff.ru.xml` | chardet | 7,471 | High markup ratio (67% tags) |
| `money.rin.ru.xml` | chardet | 7,558 |  |
| `music.peeps.ru.xml` | chardet | 7,874 |  |
| `newsru.com.xml` | chardet | 24,107 |  |
| `susu.ac.ru.xml` | chardet | 1,214 |  |

#### `iso-8859-5-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66920.txt` | CulturaX | 1,123 |  |
| `culturax_mC4_66921.txt` | CulturaX | 2,630 |  |
| `culturax_mC4_66923.txt` | CulturaX | 2,867 |  |

#### `iso-8859-5-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_iso88595_uk.txt` | ENCA | 95 | Very small (95 bytes) |
| `culturax_mC4_95020.txt` | CulturaX | 1,371 |  |
| `culturax_mC4_95021.txt` | CulturaX | 2,821 |  |

#### `iso-8859-6-ar/` — 6 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_ISO-8859-6_with_no_encoding_specified.html` | Chromium | 605 |  |
| `_uchardet_iso_8859_6.txt` | uchardet | 214 |  |
| `_ude_1.txt` | Ude | 2,637 |  |
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 2,422 |  |
| `culturax_mC4_98635.txt` | CulturaX | 1,125 |  |
| `culturax_mC4_98641.txt` | CulturaX | 1,443 |  |

#### `iso-8859-6-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 423 |  |
| `culturax_00001.txt` | CulturaX | 2,339 |  |
| `culturax_00002.txt` | CulturaX | 17,334 |  |

#### `iso-8859-7-el/` — 17 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_ISO-8859-7_with_no_encoding_specified.html` | Chromium | 339 |  |
| `_uchardet_iso_8859_7.txt` | uchardet | 582 |  |
| `_ude_1.txt` | Ude | 1,639 |  |
| `_ude_2.txt` | Ude | 1,180 |  |
| `_ude_greek.txt` | Ude | 570 |  |
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 2,745 |  |
| `culturax_mC4_103810.txt` | CulturaX | 2,131 |  |
| `culturax_mC4_103811.txt` | CulturaX | 1,109 |  |
| `disabled.gr.xml` | chardet | 10,120 | High markup ratio (68% tags) |
| `hotstation.gr.xml` | chardet | 2,051 |  |
| `naftemporiki.gr.bus.xml` | chardet | 4,505 |  |
| `naftemporiki.gr.cmm.xml` | chardet | 4,072 |  |
| `naftemporiki.gr.fin.xml` | chardet | 4,473 |  |
| `naftemporiki.gr.mrk.xml` | chardet | 4,317 |  |
| `naftemporiki.gr.mrt.xml` | chardet | 4,523 |  |
| `naftemporiki.gr.spo.xml` | chardet | 4,372 |  |
| `naftemporiki.gr.wld.xml` | chardet | 4,576 |  |

#### `iso-8859-8-he/` — 21 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_ISO-8859-8_with_no_encoding_specified.html` | Chromium | 602 |  |
| `_uchardet_iso_8859_8.txt` | uchardet | 119 |  |
| `_ude_he1.txt` | Ude | 681 |  |
| `_ude_he2.txt` | Ude | 1,608 |  |
| `_ude_he3.txt` | Ude | 340 |  |
| `culturax_OSCAR-2301_58265.txt` | CulturaX | 2,987 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 2,987 |  |
| `culturax_OSCAR-2301_58268.txt` | CulturaX | 3,000 |  |
| `exego.net.2.xml` | chardet | 11,896 |  |
| `hagada.org.il.xml` | chardet | 3,687 |  |
| `halemo.net.edoar.xml` | chardet | 15,049 |  |
| `hevra.org.il.xml` | chardet | 2,111 |  |
| `info.org.il.xml` | chardet | 7,980 |  |
| `infomed.co.il.xml` | chardet | 8,119 |  |
| `law.co.il.xml` | chardet | 5,477 |  |
| `maakav.org.xml` | chardet | 1,835 |  |
| `notes.co.il.50.xml` | chardet | 10,945 |  |
| `notes.co.il.7.xml` | chardet | 9,972 |  |
| `notes.co.il.8.xml` | chardet | 10,860 |  |
| `pcplus.co.il.xml` | chardet | 1,652 |  |
| `sharks.co.il.xml` | chardet | 4,949 |  |

#### `iso-8859-9-tr/` — 10 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_windows-1254_with_no_encoding_specified.html` | Chromium | 340 |  |
| `_uchardet_iso_8859_9.txt` | uchardet | 958 |  |
| `_ude_1.txt` | Ude | 1,379 |  |
| `_ude_2.txt` | Ude | 2,394 |  |
| `culturax_mC4_107848.txt` | CulturaX | 1,161 |  |
| `culturax_mC4_107849.txt` | CulturaX | 729 |  |
| `culturax_mC4_107851.txt` | CulturaX | 2,309 |  |
| `divxplanet.com.xml` | chardet | 5,971 |  |
| `subtitle.srt` | unknown | 1,440 | Subtitle file |
| `wikitop_tr_ISO-8859-9.txt` | unknown | 1,840 |  |

### Windows code pages (158 files in 37 directories)

#### `windows-1250-cs/` — 6 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1250_cs.txt` | ENCA | 59 | Very small (59 bytes) |
| `_ude_1.txt` | Ude | 2,154 |  |
| `_ude_2.txt` | Ude | 1,646 |  |
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 1,362 |  |
| `culturax_mC4_98820.txt` | CulturaX | 1,424 |  |
| `culturax_mC4_98823.txt` | CulturaX | 1,627 |  |

#### `windows-1250-hr/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1250_hr.txt` | ENCA | 129 |  |
| `_ude_1.txt` | Ude | 5,976 |  |

#### `windows-1250-hu/` — 9 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1250_hu.txt` | ENCA | 66 | Very small (66 bytes) |
| `_uchardet_windows_1250.txt` | uchardet | 912 |  |
| `_ude_1.txt` | Ude | 1,685 |  |
| `_ude_2.txt` | Ude | 2,348 |  |
| `_ude_3.txt` | Ude | 2,009 |  |
| `bbc.co.uk.hu.forum.xml` | chardet | 21,564 |  |
| `bbc.co.uk.hu.learningenglish.xml` | chardet | 18,576 |  |
| `bbc.co.uk.hu.pressreview.xml` | chardet | 17,091 |  |
| `bbc.co.uk.hu.xml` | chardet | 46,615 |  |

#### `windows-1250-pl/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1250_pl.txt` | ENCA | 106 |  |
| `_ude_1.txt` | Ude | 3,413 |  |
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 1,815 |  |
| `culturax_mC4_97060.txt` | CulturaX | 1,300 |  |
| `culturax_mC4_97061.txt` | CulturaX | 2,383 |  |

#### `windows-1250-ro/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 738 |  |
| `culturax_00001.txt` | CulturaX | 1,852 |  |
| `culturax_00002.txt` | CulturaX | 7,539 |  |

#### `windows-1250-sk/` — 6 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 1,293 |  |
| `_ude_2.txt` | Ude | 1,136 |  |
| `_ude_3.txt` | Ude | 3,201 |  |
| `culturax_mC4_95224.txt` | CulturaX | 1,445 |  |
| `culturax_mC4_95226.txt` | CulturaX | 2,004 |  |
| `culturax_mC4_95227.txt` | CulturaX | 2,868 |  |

#### `windows-1250-sl/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1250_sl.txt` | ENCA | 75 | Very small (75 bytes) |
| `_ude_1.txt` | Ude | 2,535 |  |
| `culturax_mC4_66688.txt` | CulturaX | 2,892 |  |
| `culturax_mC4_66689.txt` | CulturaX | 2,655 |  |
| `culturax_mC4_66690.txt` | CulturaX | 1,188 |  |

#### `windows-1250-sr/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `github_bug_672.txt` | charset-normalizer | 56,423 | Serbian subtitle file from charset-normalizer issue #672 |

#### `windows-1251-be/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1251_be.txt` | ENCA | 25 | Very small (25 bytes) |
| `culturax_mC4_77015.txt` | CulturaX | 2,933 |  |
| `culturax_mC4_77016.txt` | CulturaX | 1,188 |  |
| `culturax_mC4_77018.txt` | CulturaX | 1,326 |  |

#### `windows-1251-bg/` — 21 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1251_bg.txt` | ENCA | 53 | Very small (53 bytes) |
| `_uchardet_windows_1251.txt` | uchardet | 358 |  |
| `bbc.co.uk.popshow.xml` | chardet | 28,125 |  |
| `bpm.cult.bg.2.xml` | chardet | 12,193 |  |
| `bpm.cult.bg.3.xml` | chardet | 11,184 | High markup ratio (65% tags) |
| `bpm.cult.bg.4.xml` | chardet | 11,873 | High markup ratio (61% tags) |
| `bpm.cult.bg.9.xml` | chardet | 12,546 |  |
| `bpm.cult.bg.medusa.4.xml` | chardet | 11,880 | High markup ratio (61% tags) |
| `bpm.cult.bg.xml` | chardet | 6,568 | Very high markup ratio (84% tags) |
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 1,309 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 1,256 |  |
| `culturax_mC4_84187.txt` | CulturaX | 2,370 |  |
| `debian.gabrovo.com.news.xml` | chardet | 2,115 |  |
| `debian.gabrovo.com.xml` | chardet | 1,197 |  |
| `doncho.net.comments.xml` | chardet | 9,587 |  |
| `doncho.net.xml` | chardet | 9,177 |  |
| `ecloga.cult.bg.xml` | chardet | 13,203 |  |
| `ide.li.xml` | chardet | 3,963 |  |
| `informator.org.xml` | chardet | 2,026 |  |
| `linux-bg.org.xml` | chardet | 3,030 |  |
| `rinennor.org.xml` | chardet | 5,559 |  |

#### `windows-1251-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 2,363 |  |
| `culturax_mC4_102726.txt` | CulturaX | 1,205 |  |
| `culturax_mC4_102727.txt` | CulturaX | 3,000 |  |

#### `windows-1251-ru/` — 27 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_windows-1251_with_no_encoding_specified.html` | Chromium | 589 |  |
| `_enca_cp1251_ru.txt` | ENCA | 36 | Very small (36 bytes) |
| `_uchardet_windows_1251.txt` | uchardet | 879 |  |
| `_ude_1.txt` | Ude | 1,211 |  |
| `aif.ru.health.xml` | chardet | 7,827 |  |
| `anthropology.ru.xml` | chardet | 10,482 |  |
| `archive_lenta_ru_20050601.txt` | Web Archive | 8,192 |  |
| `archive_www_mail_ru_20050601.txt` | Web Archive | 3,546 |  |
| `archive_www_rbc_ru_20050601.txt` | Web Archive | 8,192 |  |
| `aug32.hole.ru.xml` | chardet | 633 |  |
| `aviaport.ru.xml` | chardet | 60,039 |  |
| `blog.mlmaster.com.xml` | chardet | 6,461 | High markup ratio (61% tags) |
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 3,000 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 2,831 |  |
| `culturax_mC4_85056.txt` | CulturaX | 1,326 |  |
| `forum.template-toolkit.ru.1.xml` | chardet | 24,875 |  |
| `forum.template-toolkit.ru.4.xml` | chardet | 10,742 |  |
| `forum.template-toolkit.ru.6.xml` | chardet | 32,212 |  |
| `forum.template-toolkit.ru.8.xml` | chardet | 17,756 |  |
| `forum.template-toolkit.ru.9.xml` | chardet | 2,980 |  |
| `greek.ru.xml` | chardet | 2,065 |  |
| `intertat.ru.xml` | chardet | 587 |  |
| `janulalife.blogspot.com.xml` | chardet | 18,821 |  |
| `kapranoff.ru.xml` | chardet | 7,475 | High markup ratio (67% tags) |
| `money.rin.ru.xml` | chardet | 7,562 |  |
| `music.peeps.ru.xml` | chardet | 7,878 |  |
| `newsru.com.xml` | chardet | 24,111 |  |

#### `windows-1251-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66920.txt` | CulturaX | 1,123 |  |
| `culturax_mC4_66921.txt` | CulturaX | 2,630 |  |
| `culturax_mC4_66923.txt` | CulturaX | 2,867 |  |

#### `windows-1251-uk/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1251_uk.txt` | ENCA | 96 | Very small (96 bytes) |
| `culturax_mC4_95019.txt` | CulturaX | 1,080 |  |
| `culturax_mC4_95020.txt` | CulturaX | 1,371 |  |
| `culturax_mC4_95021.txt` | CulturaX | 2,821 |  |

#### `windows-1252-da/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_windows_1252.txt` | uchardet | 615 |  |
| `culturax_mC4_83469.txt` | CulturaX | 2,827 |  |

#### `windows-1252-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_windows_1252.txt` | uchardet | 765 |  |
| `_ude_1.txt` | Ude | 865 |  |
| `anzeige-value-stars.html` | charset-normalizer | 210,655 |  |

#### `windows-1252-en/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `github_bug_9.txt` | unknown | 136 | Regression test for chardet [issue #9](https://github.com/chardet/chardet/issues/9) |

#### `windows-1252-es/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_windows_1252.txt` | uchardet | 371 |  |

#### `windows-1252-fi/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80362.txt` | CulturaX | 3,000 |  |

#### `windows-1252-fr/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_windows_1252.txt` | uchardet | 163 |  |
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 2,871 |  |

#### `windows-1252-ga/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63469.txt` | CulturaX | 2,897 |  |

#### `windows-1252-id/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 500 |  |

#### `windows-1252-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 581 |  |
| `culturax_00001.txt` | CulturaX | 1,075 |  |
| `culturax_00002.txt` | CulturaX | 3,787 |  |

#### `windows-1252-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 500 |  |
| `culturax_00001.txt` | CulturaX | 1,476 |  |
| `culturax_00002.txt` | CulturaX | 11,839 |  |

#### `windows-1252-nl/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_2.txt` | Ude | 2,257 |  |
| `culturax_00000.txt` | CulturaX | 500 |  |
| `culturax_00001.txt` | CulturaX | 1,000 |  |
| `culturax_00002.txt` | CulturaX | 7,928 |  |

#### `windows-1252-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 250 |  |
| `culturax_00001.txt` | CulturaX | 2,283 |  |
| `culturax_00002.txt` | CulturaX | 10,617 |  |

#### `windows-1252-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 250 |  |
| `culturax_00001.txt` | CulturaX | 1,000 |  |
| `culturax_00002.txt` | CulturaX | 3,142 |  |

#### `windows-1252-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 271 |  |
| `culturax_00001.txt` | CulturaX | 1,071 |  |
| `culturax_00002.txt` | CulturaX | 3,754 |  |

#### `windows-1253-el/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_windows_1253.txt` | uchardet | 467 |  |
| `culturax_mC4_103810.txt` | CulturaX | 2,131 |  |

#### `windows-1254-tr/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 2,088 |  |

#### `windows-1255-he/` — 7 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_windows-1255_with_no_encoding_specified.html` | Chromium | 604 |  |
| `_uchardet_windows_1255.txt` | uchardet | 152 |  |
| `carshops.co.il.xml` | chardet | 142,386 | Large file (142,386 bytes); High markup ratio (72% tags) |
| `hydepark.hevre.co.il.7957.xml` | chardet | 82,358 |  |
| `neviim.net.xml` | chardet | 7,245 |  |
| `notes.co.il.6.xml` | chardet | 10,056 |  |
| `whatsup.org.il.xml` | chardet | 8,755 |  |

#### `windows-1256-ar/` — 6 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_windows-1256_with_no_encoding_specified.html` | Chromium | 607 |  |
| `_uchardet_windows_1256.txt` | uchardet | 214 |  |
| `_ude_1.txt` | Ude | 2,637 |  |
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 2,422 |  |
| `culturax_mC4_98635.txt` | CulturaX | 1,125 |  |
| `culturax_mC4_98641.txt` | CulturaX | 1,443 |  |

#### `windows-1256-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 385 |  |
| `culturax_00001.txt` | CulturaX | 2,140 |  |
| `culturax_00002.txt` | CulturaX | 15,829 |  |

#### `windows-1257-et/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1257_et.txt` | ENCA | 81 | Very small (81 bytes) |
| `_ude_1.txt` | Ude | 1,532 |  |

#### `windows-1257-lt/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1257_lt.txt` | ENCA | 78 | Very small (78 bytes) |

#### `windows-1257-lv/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1257_lv.txt` | ENCA | 72 | Very small (72 bytes) |

#### `windows-1258-vi/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_2.txt` | Ude | 276 |  |
| `culturax_OSCAR-2019_85698.txt` | CulturaX | 1,290 |  |
| `culturax_OSCAR-2109_85695.txt` | CulturaX | 670 |  |
| `culturax_mC4_85693.txt` | CulturaX | 2,668 |  |
| `culturax_mC4_85696.txt` | CulturaX | 1,573 |  |

### IBM/DOS code pages (322 files in 113 directories)

#### `cp037-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 1,918 |  |
| `culturax_mC4_83468.txt` | CulturaX | 2,019 |  |
| `culturax_mC4_83470.txt` | CulturaX | 2,853 |  |

#### `cp037-de/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 2,576 |  |
| `culturax_mC4_83756.txt` | CulturaX | 2,259 |  |

#### `cp037-en/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 658 |  |
| `culturax_mC4_84512.txt` | CulturaX | 849 |  |

#### `cp037-es/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 1,463 |  |
| `culturax_mC4_87070.txt` | CulturaX | 2,970 |  |

#### `cp037-fi/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80364.txt` | CulturaX | 1,790 |  |

#### `cp037-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 912 |  |
| `culturax_00001.txt` | CulturaX | 3,387 |  |
| `culturax_00002.txt` | CulturaX | 9,484 |  |

#### `cp037-id/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 2,572 |  |
| `culturax_mC4_114892.txt` | CulturaX | 1,530 |  |

#### `cp037-is/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77489.txt` | CulturaX | 2,808 |  |

#### `cp037-it/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 2,378 |  |
| `culturax_mC4_92391.txt` | CulturaX | 1,445 |  |

#### `cp037-ms/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00002.txt` | CulturaX | 4,066 |  |

#### `cp037-nl/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107675.txt` | CulturaX | 2,455 |  |

#### `cp037-no/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 2,502 |  |
| `culturax_mC4_66764.txt` | CulturaX | 3,000 |  |

#### `cp037-pt/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_101817.txt` | CulturaX | 2,875 |  |

#### `cp037-sv/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 2,733 |  |
| `culturax_mC4_96486.txt` | CulturaX | 2,533 |  |

#### `cp037-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 245 |  |
| `culturax_00001.txt` | CulturaX | 2,169 |  |
| `culturax_00002.txt` | CulturaX | 6,157 |  |

#### `cp1006-ur/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 672 |  |
| `culturax_00001.txt` | CulturaX | 2,422 |  |
| `culturax_00002.txt` | CulturaX | 8,191 |  |

#### `cp1026-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 1,161 |  |
| `culturax_mC4_107849.txt` | CulturaX | 729 |  |
| `culturax_mC4_107851.txt` | CulturaX | 2,309 |  |

#### `cp1125-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_cp1125_uk.txt` | ENCA | 96 | Very small (96 bytes) |
| `culturax_mC4_95020.txt` | CulturaX | 1,371 |  |
| `culturax_mC4_95021.txt` | CulturaX | 2,821 |  |

#### `cp273-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 219 |  |
| `culturax_00001.txt` | CulturaX | 2,076 |  |
| `culturax_00002.txt` | CulturaX | 7,474 |  |

#### `cp424-he/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 205 |  |
| `culturax_OSCAR-2301_58265.txt` | CulturaX | 2,987 |  |
| `culturax_OSCAR-2301_58267.txt` | CulturaX | 2,987 |  |
| `culturax_OSCAR-2301_58268.txt` | CulturaX | 3,000 |  |

#### `cp437-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 266 |  |
| `culturax_00001.txt` | CulturaX | 2,059 |  |
| `culturax_00002.txt` | CulturaX | 19,277 |  |

#### `cp437-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 219 |  |
| `culturax_00001.txt` | CulturaX | 2,076 |  |
| `culturax_00002.txt` | CulturaX | 7,474 |  |

#### `cp437-en/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 2,042 |  |
| `culturax_00001.txt` | CulturaX | 2,871 |  |
| `culturax_00002.txt` | CulturaX | 4,191 |  |

#### `cp437-es/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87073.txt` | CulturaX | 1,577 |  |

#### `cp437-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 180 |  |
| `culturax_00001.txt` | CulturaX | 2,041 |  |
| `culturax_00002.txt` | CulturaX | 9,321 |  |

#### `cp437-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 129 |  |
| `culturax_00001.txt` | CulturaX | 1,979 |  |
| `culturax_00002.txt` | CulturaX | 9,483 |  |

#### `cp437-ga/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63473.txt` | CulturaX | 2,786 |  |

#### `cp437-it/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92393.txt` | CulturaX | 1,985 |  |
| `culturax_mC4_92395.txt` | CulturaX | 1,392 |  |

#### `cp437-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 886 |  |
| `culturax_00001.txt` | CulturaX | 3,566 |  |
| `culturax_00002.txt` | CulturaX | 7,922 |  |

#### `cp437-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 177 |  |
| `culturax_00001.txt` | CulturaX | 1,795 |  |
| `culturax_00002.txt` | CulturaX | 8,570 |  |

#### `cp437-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 209 |  |
| `culturax_00001.txt` | CulturaX | 1,395 |  |
| `culturax_00002.txt` | CulturaX | 4,993 |  |

#### `cp500-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 1,918 |  |
| `culturax_mC4_83468.txt` | CulturaX | 2,019 |  |
| `culturax_mC4_83470.txt` | CulturaX | 2,853 |  |

#### `cp500-de/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 2,576 |  |
| `culturax_mC4_83756.txt` | CulturaX | 2,259 |  |

#### `cp500-en/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_84512.txt` | CulturaX | 849 |  |

#### `cp500-es/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 1,463 |  |
| `culturax_mC4_87070.txt` | CulturaX | 2,970 |  |

#### `cp500-fi/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80364.txt` | CulturaX | 1,790 |  |

#### `cp500-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 911 |  |
| `culturax_00001.txt` | CulturaX | 2,710 |  |
| `culturax_00002.txt` | CulturaX | 9,484 |  |

#### `cp500-id/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 2,572 |  |
| `culturax_mC4_114892.txt` | CulturaX | 1,530 |  |

#### `cp500-is/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77489.txt` | CulturaX | 2,808 |  |

#### `cp500-it/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 2,378 |  |
| `culturax_mC4_92391.txt` | CulturaX | 1,445 |  |

#### `cp500-ms/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00002.txt` | CulturaX | 4,066 |  |

#### `cp500-nl/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107675.txt` | CulturaX | 2,455 |  |

#### `cp500-no/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 2,502 |  |
| `culturax_mC4_66764.txt` | CulturaX | 3,000 |  |

#### `cp500-pt/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_101817.txt` | CulturaX | 2,875 |  |

#### `cp500-sv/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 2,733 |  |
| `culturax_mC4_96486.txt` | CulturaX | 2,533 |  |

#### `cp720-ar/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_98639.txt` | CulturaX | 2,422 |  |
| `culturax_mC4_98635.txt` | CulturaX | 1,125 |  |
| `culturax_mC4_98641.txt` | CulturaX | 1,443 |  |

#### `cp720-fa/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 440 |  |
| `culturax_00001.txt` | CulturaX | 1,672 |  |
| `culturax_00002.txt` | CulturaX | 8,850 |  |

#### `cp737-el/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_103810.txt` | CulturaX | 2,131 |  |

#### `cp775-et/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 1,532 |  |
| `culturax_mC4_66818.txt` | CulturaX | 2,580 |  |
| `culturax_mC4_66820.txt` | CulturaX | 1,025 |  |
| `culturax_mC4_66822.txt` | CulturaX | 2,894 |  |

#### `cp775-lt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73445.txt` | CulturaX | 2,682 |  |
| `culturax_mC4_73446.txt` | CulturaX | 2,683 |  |
| `culturax_mC4_73447.txt` | CulturaX | 3,000 |  |

#### `cp775-lv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_71628.txt` | CulturaX | 2,968 |  |
| `culturax_mC4_71629.txt` | CulturaX | 1,304 |  |
| `culturax_mC4_71630.txt` | CulturaX | 766 |  |

#### `cp850-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 272 |  |
| `culturax_00001.txt` | CulturaX | 2,074 |  |
| `culturax_00002.txt` | CulturaX | 19,561 |  |

#### `cp850-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 353 |  |
| `culturax_00001.txt` | CulturaX | 1,963 |  |
| `culturax_00002.txt` | CulturaX | 7,170 |  |

#### `cp850-en/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 2,000 |  |
| `culturax_00001.txt` | CulturaX | 3,371 |  |

#### `cp850-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 652 |  |
| `culturax_00001.txt` | CulturaX | 3,158 |  |
| `culturax_00002.txt` | CulturaX | 18,231 |  |

#### `cp850-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 291 |  |
| `culturax_00001.txt` | CulturaX | 1,265 |  |
| `culturax_00002.txt` | CulturaX | 6,844 |  |

#### `cp850-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 185 |  |
| `culturax_00001.txt` | CulturaX | 1,338 |  |
| `culturax_00002.txt` | CulturaX | 9,484 |  |

#### `cp850-id/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 2,693 |  |

#### `cp850-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 554 |  |
| `culturax_00001.txt` | CulturaX | 2,170 |  |
| `culturax_00002.txt` | CulturaX | 22,441 |  |

#### `cp850-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 570 |  |
| `culturax_00001.txt` | CulturaX | 3,135 |  |
| `culturax_00002.txt` | CulturaX | 28,207 |  |

#### `cp850-ms/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 266 |  |

#### `cp850-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 887 |  |
| `culturax_00001.txt` | CulturaX | 1,661 |  |
| `culturax_00002.txt` | CulturaX | 5,386 |  |

#### `cp850-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 827 |  |
| `culturax_00001.txt` | CulturaX | 2,857 |  |
| `culturax_00002.txt` | CulturaX | 34,353 |  |

#### `cp850-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 180 |  |
| `culturax_00001.txt` | CulturaX | 1,806 |  |
| `culturax_00002.txt` | CulturaX | 8,646 |  |

#### `cp850-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 219 |  |
| `culturax_00001.txt` | CulturaX | 1,145 |  |
| `culturax_00002.txt` | CulturaX | 4,565 |  |

#### `cp852-cs/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 7,598 |  |
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 1,362 |  |
| `culturax_mC4_98820.txt` | CulturaX | 1,424 |  |
| `culturax_mC4_98823.txt` | CulturaX | 1,627 |  |

#### `cp852-hr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 104 |  |
| `culturax_00001.txt` | CulturaX | 141 |  |
| `culturax_00002.txt` | CulturaX | 576 |  |

#### `cp852-hu/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_82421.txt` | CulturaX | 2,837 |  |
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 1,572 |  |
| `culturax_mC4_82418.txt` | CulturaX | 627 |  |

#### `cp852-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 1,815 |  |
| `culturax_mC4_97060.txt` | CulturaX | 1,300 |  |
| `culturax_mC4_97061.txt` | CulturaX | 2,383 |  |

#### `cp852-ro/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_78977.txt` | CulturaX | 771 |  |
| `culturax_mC4_78976.txt` | CulturaX | 2,633 |  |
| `culturax_mC4_78978.txt` | CulturaX | 2,727 |  |
| `culturax_mC4_78979.txt` | CulturaX | 2,872 |  |

#### `cp852-sk/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_ibm852_sk.txt` | ENCA | 136 |  |
| `culturax_mC4_95224.txt` | CulturaX | 1,445 |  |
| `culturax_mC4_95227.txt` | CulturaX | 2,868 |  |
| `culturax_mC4_95230.txt` | CulturaX | 2,928 |  |

#### `cp852-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 2,892 |  |
| `culturax_mC4_66689.txt` | CulturaX | 2,655 |  |
| `culturax_mC4_66690.txt` | CulturaX | 1,188 |  |

#### `cp855-be/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77015.txt` | CulturaX | 2,933 |  |
| `culturax_mC4_77016.txt` | CulturaX | 1,188 |  |
| `culturax_mC4_77018.txt` | CulturaX | 1,326 |  |

#### `cp855-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 1,309 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 1,256 |  |
| `culturax_mC4_84187.txt` | CulturaX | 2,370 |  |

#### `cp855-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 2,363 |  |
| `culturax_mC4_102726.txt` | CulturaX | 1,205 |  |
| `culturax_mC4_102727.txt` | CulturaX | 3,000 |  |

#### `cp855-ru/` — 23 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_ibm855.txt` | uchardet | 338 |  |
| `_ude_1.txt` | Ude | 1,211 |  |
| `_ude_2.txt` | Ude | 3,067 |  |
| `aif.ru.health.xml` | chardet | 7,815 |  |
| `aug32.hole.ru.xml` | chardet | 621 |  |
| `aviaport.ru.xml` | chardet | 44,660 |  |
| `blog.mlmaster.com.xml` | chardet | 6,449 | High markup ratio (61% tags) |
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 3,000 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 2,831 |  |
| `culturax_mC4_85056.txt` | CulturaX | 1,326 |  |
| `forum.template-toolkit.ru.1.xml` | chardet | 24,863 |  |
| `forum.template-toolkit.ru.4.xml` | chardet | 10,730 |  |
| `forum.template-toolkit.ru.6.xml` | chardet | 32,200 |  |
| `forum.template-toolkit.ru.8.xml` | chardet | 17,744 |  |
| `forum.template-toolkit.ru.9.xml` | chardet | 2,968 |  |
| `greek.ru.xml` | chardet | 2,948 |  |
| `intertat.ru.xml` | chardet | 575 |  |
| `janulalife.blogspot.com.xml` | chardet | 18,809 |  |
| `kapranoff.ru.xml` | chardet | 7,463 | High markup ratio (67% tags) |
| `money.rin.ru.xml` | chardet | 7,550 |  |
| `music.peeps.ru.xml` | chardet | 7,866 |  |
| `newsru.com.xml` | chardet | 24,099 |  |
| `susu.ac.ru.xml` | chardet | 2,059 |  |

#### `cp855-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66920.txt` | CulturaX | 1,123 |  |
| `culturax_mC4_66921.txt` | CulturaX | 2,630 |  |
| `culturax_mC4_66923.txt` | CulturaX | 2,867 |  |

#### `cp855-uk/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_ibm855_uk.txt` | ENCA | 96 | Very small (96 bytes) |
| `culturax_mC4_95019.txt` | CulturaX | 1,080 |  |
| `culturax_mC4_95020.txt` | CulturaX | 1,371 |  |
| `culturax_mC4_95021.txt` | CulturaX | 2,821 |  |

#### `cp856-he/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 119 |  |
| `culturax_00001.txt` | CulturaX | 1,217 |  |
| `culturax_00002.txt` | CulturaX | 9,489 |  |

#### `cp857-tr/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 1,583 |  |
| `culturax_mC4_107848.txt` | CulturaX | 1,161 |  |
| `culturax_mC4_107849.txt` | CulturaX | 729 |  |
| `culturax_mC4_107851.txt` | CulturaX | 2,309 |  |

#### `cp858-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 250 |  |
| `culturax_00001.txt` | CulturaX | 1,629 |  |
| `culturax_00002.txt` | CulturaX | 9,021 |  |

#### `cp858-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 250 |  |
| `culturax_00001.txt` | CulturaX | 1,000 |  |
| `culturax_00002.txt` | CulturaX | 7,084 |  |

#### `cp858-en/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 3,372 |  |

#### `cp858-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 500 |  |
| `culturax_00001.txt` | CulturaX | 2,546 |  |
| `culturax_00002.txt` | CulturaX | 12,119 |  |

#### `cp858-fi/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80362.txt` | CulturaX | 3,000 |  |

#### `cp858-fr/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 2,871 |  |

#### `cp858-ga/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63469.txt` | CulturaX | 2,897 |  |

#### `cp858-id/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 1,000 |  |

#### `cp858-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 500 |  |
| `culturax_00001.txt` | CulturaX | 1,930 |  |
| `culturax_00002.txt` | CulturaX | 5,304 |  |

#### `cp858-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 500 |  |
| `culturax_00001.txt` | CulturaX | 1,476 |  |
| `culturax_00002.txt` | CulturaX | 11,839 |  |

#### `cp858-ms/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 250 |  |

#### `cp858-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 969 |  |
| `culturax_00001.txt` | CulturaX | 1,000 |  |
| `culturax_00002.txt` | CulturaX | 7,928 |  |

#### `cp858-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 500 |  |
| `culturax_00001.txt` | CulturaX | 2,328 |  |
| `culturax_00002.txt` | CulturaX | 10,617 |  |

#### `cp858-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 461 |  |
| `culturax_00001.txt` | CulturaX | 1,154 |  |
| `culturax_00002.txt` | CulturaX | 3,475 |  |

#### `cp858-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 271 |  |
| `culturax_00001.txt` | CulturaX | 1,071 |  |
| `culturax_00002.txt` | CulturaX | 3,754 |  |

#### `cp860-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 952 |  |
| `culturax_mC4_101817.txt` | CulturaX | 2,875 |  |
| `culturax_mC4_101818.txt` | CulturaX | 2,837 |  |

#### `cp861-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 2,881 |  |
| `culturax_mC4_77488.txt` | CulturaX | 1,506 |  |
| `culturax_mC4_77489.txt` | CulturaX | 2,808 |  |

#### `cp862-he/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 119 |  |
| `culturax_00001.txt` | CulturaX | 984 |  |
| `culturax_00002.txt` | CulturaX | 9,148 |  |

#### `cp863-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 530 |  |
| `culturax_00001.txt` | CulturaX | 2,710 |  |
| `culturax_00002.txt` | CulturaX | 9,484 |  |

#### `cp864-ar/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 385 |  |

#### `cp865-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 349 |  |
| `culturax_00001.txt` | CulturaX | 1,445 |  |
| `culturax_00002.txt` | CulturaX | 19,560 |  |

#### `cp865-no/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66765.txt` | CulturaX | 669 |  |

#### `cp866-be/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_ibm866_be.txt` | ENCA | 25 | Very small (25 bytes) |
| `culturax_mC4_77016.txt` | CulturaX | 1,188 |  |
| `culturax_mC4_77018.txt` | CulturaX | 1,326 |  |
| `culturax_mC4_77019.txt` | CulturaX | 1,915 |  |

#### `cp866-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 1,309 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 1,256 |  |
| `culturax_mC4_84187.txt` | CulturaX | 2,370 |  |

#### `cp866-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 427 |  |
| `culturax_00001.txt` | CulturaX | 2,166 |  |
| `culturax_00002.txt` | CulturaX | 7,511 |  |

#### `cp866-ru/` — 22 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_ibm866.txt` | uchardet | 680 |  |
| `_ude_1.txt` | Ude | 1,211 |  |
| `aif.ru.health.xml` | chardet | 7,815 |  |
| `aug32.hole.ru.xml` | chardet | 621 |  |
| `aviaport.ru.xml` | chardet | 26,912 |  |
| `blog.mlmaster.com.xml` | chardet | 6,449 | High markup ratio (61% tags) |
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 3,000 |  |
| `culturax_OSCAR-2301_85060.txt` | CulturaX | 2,406 |  |
| `culturax_mC4_85059.txt` | CulturaX | 929 |  |
| `forum.template-toolkit.ru.1.xml` | chardet | 24,863 |  |
| `forum.template-toolkit.ru.4.xml` | chardet | 10,730 |  |
| `forum.template-toolkit.ru.6.xml` | chardet | 32,200 |  |
| `forum.template-toolkit.ru.8.xml` | chardet | 17,744 |  |
| `forum.template-toolkit.ru.9.xml` | chardet | 2,968 |  |
| `greek.ru.xml` | chardet | 4,267 |  |
| `intertat.ru.xml` | chardet | 575 |  |
| `janulalife.blogspot.com.xml` | chardet | 18,809 |  |
| `kapranoff.ru.xml` | chardet | 7,463 | High markup ratio (67% tags) |
| `money.rin.ru.xml` | chardet | 7,550 |  |
| `music.peeps.ru.xml` | chardet | 7,866 |  |
| `newsru.com.xml` | chardet | 24,099 |  |
| `susu.ac.ru.xml` | chardet | 2,059 |  |

#### `cp866-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 742 |  |
| `culturax_00001.txt` | CulturaX | 2,397 |  |
| `culturax_00002.txt` | CulturaX | 11,627 |  |

#### `cp866-uk/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_95020.txt` | CulturaX | 1,371 |  |
| `culturax_mC4_95021.txt` | CulturaX | 2,821 |  |

#### `cp869-el/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 2,307 |  |
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 2,745 |  |
| `culturax_mC4_103810.txt` | CulturaX | 2,131 |  |
| `culturax_mC4_103811.txt` | CulturaX | 1,109 |  |

#### `cp874-th/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `pharmacy.kku.ac.th.centerlab.xml` | chardet | 9,540 |  |
| `pharmacy.kku.ac.th.healthinfo-ne.xml` | chardet | 19,707 |  |

#### `cp875-el/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 2,745 |  |
| `culturax_mC4_103810.txt` | CulturaX | 2,131 |  |
| `culturax_mC4_103811.txt` | CulturaX | 1,109 |  |

#### `cp932-ja/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `5554s2a-cp932.txt` | unknown | 486 |  |
| `culturax_OSCAR-2019_7.txt` | CulturaX | 1,604 |  |
| `hardsoft.at.webry.info.xml` | chardet | 45,871 | High markup ratio (60% tags) |
| `www2.chuo-u.ac.jp-suishin.xml` | chardet | 4,420 |  |
| `y-moto.com.xml` | chardet | 37,856 |  |

#### `cp949-ko/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `ricanet.com.xml` | chardet | 35,289 |  |

### Mac encodings (109 files in 31 directories)

#### `maccyrillic-be/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_maccyr_be.txt` | ENCA | 24 | Very small (24 bytes) |
| `culturax_mC4_77015.txt` | CulturaX | 2,933 |  |
| `culturax_mC4_77016.txt` | CulturaX | 1,188 |  |
| `culturax_mC4_77018.txt` | CulturaX | 1,326 |  |

#### `maccyrillic-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 1,309 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 1,256 |  |
| `culturax_mC4_84187.txt` | CulturaX | 2,370 |  |

#### `maccyrillic-mk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_102724.txt` | CulturaX | 2,363 |  |
| `culturax_mC4_102726.txt` | CulturaX | 1,205 |  |
| `culturax_mC4_102727.txt` | CulturaX | 3,000 |  |

#### `maccyrillic-ru/` — 21 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_mac_cyrillic.txt` | uchardet | 491 |  |
| `_ude_1.txt` | Ude | 1,211 |  |
| `aif.ru.health.xml` | chardet | 7,825 |  |
| `aug32.hole.ru.xml` | chardet | 631 |  |
| `aviaport.ru.xml` | chardet | 60,037 |  |
| `blog.mlmaster.com.xml` | chardet | 6,459 | High markup ratio (61% tags) |
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 3,000 |  |
| `culturax_OSCAR-2019_85057.txt` | CulturaX | 2,831 |  |
| `culturax_mC4_85056.txt` | CulturaX | 1,326 |  |
| `forum.template-toolkit.ru.4.xml` | chardet | 10,740 |  |
| `forum.template-toolkit.ru.6.xml` | chardet | 32,210 |  |
| `forum.template-toolkit.ru.8.xml` | chardet | 17,754 |  |
| `forum.template-toolkit.ru.9.xml` | chardet | 2,978 |  |
| `greek.ru.xml` | chardet | 2,063 |  |
| `intertat.ru.xml` | chardet | 585 |  |
| `kapranoff.ru.xml` | chardet | 7,473 | High markup ratio (67% tags) |
| `koi.kinder.ru.xml` | chardet | 3,656 |  |
| `money.rin.ru.xml` | chardet | 7,560 |  |
| `music.peeps.ru.xml` | chardet | 7,876 |  |
| `newsru.com.xml` | chardet | 24,109 |  |
| `susu.ac.ru.xml` | chardet | 1,216 |  |

#### `maccyrillic-sr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66920.txt` | CulturaX | 1,123 |  |
| `culturax_mC4_66921.txt` | CulturaX | 2,630 |  |
| `culturax_mC4_66923.txt` | CulturaX | 2,867 |  |

#### `maccyrillic-uk/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_maccyr_uk.txt` | ENCA | 95 | Very small (95 bytes) |
| `culturax_mC4_95019.txt` | CulturaX | 1,080 |  |
| `culturax_mC4_95020.txt` | CulturaX | 1,371 |  |
| `culturax_mC4_95021.txt` | CulturaX | 2,821 |  |

#### `macgreek-el/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_103812.txt` | CulturaX | 2,745 |  |
| `culturax_mC4_103810.txt` | CulturaX | 2,131 |  |
| `culturax_mC4_103811.txt` | CulturaX | 1,109 |  |

#### `maciceland-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_77487.txt` | CulturaX | 2,881 |  |
| `culturax_mC4_77488.txt` | CulturaX | 1,506 |  |
| `culturax_mC4_77489.txt` | CulturaX | 2,808 |  |

#### `maclatin2-cs/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_macce_cs.txt` | ENCA | 58 | Very small (58 bytes) |
| `culturax_OSCAR-2019_98821.txt` | CulturaX | 1,362 |  |
| `culturax_mC4_98820.txt` | CulturaX | 1,424 |  |
| `culturax_mC4_98823.txt` | CulturaX | 1,627 |  |

#### `maclatin2-hr/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_macce_hr.txt` | ENCA | 127 |  |
| `culturax_00000.txt` | CulturaX | 104 |  |
| `culturax_00001.txt` | CulturaX | 141 |  |
| `culturax_00002.txt` | CulturaX | 572 |  |

#### `maclatin2-hu/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_82419.txt` | CulturaX | 1,572 |  |
| `culturax_OSCAR-2301_82420.txt` | CulturaX | 1,582 |  |
| `culturax_mC4_82418.txt` | CulturaX | 627 |  |

#### `maclatin2-pl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_97062.txt` | CulturaX | 1,815 |  |
| `culturax_mC4_97060.txt` | CulturaX | 1,300 |  |
| `culturax_mC4_97061.txt` | CulturaX | 2,383 |  |

#### `maclatin2-sk/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_macce_sk.txt` | ENCA | 135 |  |
| `culturax_mC4_95224.txt` | CulturaX | 1,445 |  |
| `culturax_mC4_95227.txt` | CulturaX | 2,868 |  |
| `culturax_mC4_95230.txt` | CulturaX | 2,928 |  |

#### `maclatin2-sl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66688.txt` | CulturaX | 2,892 |  |
| `culturax_mC4_66689.txt` | CulturaX | 2,655 |  |
| `culturax_mC4_66690.txt` | CulturaX | 1,188 |  |

#### `macroman-br/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_43764.txt` | CulturaX | 628 |  |

#### `macroman-cy/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_78727.txt` | CulturaX | 2,884 |  |
| `culturax_mC4_78729.txt` | CulturaX | 1,908 |  |

#### `macroman-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_83466.txt` | CulturaX | 1,918 |  |
| `culturax_mC4_83468.txt` | CulturaX | 2,019 |  |
| `culturax_mC4_83469.txt` | CulturaX | 2,827 |  |

#### `macroman-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_83754.txt` | CulturaX | 2,576 |  |
| `culturax_mC4_83755.txt` | CulturaX | 2,157 |  |
| `culturax_mC4_83756.txt` | CulturaX | 2,259 |  |

#### `macroman-en/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_84512.txt` | CulturaX | 849 |  |

#### `macroman-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_87069.txt` | CulturaX | 1,463 |  |
| `culturax_mC4_87070.txt` | CulturaX | 2,970 |  |
| `culturax_mC4_87071.txt` | CulturaX | 2,778 |  |

#### `macroman-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_80361.txt` | CulturaX | 866 |  |
| `culturax_mC4_80362.txt` | CulturaX | 3,000 |  |
| `culturax_mC4_80363.txt` | CulturaX | 2,804 |  |

#### `macroman-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2201_88371.txt` | CulturaX | 2,871 |  |
| `culturax_OSCAR-2301_88370.txt` | CulturaX | 733 |  |
| `culturax_mC4_88373.txt` | CulturaX | 1,629 |  |

#### `macroman-ga/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_63468.txt` | CulturaX | 2,922 |  |
| `culturax_mC4_63469.txt` | CulturaX | 2,897 |  |
| `culturax_mC4_63470.txt` | CulturaX | 1,222 |  |

#### `macroman-id/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_114889.txt` | CulturaX | 2,572 |  |

#### `macroman-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 536 |  |
| `culturax_00001.txt` | CulturaX | 2,088 |  |
| `culturax_00002.txt` | CulturaX | 21,381 |  |

#### `macroman-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_92388.txt` | CulturaX | 2,378 |  |
| `culturax_mC4_92390.txt` | CulturaX | 1,278 |  |
| `culturax_mC4_92391.txt` | CulturaX | 1,445 |  |

#### `macroman-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2301_107677.txt` | CulturaX | 1,362 |  |
| `culturax_mC4_107675.txt` | CulturaX | 2,455 |  |
| `culturax_mC4_107676.txt` | CulturaX | 1,043 |  |

#### `macroman-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_66762.txt` | CulturaX | 2,502 |  |
| `culturax_mC4_66763.txt` | CulturaX | 3,000 |  |
| `culturax_mC4_66764.txt` | CulturaX | 3,000 |  |

#### `macroman-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2109_101819.txt` | CulturaX | 952 |  |
| `culturax_mC4_101817.txt` | CulturaX | 2,875 |  |
| `culturax_mC4_101818.txt` | CulturaX | 2,837 |  |

#### `macroman-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_96485.txt` | CulturaX | 2,733 |  |
| `culturax_mC4_96486.txt` | CulturaX | 2,533 |  |
| `culturax_mC4_96487.txt` | CulturaX | 2,111 |  |

#### `macturkish-tr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_107848.txt` | CulturaX | 1,161 |  |
| `culturax_mC4_107849.txt` | CulturaX | 729 |  |
| `culturax_mC4_107851.txt` | CulturaX | 2,309 |  |

### KOI8 (34 files in 4 directories)

#### `koi8-r-bg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_OSCAR-2019_84188.txt` | CulturaX | 1,309 |  |
| `culturax_OSCAR-2301_84186.txt` | CulturaX | 1,256 |  |
| `culturax_mC4_84187.txt` | CulturaX | 2,370 |  |

#### `koi8-r-ru/` — 25 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_chromium_KOI8-R_with_no_encoding_specified.html` | Chromium | 583 |  |
| `_uchardet_koi8_r.txt` | uchardet | 352 |  |
| `_ude_1.txt` | Ude | 1,211 |  |
| `aif.ru.health.xml` | chardet | 7,966 |  |
| `archive_www_rambler_ru_20050601.txt` | Web Archive | 2,575 |  |
| `aug32.hole.ru.xml` | chardet | 634 |  |
| `aviaport.ru.xml` | chardet | 61,945 |  |
| `blog.mlmaster.com.xml` | chardet | 6,455 | High markup ratio (61% tags) |
| `culturax_OSCAR-2019_85055.txt` | CulturaX | 3,000 |  |
| `culturax_OSCAR-2301_85060.txt` | CulturaX | 2,406 |  |
| `culturax_mC4_85059.txt` | CulturaX | 929 |  |
| `forum.template-toolkit.ru.1.xml` | chardet | 24,894 |  |
| `forum.template-toolkit.ru.4.xml` | chardet | 11,051 |  |
| `forum.template-toolkit.ru.6.xml` | chardet | 32,901 |  |
| `forum.template-toolkit.ru.8.xml` | chardet | 18,265 |  |
| `forum.template-toolkit.ru.9.xml` | chardet | 2,979 |  |
| `greek.ru.xml` | chardet | 4,271 |  |
| `intertat.ru.xml` | chardet | 66,462 |  |
| `janulalife.blogspot.com.xml` | chardet | 18,809 |  |
| `kapranoff.ru.xml` | chardet | 7,701 | High markup ratio (68% tags) |
| `koi.kinder.ru.xml` | chardet | 25,155 |  |
| `money.rin.ru.xml` | chardet | 7,582 |  |
| `music.peeps.ru.xml` | chardet | 7,947 |  |
| `newsru.com.xml` | chardet | 24,264 |  |
| `susu.ac.ru.xml` | chardet | 13,623 |  |

#### `koi8-t-tg/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_74865.txt` | CulturaX | 1,744 |  |
| `culturax_mC4_74866.txt` | CulturaX | 2,819 |  |
| `culturax_mC4_74867.txt` | CulturaX | 2,853 |  |

#### `koi8-u-uk/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_enca_koi8u_uk.txt` | ENCA | 96 | Very small (96 bytes) |
| `culturax_mC4_95020.txt` | CulturaX | 1,371 |  |
| `culturax_mC4_95021.txt` | CulturaX | 2,821 |  |

### HP encodings (42 files in 14 directories)

#### `hp-roman8-da/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 272 |  |
| `culturax_00001.txt` | CulturaX | 2,074 |  |
| `culturax_00002.txt` | CulturaX | 19,559 |  |

#### `hp-roman8-de/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 219 |  |
| `culturax_00001.txt` | CulturaX | 2,076 |  |
| `culturax_00002.txt` | CulturaX | 7,474 |  |

#### `hp-roman8-en/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 276 |  |
| `culturax_00001.txt` | CulturaX | 2,405 |  |
| `culturax_00002.txt` | CulturaX | 11,230 |  |

#### `hp-roman8-es/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 652 |  |
| `culturax_00001.txt` | CulturaX | 3,158 |  |
| `culturax_00002.txt` | CulturaX | 18,231 |  |

#### `hp-roman8-fi/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 180 |  |
| `culturax_00001.txt` | CulturaX | 2,041 |  |
| `culturax_00002.txt` | CulturaX | 9,321 |  |

#### `hp-roman8-fr/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 129 |  |
| `culturax_00001.txt` | CulturaX | 1,979 |  |
| `culturax_00002.txt` | CulturaX | 9,484 |  |

#### `hp-roman8-id/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 233 |  |
| `culturax_00001.txt` | CulturaX | 2,079 |  |
| `culturax_00002.txt` | CulturaX | 13,140 |  |

#### `hp-roman8-is/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 554 |  |
| `culturax_00001.txt` | CulturaX | 2,170 |  |
| `culturax_00002.txt` | CulturaX | 22,441 |  |

#### `hp-roman8-it/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 570 |  |
| `culturax_00001.txt` | CulturaX | 3,135 |  |
| `culturax_00002.txt` | CulturaX | 75,190 |  |

#### `hp-roman8-ms/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 248 |  |
| `culturax_00001.txt` | CulturaX | 507 |  |
| `culturax_00002.txt` | CulturaX | 2,303 |  |

#### `hp-roman8-nl/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 238 |  |
| `culturax_00001.txt` | CulturaX | 1,183 |  |
| `culturax_00002.txt` | CulturaX | 7,922 |  |

#### `hp-roman8-no/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 827 |  |
| `culturax_00001.txt` | CulturaX | 2,857 |  |
| `culturax_00002.txt` | CulturaX | 34,334 |  |

#### `hp-roman8-pt/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 180 |  |
| `culturax_00001.txt` | CulturaX | 1,806 |  |
| `culturax_00002.txt` | CulturaX | 8,646 |  |

#### `hp-roman8-sv/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 209 |  |
| `culturax_00001.txt` | CulturaX | 1,395 |  |
| `culturax_00002.txt` | CulturaX | 4,993 |  |

### Chinese encodings (59 files in 4 directories)

#### `big5-zh/` — 29 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `0804.blogspot.com.xml` | chardet | 23,616 |  |
| `_chromium_Big5_with_no_encoding_specified.html` | Chromium | 770 |  |
| `_enca_big5_zh.txt` | ENCA | 229 |  |
| `_ude_1.txt` | Ude | 743 |  |
| `_ude_2.txt` | Ude | 1,429 |  |
| `blog.worren.net.xml` | chardet | 10,233 |  |
| `carbonxiv.blogspot.com.xml` | chardet | 17,791 |  |
| `catshadow.blogspot.com.xml` | chardet | 18,147 | High markup ratio (65% tags) |
| `coolloud.org.tw.xml` | chardet | 18,354 |  |
| `culturax_OSCAR-2201_10.txt` | CulturaX | 1,619 |  |
| `digitalwall.com.xml` | chardet | 1,343 |  |
| `ebao.us.xml` | chardet | 4,945 |  |
| `fudesign.blogspot.com.xml` | chardet | 28,525 |  |
| `kafkatseng.blogspot.com.xml` | chardet | 13,558 |  |
| `ke207.blogspot.com.xml` | chardet | 14,599 |  |
| `leavesth.blogspot.com.xml` | chardet | 18,471 | High markup ratio (74% tags) |
| `letterlego.blogspot.com.xml` | chardet | 66,777 |  |
| `linyijen.blogspot.com.xml` | chardet | 12,547 | High markup ratio (63% tags) |
| `marilynwu.blogspot.com.xml` | chardet | 6,440 | Very high markup ratio (82% tags) |
| `myblog.pchome.com.tw.xml` | chardet | 1,464 |  |
| `oui-design.com.xml` | chardet | 2,278 |  |
| `sanwenji.blogspot.com.xml` | chardet | 67,286 | High markup ratio (72% tags) |
| `sinica.edu.tw.xml` | chardet | 10,639 | High markup ratio (66% tags) |
| `sylvia1976.blogspot.com.xml` | chardet | 17,028 |  |
| `tlkkuo.blogspot.com.xml` | chardet | 7,192 | High markup ratio (73% tags) |
| `unoriginalblog.com.xml` | chardet | 15,769 |  |
| `upsaid.com.xml` | chardet | 68,305 |  |
| `willythecop.blogspot.com.xml` | chardet | 13,437 | High markup ratio (73% tags) |
| `ytc.blogspot.com.xml` | chardet | 54,145 |  |

#### `gb18030-zh/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_uchardet_gb18030.txt` | uchardet | 88 | Very small (88 bytes) |
| `_ude_1.txt` | Ude | 1,429 | Chinese Wikipedia article about the Chinese language. Requires gb18030 (not decodable as gb2312). |
| `culturax_mC4_3.txt` | CulturaX | 3,848 | Uses gb18030 4-byte sequences |
| `culturax_mC4_7.txt` | CulturaX | 2,051 | Uses gbk-range bytes beyond gb2312 |

#### `gb2312-zh/` — 24 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `14.blog.westca.com.xml` | chardet | 15,445 |  |
| `2.blog.westca.com.xml` | chardet | 21,264 |  |
| `744521a-gbk.txt` | unknown | 71 | Very small (71 bytes) |
| `_chromium_gb18030_with_no_encoding_specified.html.xml` | Chromium | 990 |  |
| `_enca_gbk_zh.txt` | ENCA | 229 |  |
| `_mozilla_bug171813_text.html` | Mozilla | 1,126 | Very high markup ratio (82% tags) |
| `_ude_1.txt` | Ude | 17,096 |  |
| `acnnewswire.net.xml` | chardet | 10,620 | High markup ratio (69% tags) |
| `bbs.blogsome.com.xml` | chardet | 15,536 |  |
| `cappuccinos.3322.org.xml` | chardet | 7,336 |  |
| `chen56.blogcn.com.xml` | chardet | 15,173 | High markup ratio (62% tags) |
| `cindychen.com.xml` | chardet | 16,608 |  |
| `cnblog.org.xml` | chardet | 26,465 |  |
| `coverer.com.xml` | chardet | 12,095 |  |
| `culturax_mC4_5.txt` | CulturaX | 1,227 |  |
| `eighthday.blogspot.com.xml` | chardet | 22,083 |  |
| `godthink.blogsome.com.xml` | chardet | 2,628 |  |
| `jjgod.3322.org.xml` | chardet | 9,356 |  |
| `lily.blogsome.com.xml` | chardet | 38,414 |  |
| `luciferwang.blogcn.com.xml` | chardet | 20,395 | High markup ratio (61% tags) |
| `pda.blogsome.com.xml` | chardet | 4,479 |  |
| `softsea.net.xml` | chardet | 87,552 |  |
| `w3cn.org.xml` | chardet | 5,646 |  |
| `xy15400.blogcn.com.xml` | chardet | 19,804 |  |

#### `hz-gb-2312-zh/` — 2 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_5.txt` | CulturaX | 1,419 |  |
| `culturax_mC4_8.txt` | CulturaX | 5,462 |  |

### Japanese encodings (70 files in 5 directories)

#### `euc-jp-ja/` — 32 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_mozilla_bug426271_text-euc-jp.html` | Mozilla | 737 |  |
| `_mozilla_bug431054_text.html` | Mozilla | 39 | Very small (39 bytes); High markup ratio (74% tags) |
| `_mozilla_bug620106_text.html` | Mozilla | 1,176 |  |
| `_uchardet_euc_jp.txt` | uchardet | 262 |  |
| `_ude_1.txt` | Ude | 1,375 |  |
| `aivy.co.jp.xml` | chardet | 15,308 |  |
| `akaname.main.jp.xml` | chardet | 34,082 |  |
| `arclamp.jp.xml` | chardet | 73,993 |  |
| `aristrist.s57.xrea.com.xml` | chardet | 30,931 |  |
| `artifact-jp.com.xml` | chardet | 8,855 |  |
| `atom.ycf.nanet.co.jp.xml` | chardet | 19,768 |  |
| `azito.under.jp.xml` | chardet | 8,072 | High markup ratio (68% tags) |
| `azoz.org.xml` | chardet | 15,188 |  |
| `blog.kabu-navi.com.atom.xml` | chardet | 20,472 |  |
| `blog.kabu-navi.com.xml` | chardet | 17,527 |  |
| `bphrs.net.xml` | chardet | 7,808 | High markup ratio (74% tags) |
| `ch.kitaguni.tv.xml` | chardet | 22,171 |  |
| `club.h14m.org.xml` | chardet | 8,094 |  |
| `contents-factory.com.xml` | chardet | 11,504 |  |
| `culturax_OSCAR-2301_6.txt` | CulturaX | 1,482 |  |
| `culturax_mC4_5.txt` | CulturaX | 5,727 |  |
| `furusatonoeki.cutegirl.jp.xml` | chardet | 14,851 |  |
| `manana.moo.jp.xml` | chardet | 7,471 | High markup ratio (67% tags) |
| `mimizun.com.xml` | chardet | 12,646 |  |
| `misuzilla.org.xml` | chardet | 20,052 |  |
| `overcube.com.atom.xml` | chardet | 98,950 |  |
| `overcube.com.xml` | chardet | 8,857 |  |
| `pinkupa.com.xml` | chardet | 19,883 |  |
| `rdf.ycf.nanet.co.jp.xml` | chardet | 10,855 |  |
| `siesta.co.jp.aozora.xml` | chardet | 122,707 | Large file (122,707 bytes) |
| `tls.org.xml` | chardet | 15,158 | High markup ratio (62% tags) |
| `yukiboh.moo.jp.xml` | chardet | 11,596 |  |

#### `iso-2022-jp-2004-ja/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 226 |  |
| `culturax_00001.txt` | CulturaX | 1,759 |  |
| `culturax_00002.txt` | CulturaX | 10,084 |  |

#### `iso-2022-jp-ext-ja/` — 1 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00001.txt` | CulturaX | 1,750 |  |

#### `iso-2022-jp-ja/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 1,561 |  |
| `culturax_OSCAR-2301_6.txt` | CulturaX | 1,776 |  |
| `culturax_mC4_5.txt` | CulturaX | 6,387 |  |

#### `shift_jis-ja/` — 31 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `10e.org.xml` | chardet | 49,064 |  |
| `1affliate.com.xml` | chardet | 55,398 |  |
| `A1.csv` | unknown | 315 |  |
| `_chromium_Shift-JIS_with_no_encoding_specified.html` | Chromium | 1,030 |  |
| `_ude_1.txt` | Ude | 24,612 |  |
| `_ude_2.txt` | Ude | 1,375 |  |
| `_ude_4.txt` | Ude | 34,727 |  |
| `accessories-brand.com.xml` | chardet | 13,732 | High markup ratio (66% tags) |
| `amefoot.net.xml` | chardet | 58,977 | High markup ratio (65% tags) |
| `andore.com.inami.xml` | chardet | 11,351 |  |
| `andore.com.money.xml` | chardet | 17,236 |  |
| `andore.com.xml` | chardet | 8,325 | High markup ratio (60% tags) |
| `archive_www_nhk_or_jp_20020601.txt` | Web Archive | 3,690 |  |
| `blog.inkase.net.xml` | chardet | 30,255 | High markup ratio (63% tags) |
| `blog.paseri.ne.jp.xml` | chardet | 24,859 | High markup ratio (67% tags) |
| `bloglelife.com.xml` | chardet | 27,219 | High markup ratio (68% tags) |
| `brag.zaka.to.xml` | chardet | 17,145 | High markup ratio (67% tags) |
| `celeb.lalalu.com.xml` | chardet | 47,828 | High markup ratio (71% tags) |
| `clickablewords.com.xml` | chardet | 18,136 |  |
| `do.beginnersrack.com.xml` | chardet | 30,840 |  |
| `dogsinn.jp.xml` | chardet | 17,767 |  |
| `grebeweb.net.xml` | chardet | 16,953 |  |
| `milliontimes.jp.xml` | chardet | 34,584 |  |
| `moon-light.ne.jp.xml` | chardet | 7,588 |  |
| `nextbeaut.com.xml` | chardet | 21,540 | High markup ratio (73% tags) |
| `ooganemochi.com.xml` | chardet | 2,681 |  |
| `perth-on.net.xml` | chardet | 3,760 |  |
| `sakusaka-silk.net.xml` | chardet | 51,676 | High markup ratio (62% tags) |
| `setsuzei119.jp.xml` | chardet | 48,592 |  |
| `tamuyou.haun.org.xml` | chardet | 30,148 |  |
| `yasuhisa.com.xml` | chardet | 5,431 | High markup ratio (60% tags) |

### Korean encodings (45 files in 3 directories)

#### `euc-kr-ko/` — 33 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `88455a1-euc-kr.txt` | unknown | 863 |  |
| `_chromium_windows-949_with_no_encoding_specified.html` | Chromium | 746 |  |
| `_mozilla_bug9357_text.html` | Mozilla | 291 |  |
| `_ude_euc1.txt` | Ude | 387 |  |
| `_ude_euc2.txt` | Ude | 1,164 |  |
| `acnnewswire.net.xml` | chardet | 10,865 | High markup ratio (70% tags) |
| `alogblog.com.xml` | chardet | 10,715 |  |
| `arts.egloos.com.xml` | chardet | 16,157 | High markup ratio (70% tags) |
| `birder.egloos.com.xml` | chardet | 11,272 |  |
| `blog.bd-lab.com.xml` | chardet | 9,146 |  |
| `blog.empas.com.xml` | chardet | 1,869 | High markup ratio (64% tags) |
| `blog.rss.naver.com.xml` | chardet | 4,188 | High markup ratio (64% tags) |
| `calmguy.egloos.com.xml` | chardet | 28,099 |  |
| `chisato.info.xml` | chardet | 71,320 |  |
| `console.linuxstudy.pe.kr.xml` | chardet | 9,970 |  |
| `critique.or.kr.xml` | chardet | 5,986 | High markup ratio (74% tags) |
| `epitaph.egloos.com.xml` | chardet | 8,486 |  |
| `ittrend.egloos.com.xml` | chardet | 26,659 |  |
| `jely.egloos.com.xml` | chardet | 19,721 | Very high markup ratio (86% tags) |
| `jely.pe.kr.xml` | chardet | 20,847 |  |
| `jowchung.oolim.net.xml` | chardet | 41,204 | High markup ratio (76% tags) |
| `kina.egloos.com.xml` | chardet | 8,538 |  |
| `lennon81.egloos.com.xml` | chardet | 6,122 | High markup ratio (75% tags) |
| `oroll.egloos.com.xml` | chardet | 6,339 | High markup ratio (73% tags) |
| `poliplus.egloos.com.xml` | chardet | 6,519 | High markup ratio (74% tags) |
| `scarletkh2.egloos.com.xml` | chardet | 10,320 |  |
| `siwoo.org.xml` | chardet | 12,646 |  |
| `sparcs.kaist.ac.kr.xml` | chardet | 6,828 |  |
| `tori02.egloos.com.xml` | chardet | 19,792 |  |
| `willis.egloos.com.xml` | chardet | 21,598 | Very high markup ratio (83% tags) |
| `xenix.egloos.com.xml` | chardet | 44,107 |  |
| `yunho.egloos.com.xml` | chardet | 23,344 |  |
| `zangsalang.egloos.com.xml` | chardet | 14,754 |  |

#### `iso-2022-kr-ko/` — 5 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_iso1.txt` | Ude | 501 |  |
| `_ude_iso2.txt` | Ude | 1,460 |  |
| `culturax_mC4_0.txt` | CulturaX | 3,852 |  |
| `culturax_mC4_1.txt` | CulturaX | 6,269 |  |
| `culturax_mC4_2.txt` | CulturaX | 1,311 |  |

#### `johab-ko/` — 7 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_ude_1.txt` | Ude | 1,165 |  |
| `culturax_mC4_0.txt` | CulturaX | 2,992 |  |
| `culturax_mC4_1.txt` | CulturaX | 4,929 |  |
| `culturax_mC4_2.txt` | CulturaX | 1,035 |  |
| `hlpro-readme.txt` | Contributed | 1,541 |  |
| `iyagi-readme.txt` | Contributed | 18,649 |  |
| `mdir-doc.txt` | Contributed | 70,536 |  |

### Thai encodings (8 files in 1 directories)

#### `tis-620-th/` — 8 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `_mozilla_bug488426_text.html` | Mozilla | 70 | Very small (70 bytes) |
| `_uchardet_tis_620.txt` | uchardet | 399 |  |
| `culturax_OSCAR-2109_109138.txt` | CulturaX | 741 |  |
| `culturax_OSCAR-2301_109134.txt` | CulturaX | 2,748 |  |
| `culturax_mC4_109139.txt` | CulturaX | 2,524 |  |
| `opentle.org.xml` | chardet | 14,156 |  |
| `pharmacy.kku.ac.th.analyse1.xml` | chardet | 12,082 |  |
| `trickspot.boxchart.com.xml` | chardet | 13,027 |  |

### Central Asian encodings (8 files in 2 directories)

#### `kz1048-kk/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 1,954 |  |
| `culturax_mC4_73161.txt` | CulturaX | 654 |  |
| `culturax_mC4_73162.txt` | CulturaX | 2,921 |  |
| `useful-sentences.html` | charset-normalizer | 260 |  |

#### `ptcp154-kk/` — 4 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_mC4_73160.txt` | CulturaX | 1,954 |  |
| `culturax_mC4_73161.txt` | CulturaX | 654 |  |
| `culturax_mC4_73162.txt` | CulturaX | 2,921 |  |
| `useful-sentences.html` | charset-normalizer | 264 |  |

### Other encodings (3 files in 1 directories)

#### `shift-jis-ja/` — 3 files

| File | Source | Size | Notes |
|------|--------|-----:|-------|
| `culturax_00000.txt` | CulturaX | 200 |  |
| `culturax_00001.txt` | CulturaX | 1,482 |  |
| `culturax_00002.txt` | CulturaX | 9,184 |  |
