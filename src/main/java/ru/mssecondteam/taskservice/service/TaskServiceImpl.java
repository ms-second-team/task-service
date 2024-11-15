package ru.mssecondteam.taskservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.mssecondteam.taskservice.dto.TaskSearchFilter;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;
import ru.mssecondteam.taskservice.exception.NotFoundException;
import ru.mssecondteam.taskservice.mapper.TaskMapper;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.repository.TaskRepository;
import ru.mssecondteam.taskservice.repository.TaskSpecification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    private final TaskMapper taskMapper;

    @Override
    public Task createTask(Long userId, Task task) {
        task.setAuthorId(userId);
        Task createdTask = taskRepository.save(task);
        log.info("Task with '{}' was created", createdTask.getId());
        return createdTask;
    }

    @Override
    public Task updateTask(Long taskId, Long userId, TaskUpdateRequest updateRequest) {
        final Task task = getTaskById(taskId);
        checkIfUserCanModifyTask(taskId, userId, task);
        taskMapper.updateTask(updateRequest, task);
        Task updatedTask = taskRepository.save(task);
        log.info("Task with id '{}' was updated", updatedTask.getId());
        return updatedTask;
    }

    @Override
    public Task findTaskById(Long taskId) {
        final Task task = getTaskById(taskId);
        log.debug("Task with id '{}' was found", taskId);
        return task;
    }

    @Override
    public List<Task> searchTasks(Integer page, Integer size, TaskSearchFilter searchFilter) {
        final Pageable pageable = PageRequest.of(page, size);
        final List<Specification<Task>> specifications = searchFilterToSpecificationList(searchFilter);
        final Specification<Task> resultSpec = specifications.stream().reduce(Specification::and).orElse(null);
        final List<Task> tasks = taskRepository.findAll(resultSpec, pageable).getContent();
        log.debug("Found '{}' tasks", tasks.size());
        return tasks;
    }

    @Override
    public void deleteTaskById(Long taskId, Long userId) {
        final Task task = getTaskById(taskId);
        checkIfUserCanDeleteTask(taskId, userId, task);
        taskRepository.deleteById(taskId);
        log.info("Task with id '{}' was deleted", taskId);
    }

    private Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException(String.format("Task with id '%s' was not found", taskId)));
    }

    private void checkIfUserCanModifyTask(Long taskId, Long userId, Task currentTask) {
        if (!(currentTask.getAssigneeId().equals(userId) || currentTask.getAuthorId().equals(userId))) {
            throw new NotAuthorizedException(String.format("User with id '%s' is not authorized to modify task with id " +
                    "'%s'", userId, taskId));
        }
    }

    private void checkIfUserCanDeleteTask(Long taskId, Long userId, Task task) {
        if (!task.getAuthorId().equals(userId)) {
            throw new NotAuthorizedException(String.format("User with id '%s' is not authorized to delete task with id " +
                    "'%s'", userId, taskId));
        }
    }

    private List<Specification<Task>> searchFilterToSpecificationList(TaskSearchFilter searchFilter) {
        List<Specification<Task>> resultList = new ArrayList<>();
        resultList.add(TaskSpecification.eventIdEquals(searchFilter.eventId()));
        resultList.add(TaskSpecification.assigneeIdEquals(searchFilter.assignId()));
        resultList.add(TaskSpecification.authorIdEquals(searchFilter.authorId()));
        return resultList.stream().filter(Objects::nonNull).toList();
    }
}
