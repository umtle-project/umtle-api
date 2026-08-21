CREATE TABLE learning_records (
    id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
