Create Database WN_DB;
Go

Use WN_DB;
Go


CREATE TABLE Admin (
    admin_id        UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    NVARCHAR(100) NULL,
    is_active       BIT NOT NULL DEFAULT 1,
    create_at       DATETIME NOT NULL DEFAULT GETDATE(),
    last_login_at   DATETIME NULL,

    CONSTRAINT UK_Admin_Email UNIQUE (email)
);
--
--TABLE USER=================================================
--
CREATE TABLE [User] (
    user_id         UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NULL,              -- NULL nếu dùng Firebase
    display_name    NVARCHAR(100) NULL,
    is_active       BIT NOT NULL DEFAULT 1,
    create_at       DATETIME NOT NULL DEFAULT GETDATE(),
    last_login_at   DATETIME NULL,

    CONSTRAINT UK_User_Email UNIQUE (email)
);
GO

--
--TABLE DECK=================================================
--

Create table Deck(
    deck_id     UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
    user_id     UNIQUEIDENTIFIER NOT NULL,
    name_deck  NVARCHAR(150) NOT NULL,
    description NVARCHAR(500) NULL,         
    create_at   DATETIME NOT NULL DEFAULT GETDATE(),  

    CONSTRAINT FK_deck_userid 
        FOREIGN KEY (user_id) REFERENCES [User](user_id) ON DELETE CASCADE 
);
GO
--
--TABLE SETTING================================================
--

Create table setting(
	setting_id		UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
	user_id		UNIQUEIDENTIFIER NOT NULL,
	reminder_enable BIT NOT NULL DEFAULT 0,
	reminder_time Time null,
	dark_mode BIT NOT NULL DEFAULT 0,
	tts_enable BIT NOT NULL DEFAULT 1,
	auto_suggest_enabled BIT NOT NULL DEFAULT 1,


	CONSTRAINT UK_setting_userid UNIQUE (user_id),
	CONSTRAINT FK_setting_userid
		FOREIGN KEY (user_id) references [User](user_id) ON DELETE CASCADE

);
GO

Create table WORD(
	word_id		UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
	deck_id UNIQUEIDENTIFIER NOT NULL,
	term VARCHAR(100) NOT NULL,
	meaning NVARCHAR(500) NOT NULL,
	phonetic VARCHAR(100) NULL,
	example VARCHAR(500) NULL,
	status VARCHAR(100) NULL,
	correct_streak INT NOT NULL DEFAULT 0,
	wrong_count INT NOT NULL DEFAULT 0,
	interval_days INT NOT NULL DEFAULT 0,
	next_review_at DATE null,
	last_review_at DATETIME NULL,
	create_at DATETIME NOT NULL DEFAULT GETDATE(),

	CONSTRAINT FK_word_deckid
		FOREIGN KEY (deck_id) REFERENCES [Deck](deck_id) ON DELETE CASCADE
);
GO


Create TABLE study_session(
	session_id		UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
	deck_id UNIQUEIDENTIFIER NOT NULL,
	started_at DATETIME NOT NULL DEFAULT GETDATE(),
	ended_at DATETIME NULL,
	known_count INT NOT NULL DEFAULT 0,
	unknown_count INT NOT NULL DEFAULT 0,

	CONSTRAINT FK_STUDIEDSEESION_deckid
		FOREIGN KEY (deck_id) REFERENCES [deck](deck_id) ON DELETE CASCADE
);
GO


CREATE TABLE StudyItem (
    item_id         UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
    session_id      UNIQUEIDENTIFIER NOT NULL,
    word_id         UNIQUEIDENTIFIER NOT NULL,
    result          VARCHAR(20) NOT NULL,           
    order_no        INT NOT NULL,

    CONSTRAINT FK_StudyItem_Session 
        FOREIGN KEY (session_id) REFERENCES [study_session](session_id) ON DELETE CASCADE,

    CONSTRAINT FK_StudyItem_Word 
        FOREIGN KEY (word_id) REFERENCES Word(word_id) ON DELETE CASCADE,


    CONSTRAINT CK_StudyItem_Result 
        CHECK (result IN ('known', 'unknown'))
);
GO

CREATE TABLE Quiz (
    quiz_id         UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
    deck_id         UNIQUEIDENTIFIER NOT NULL,
    quiz_type       VARCHAR(20) NOT NULL DEFAULT 'normal',  
    total_questions INT NOT NULL DEFAULT 0,
    correct_count   INT NOT NULL DEFAULT 0,
    score           FLOAT NOT NULL DEFAULT 0,
    taken_at        DATETIME NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_Quiz_Deck 
        FOREIGN KEY (deck_id) REFERENCES Deck(deck_id) ON DELETE CASCADE,

    CONSTRAINT CK_Quiz_Type 
        CHECK (quiz_type IN ('normal', 'timed', 'review', 'test'))
);
GO

CREATE TABLE QuizItem (
    quiz_item_id    UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
    quiz_id         UNIQUEIDENTIFIER NOT NULL,
    word_id         UNIQUEIDENTIFIER NOT NULL,
    question_type   VARCHAR(20) NOT NULL DEFAULT 'multiple_choice',
    user_answer     VARCHAR(100) NULL,
    is_correct      BIT NOT NULL DEFAULT 0,

    CONSTRAINT FK_QuizItem_Quiz FOREIGN KEY (quiz_id) REFERENCES Quiz(quiz_id) ON DELETE CASCADE,
    CONSTRAINT FK_QuizItem_Word FOREIGN KEY (word_id) REFERENCES Word(word_id) ON DELETE CASCADE,
);

CREATE TABLE QuizOption (
    option_id       UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
    quiz_item_id    UNIQUEIDENTIFIER NOT NULL,
    option_text     NVARCHAR(200) NOT NULL,     -- Nội dung lựa chọn
    is_correct      BIT NOT NULL DEFAULT 0,     -- Đây có phải đáp án đúng không
    order_no        INT NOT NULL,               -- Thứ tự hiển thị (A, B, C, D)

    CONSTRAINT FK_QuizOption_QuizItem 
        FOREIGN KEY (quiz_item_id) REFERENCES QuizItem(quiz_item_id) ON DELETE CASCADE
);
