CREATE TABLE IF NOT EXISTS tasks (
    task_id BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    deadline TIMESTAMP,
    status VARCHAR(15) NOT NULL,
    assignee_id BIGINT,
    author_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL
);