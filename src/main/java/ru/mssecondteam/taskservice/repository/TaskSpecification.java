package ru.mssecondteam.taskservice.repository;

import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.mssecondteam.taskservice.model.Task;

@UtilityClass
public class TaskSpecification {

    public static Specification<Task> eventIdEquals(Long eventId) {
        if (eventId == null) {
            return null;
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("eventId"), eventId);
    }

    public static Specification<Task> assigneeIdEquals(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("assigneeId"), assigneeId);
    }

    public static Specification<Task> authorIdEquals(Long authorId) {
        if (authorId == null) {
            return null;
        }

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("authorId"), authorId);
    }
}
