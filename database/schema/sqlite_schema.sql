-- ============================================================
-- WordNest SQLite Schema (Version 2)
-- ============================================================

CREATE TABLE IF NOT EXISTS Admin (
    admin_id TEXT PRIMARY KEY NOT NULL,
    email TEXT NOT NULL,
    password TEXT NOT NULL,
    create_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS User (
    user_id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    password TEXT NOT NULL,
    streak_day INTEGER NOT NULL DEFAULT 0,
    last_study_date TEXT,
    create_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS Setting (
    user_id TEXT PRIMARY KEY NOT NULL,
    daily_goal INTEGER NOT NULL DEFAULT 20,
    notification_enabled INTEGER NOT NULL DEFAULT 1,
    reminder_time TEXT,
    sound_enabled INTEGER NOT NULL DEFAULT 1,
    dark_mode INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Deck (
    deck_id TEXT PRIMARY KEY NOT NULL,
    user_id TEXT NOT NULL,
    name_deck TEXT NOT NULL,
    description TEXT,
    create_at TEXT NOT NULL,
    icon TEXT NOT NULL DEFAULT 'plane',
    color TEXT NOT NULL DEFAULT 'mint',
    FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS index_Deck_user_id ON Deck(user_id);

CREATE TABLE IF NOT EXISTS Word (
    word_id TEXT PRIMARY KEY NOT NULL,
    deck_id TEXT NOT NULL,
    term TEXT NOT NULL,
    meaning TEXT NOT NULL,
    phonetic TEXT,
    example TEXT,
    note TEXT,
    status TEXT NOT NULL DEFAULT 'egg',
    correct_streak INTEGER NOT NULL DEFAULT 0,
    wrong_count INTEGER NOT NULL DEFAULT 0,
    interval_days INTEGER NOT NULL DEFAULT 0,
    next_review_at TEXT,
    last_review_at TEXT,
    create_at TEXT NOT NULL,
    FOREIGN KEY (deck_id) REFERENCES Deck(deck_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS index_Word_deck_id ON Word(deck_id);
CREATE INDEX IF NOT EXISTS index_Word_term ON Word(term);
