-- =====================================================================
-- QuizLive Sample Seed Data
-- Default password for all seed users: password123
-- =====================================================================

USE quizlive;

-- 1. Insert Initial Users
INSERT INTO users (id, name, email, password_hash, salt, role, creator_rank) VALUES
(1, 'System Administrator', 'admin@quizlive.com', '958b3891df8f6f164ed2c3384bbefc07e9c7498b7cef40359bc3fc4a6e746895', 'f5974143ae362d2cbcb6489d0c088da1', 'ADMIN', 'VERIFIED'),
(2, 'Prof. Arvind Sharma', 'creator@quizlive.com', '958b3891df8f6f164ed2c3384bbefc07e9c7498b7cef40359bc3fc4a6e746895', 'f5974143ae362d2cbcb6489d0c088da1', 'CREATOR', 'STANDARD'),
(3, 'Alice Johnson', 'alice@quizlive.com', '958b3891df8f6f164ed2c3384bbefc07e9c7498b7cef40359bc3fc4a6e746895', 'f5974143ae362d2cbcb6489d0c088da1', 'PARTICIPANT', 'STANDARD'),
(4, 'Bob Smith', 'bob@quizlive.com', '958b3891df8f6f164ed2c3384bbefc07e9c7498b7cef40359bc3fc4a6e746895', 'f5974143ae362d2cbcb6489d0c088da1', 'PARTICIPANT', 'STANDARD'),
(5, 'Charlie Brown', 'charlie@quizlive.com', '958b3891df8f6f164ed2c3384bbefc07e9c7498b7cef40359bc3fc4a6e746895', 'f5974143ae362d2cbcb6489d0c088da1', 'PARTICIPANT', 'STANDARD')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. Insert Sample Quizzes
INSERT INTO quizzes (id, title, description, creator_id, duration_seconds, status, access_code, is_public) VALUES
(1, 'Core Java Fundamentals', 'Test your knowledge on Java data types, loops, memory management, and exceptions.', 2, 300, 'APPROVED', 'JAVA101', TRUE),
(2, 'Object-Oriented Programming (OOP)', 'Master OOP concepts: Inheritance, Polymorphism, Abstraction, and Encapsulation.', 2, 450, 'APPROVED', 'OOP201', TRUE),
(3, 'Multithreading & Concurrency in Java', 'Advanced challenges on Threads, Synchronization, ConcurrentHashMap, and Executors.', 2, 600, 'PENDING', 'THREAD301', TRUE)
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- 3. Insert Questions for Quiz 1: Core Java Fundamentals
INSERT INTO questions (id, quiz_id, question_text, option_a, option_b, option_c, option_d, correct_option, points) VALUES
(1, 1, 'Which memory area in the JVM is shared among all active threads?', 'Stack', 'Heap', 'Program Counter (PC) Register', 'Native Method Stack', 'B', 1),
(2, 1, 'What is the default value of a boolean variable declared as a class instance field in Java?', 'true', 'false', '0', 'null', 'B', 1),
(3, 1, 'Which exception is thrown when an application attempts to divide an integer by zero?', 'NullPointerException', 'ArithmeticException', 'IllegalArgumentException', 'NumberFormatException', 'B', 1),
(4, 1, 'Which Java interface must be implemented to allow an object to be sorted using Collections.sort() naturally?', 'Comparator', 'Cloneable', 'Comparable', 'Serializable', 'C', 1)
ON DUPLICATE KEY UPDATE question_text=VALUES(question_text);

-- 4. Insert Questions for Quiz 2: OOP
INSERT INTO questions (id, quiz_id, question_text, option_a, option_b, option_c, option_d, correct_option, points) VALUES
(5, 2, 'Which OOP principle is demonstrated by method overriding (runtime binding)?', 'Encapsulation', 'Polymorphism', 'Abstraction', 'Inheritance', 'B', 1),
(6, 2, 'Can an abstract class have constructors in Java?', 'No, abstract classes cannot have constructors', 'Yes, and they are called when a subclass instance is created', 'Only if the class has no abstract methods', 'Only if declared private', 'B', 1),
(7, 2, 'Which keyword is used to prevent a class from being inherited in Java?', 'static', 'const', 'final', 'sealed', 'C', 1),
(8, 2, 'What access modifier makes a member accessible only within its own package and subclasses?', 'private', 'default (package-private)', 'protected', 'public', 'C', 1)
ON DUPLICATE KEY UPDATE question_text=VALUES(question_text);

-- 5. Insert Sample Attempts & Answers for Quiz 1 (To populate initial leaderboard)
-- Alice scored 4/4
INSERT INTO attempts (id, quiz_id, user_id, started_at, submitted_at, score, tab_switches, status) VALUES
(1, 1, 3, DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 16 MINUTE), 4, 0, 'SUBMITTED')
ON DUPLICATE KEY UPDATE score=VALUES(score);

INSERT INTO attempt_answers (attempt_id, question_id, selected_option, is_correct) VALUES
(1, 1, 'B', TRUE),
(1, 2, 'B', TRUE),
(1, 3, 'B', TRUE),
(1, 4, 'C', TRUE)
ON DUPLICATE KEY UPDATE is_correct=VALUES(is_correct);

-- Bob scored 3/4
INSERT INTO attempts (id, quiz_id, user_id, started_at, submitted_at, score, tab_switches, status) VALUES
(2, 1, 4, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 11 MINUTE), 3, 1, 'SUBMITTED')
ON DUPLICATE KEY UPDATE score=VALUES(score);

INSERT INTO attempt_answers (attempt_id, question_id, selected_option, is_correct) VALUES
(2, 1, 'B', TRUE),
(2, 2, 'B', TRUE),
(2, 3, 'A', FALSE),
(2, 4, 'C', TRUE)
ON DUPLICATE KEY UPDATE is_correct=VALUES(is_correct);

-- 6. Insert Sample Messages
INSERT INTO messages (id, from_user_id, to_user_id, quiz_id, content, sent_at) VALUES
(1, 3, 2, 1, 'Hello Prof. Sharma, question 4 on Comparable was really insightful!', NOW()),
(2, 2, 3, 1, 'Glad you enjoyed it Alice! Make sure to review Comparator for custom sorting as well.', NOW())
ON DUPLICATE KEY UPDATE content=VALUES(content);

-- 7. Insert Default System Settings
INSERT INTO system_settings (setting_key, setting_value) VALUES
('platform_name', 'QuizLive'),
('allow_registrations', 'true'),
('max_tab_switches_warning', '3'),
('anti_cheat_grace_seconds', '5')
ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value);
