CREATE TABLE homeworks (
    id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    lesson_id BIGINT NULL,
    title VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
