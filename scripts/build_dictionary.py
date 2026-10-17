#!/usr/bin/env python3
"""
Downloads and compiles the open dictionary dataset into an optimized SQLite database
with pre-computed synsets (thesaurus) and B-tree indexes, compressed for Android assets.
"""
import gzip
import json
import os
import re
import sqlite3
import sys
import time
import urllib.request

DATA_URL = "https://raw.githubusercontent.com/CloudBytes-Academy/English-Dictionary-Open-Source/main/v2/dictionary.jsonl.gz"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
ASSET_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets")
OUT_GZ = os.path.join(ASSET_DIR, "dictionary.db.gz")
RAW_CACHE = "/tmp/dict_source.jsonl.gz"
TEMP_DB = "/tmp/dict_build.db"

def main():
    os.makedirs(ASSET_DIR, exist_ok=True)

    if not os.path.exists(RAW_CACHE):
        print(f"Downloading dataset from {DATA_URL}...")
        urllib.request.urlretrieve(DATA_URL, RAW_CACHE)

    if os.path.exists(TEMP_DB):
        os.remove(TEMP_DB)

    print("Opening SQLite database...")
    conn = sqlite3.connect(TEMP_DB)
    conn.execute("PRAGMA page_size = 4096;")
    conn.execute("PRAGMA journal_mode = OFF;")
    conn.execute("PRAGMA synchronous = OFF;")

    conn.execute("""
    CREATE TABLE entries (
        id INTEGER PRIMARY KEY,
        word TEXT NOT NULL,
        word_lower TEXT NOT NULL,
        pos TEXT,
        definition TEXT NOT NULL,
        examples TEXT,
        synonyms TEXT
    );
    """)

    conn.execute("""
    CREATE TABLE history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        word TEXT NOT NULL UNIQUE,
        timestamp INTEGER NOT NULL
    );
    """)

    conn.execute("""
    CREATE TABLE bookmarks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        word TEXT NOT NULL UNIQUE,
        timestamp INTEGER NOT NULL
    );
    """)

    print("Extracting synset relationships for thesaurus...")
    synsets = {}
    with gzip.open(RAW_CACHE, "rt", encoding="utf-8") as f:
        for line in f:
            row = json.loads(line)
            synset_id = row.get("synset_id")
            if synset_id:
                synsets.setdefault(synset_id, set()).add(row["word"])

    print("Building dictionary entries...")
    rows = []
    with gzip.open(RAW_CACHE, "rt", encoding="utf-8") as f:
        for line in f:
            row = json.loads(line)
            word = row["word"]
            synset_id = row.get("synset_id")
            syns = [w for w in synsets.get(synset_id, set()) if w.lower() != word.lower()] if synset_id else []
            examples = json.dumps(row.get("examples", [])) if row.get("examples") else None
            synonyms = json.dumps(syns) if syns else None
            pos = row.get("pos") or row.get("wordtype")
            rows.append((word, word.lower(), pos, row["definition"], examples, synonyms))

    print(f"Inserting {len(rows)} entries...")
    conn.executemany("INSERT INTO entries (word, word_lower, pos, definition, examples, synonyms) VALUES (?, ?, ?, ?, ?, ?)", rows)

    print("Building B-tree index on word_lower...")
    conn.execute("CREATE INDEX idx_word_lower ON entries(word_lower);")

    print("Downloading & indexing International Phonetic Alphabet (IPA) pronunciations...")
    ipa_url = "https://raw.githubusercontent.com/open-dict-data/ipa-dict/master/data/en_US.txt"
    ipa_cache = "/tmp/en_US.txt"
    if not os.path.exists(ipa_cache):
        urllib.request.urlretrieve(ipa_url, ipa_cache)

    conn.execute("CREATE TABLE pronunciations (word_lower TEXT PRIMARY KEY, ipa TEXT);")
    ipa_rows = []
    with open(ipa_cache, "r", encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split("\t")
            if len(parts) == 2:
                ipa_rows.append((parts[0].lower(), parts[1]))
    conn.executemany("INSERT OR REPLACE INTO pronunciations (word_lower, ipa) VALUES (?, ?);", ipa_rows)
    print(f"Inserted {len(ipa_rows)} IPA pronunciations.")

    print("Downloading & indexing multilingual translations (Hindi, French, Spanish)...")
    conn.execute("CREATE TABLE translations (word_lower TEXT PRIMARY KEY, hi TEXT, es TEXT, fr TEXT);")

    trans_map = {}
    hin_cache = "/tmp/eng-hin.tei"
    if not os.path.exists(hin_cache):
        urllib.request.urlretrieve("https://raw.githubusercontent.com/freedict/fd-dictionaries/master/eng-hin/eng-hin.tei", hin_cache)
    with open(hin_cache, "r", encoding="utf-8") as f:
        for w, q in re.findall(r"<entry>.*?<orth>(.*?)</orth>.*?<quote>(.*?)</quote>.*?</entry>", f.read(), re.DOTALL):
            cw, cq = w.strip().lower(), q.strip().replace("~", " ")
            if cw and cq:
                trans_map.setdefault(cw, {})["hi"] = cq

    fra_cache = "/tmp/eng-fra.tei"
    if not os.path.exists(fra_cache):
        urllib.request.urlretrieve("https://raw.githubusercontent.com/freedict/fd-dictionaries/master/eng-fra/eng-fra.tei", fra_cache)
    with open(fra_cache, "r", encoding="utf-8") as f:
        for w, q in re.findall(r"<entry>.*?<orth>(.*?)</orth>.*?<quote>(.*?)</quote>.*?</entry>", f.read(), re.DOTALL):
            cw, cq = w.strip().lower(), q.strip()
            if cw and cq:
                trans_map.setdefault(cw, {})["fr"] = cq

    spa_cache = "/tmp/spa-eng.tei"
    if not os.path.exists(spa_cache):
        urllib.request.urlretrieve("https://raw.githubusercontent.com/freedict/fd-dictionaries/master/spa-eng/spa-eng.tei", spa_cache)
    with open(spa_cache, "r", encoding="utf-8") as f:
        for spa, eng in re.findall(r"<entry>.*?<orth>(.*?)</orth>.*?<quote>(.*?)</quote>.*?</entry>", f.read(), re.DOTALL):
            for word in eng.split(";"):
                cw = word.strip().lower()
                if cw and len(cw.split()) == 1:
                    trans_map.setdefault(cw, {}).setdefault("es", spa.strip())

    trans_rows = [(w, d.get("hi"), d.get("es"), d.get("fr")) for w, d in trans_map.items()]
    conn.executemany("INSERT OR REPLACE INTO translations (word_lower, hi, es, fr) VALUES (?, ?, ?, ?);", trans_rows)
    print(f"Inserted {len(trans_rows)} translation entries.")

    conn.commit()
    conn.execute("VACUUM;")
    conn.close()

    print(f"Compressing into {OUT_GZ}...")
    with open(TEMP_DB, "rb") as f_in, gzip.open(OUT_GZ, "wb", compresslevel=6) as f_out:
        while chunk := f_in.read(1024 * 1024):
            f_out.write(chunk)

    if os.path.exists(TEMP_DB):
        os.remove(TEMP_DB)

    print(f"Done! Final asset size: {os.path.getsize(OUT_GZ) / (1024 * 1024):.2f} MB")

if __name__ == "__main__":
    main()
