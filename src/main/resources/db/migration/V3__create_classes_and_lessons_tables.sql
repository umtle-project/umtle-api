CREATE TABLE classes (
    id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE class_students (
    id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_class_students_class_student UNIQUE (class_id, student_id)
);

CREATE TABLE class_teachers (
    id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_class_teachers_class_teacher UNIQUE (class_id, teacher_id)
);

CREATE TABLE lessons (
    id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
