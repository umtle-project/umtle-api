ALTER TABLE users ADD COLUMN name VARCHAR(100) NOT NULL DEFAULT '';
UPDATE users SET name = login_id WHERE name = '';
ALTER TABLE users ADD COLUMN student_id BIGINT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_student_id UNIQUE (student_id);

CREATE TABLE parent_student_links (
    id BIGINT NOT NULL,
    parent_user_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    PRIMARY KEY (id)
);
