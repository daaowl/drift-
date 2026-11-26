# Sukoon: product spec

**One-line pitch:** Every dua from *Seeking Serenity*, sorted by what you're going through, readable in seconds, fully offline.

**Core problem:** The dua you need is buried in a 107-page PDF. Pinch-zooming and scrolling to find it, especially while grieving, anxious or in a hurry, is miserable.

**Target user:** Muslims who want the right dua for the moment without hunting for it, including people who rely on transliteration.

**Out of scope:** prayer times, qibla, a full Quran reader, audio, social feeds, accounts, cloud sync, ads, AI content, any dua not in the source book. Religious text is never altered.

## Screens

| Screen | Purpose | States |
|---|---|---|
| Splash | Android 12 splash API, held until content seeds and lock state is known | n/a |
| Onboarding (3 pages, skippable) | Browse by need → read your way → reminders (leads into the notification rationale) | n/a |
| Home | Greeting, search entry, dua of the day, continue reading, 10 section cards | skeleton, error + retry, success |
| Section | Hero header with section tint and ornament; list of duas. Expanded width: list-detail with the dua beside it | skeleton, error, empty, success |
| Reader | Horizontal pager through the list; Arabic / transliteration / translation toggles; favourite, add to collection, copy, share, pin to widget, open counter | loading, error, empty (list emptied), success |
| Counter | Tap-anywhere tasbih with progress ring, rounds, reset; haptics and optional tick sound | loading, error, success |
| Search | Live FTS search with spelling-tolerant matching; suggestion chips | suggestions (no query), no results, error, results |
| Saved | Tabs: Favourites and Collections; create collection | skeleton, error, empty (each tab), success |
| Collection | Rename, delete, swipe to remove with undo | loading, error, empty, success |
| Settings | Appearance, Reading, Counter, Behaviour, Reminders, Notification channels, Widget, Privacy, Your data, Language, About, Reset | n/a |
| PIN setup | Choose then confirm a 4-digit PIN | mismatch error |
| Lock | Biometric prompt with PIN fallback; shake + haptic on a wrong PIN | wrong PIN |
| About | Version, source credit, changelog, licences, privacy policy, rate, contact | n/a |
| Document | Changelog / licences / privacy policy | loading, error, success |
| Widget | Dua of the day or a pinned dua; tapping it opens the reader | empty (library not loaded) |

## Core flows

1. **Find by feeling:** Home → section card (shared-element) → dua row (shared-element) → reader → swipe through the section.
2. **Find by word:** Home search bar (shared-element) → Search → type "hasbunallah", "anxiety" or "2:201" → result → reader.
3. **Recite with a count:** Reader → Count ×7 → tap anywhere; haptic on each tap, strong haptic and optional chime on completion; auto-reset starts a new round.
4. **Keep close:** heart (favourite) or Add to collection → Saved tab. Reader opened from Favourites or a Collection pages through that list.
5. **Daily habit:** onboarding or Settings → rationale sheet → system prompt → reminders scheduled with WorkManager. Permanently denied → dialog that deep-links to system settings.
6. **Privacy:** Settings → Set PIN → App lock on → app locks every time it goes to the background; biometric first, PIN fallback.
7. **Move phones:** Settings → Back up to a file (SAF) → Restore from a file (merges, never deletes).
8. **Deep links:** notification or widget tap → reader for that dua (AUTO list resolves to its section).

## Data model

```
Room (sukoon.db, v1, schema exported to app/schemas)
├── sections        id PK · title · subtitle · sort_order
├── duas            id PK · section_id FK→sections (cascade) · title · arabic · transliteration
│                   translation · reference · recitation_note? · repeat_count? · sort_order · kind (DUA|GUIDANCE)
├── dua_fts (FTS4)  dua_id (not indexed) · body (normalised title+section+transliteration+translation+reference)
├── favorites       dua_id PK FK→duas (cascade) · added_at
├── collections     id PK auto · name · created_at
├── collection_items (collection_id FK cascade, dua_id FK cascade) PK · added_at
└── counters        dua_id PK FK→duas (cascade) · count · completed_rounds · updated_at

DataStore (sukoon_settings)
└── UserSettings (theme, accent, dynamic colour, reading scales/toggles/font, counter, launch,
    last read, reminders, widget, privacy, onboarding) + content_version + pin_hash + pin_salt
```

Content ships as `assets/duas.json` (`contentVersion` 1, 10 sections, 88 entries). `SeedLoader` upserts content in one transaction when the bundled version is newer, so user rows that reference duas survive content updates; duas removed from the content cascade away cleanly.

## Content notes

Text extraction from the source PDF mangles the Arabic, so the Arabic in `duas.json` was typeset from the standard Quranic and hadith wording for each cited reference. Three layout errors in the book were corrected: *Halaal* (the book prints Quran 65:2-3 transliteration over the Tirmidhi 3563 translation), *Wealth* (missing transliteration) and *Well Being* (stray "subheading" text). *Grief: Talbina* is a hadith plus recipe, so it is stored as a `GUIDANCE` entry: shown in the reader, never picked as the dua of the day, and has no counter.
