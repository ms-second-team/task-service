package ru.mssecondteam.taskservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.mssecondteam.taskservice.dto.TaskSearchFilter;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicUpdateRequest;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;
import ru.mssecondteam.taskservice.exception.NotFoundException;
import ru.mssecondteam.taskservice.exception.OperationNotAllowedException;
import ru.mssecondteam.taskservice.mapper.EpicMapper;
import ru.mssecondteam.taskservice.mapper.TaskMapper;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.repository.EpicRepository;
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

    private final EpicRepository epicRepository;

    private final TaskMapper taskMapper;

    private final EpicMapper epicMapper;

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

    @Override
    public Epic createEpic(Epic epic) {
        Epic createdEpic = epicRepository.save(epic);
        log.info("Epic with id '{}' was created", createdEpic.getId());
        return createdEpic;
    }

    @Override
    public Epic updateEpic(Long epicId, EpicUpdateRequest updateRequest) {
        final Epic epic = getEpicById(epicId);
        epicMapper.updateEpic(updateRequest, epic);
        Epic updatedEpic = epicRepository.save(epic);
        log.info("Epic with id '{}' was updated", updatedEpic.getId());
        return updatedEpic;
    }

    @Override
    public Task addTaskToEpic(Long userId, Long epicId, Long taskId) {
        final Epic epic = getEpicById(epicId);
        checkIfUserCanAddModifyEpicsTasks(userId, epic);
        final Task task = getTaskById(taskId);
        checkIfTaskAndEpicBelongsToTheSameEvent(epic, task);
        task.setEpic(epic);
        Task addedToEpicTask = taskRepository.save(task);
        log.info("Task with id '{}' was added to Epic with id '{}'", addedToEpicTask.getId(), epicId);
        return addedToEpicTask;
    }

    @Override
    public Task deleteTaskFromEpic(Long userId, Long epicId, Long taskId) {
        final Epic epic = getEpicById(epicId);
        checkIfUserCanAddModifyEpicsTasks(userId, epic);
        final Task task = getTaskById(taskId);
        checkIfTaskBelongsToEpic(task, epic);
        task.setEpic(null);
        Task deletedFromEpicTask = taskRepository.save(task);
        log.info("Task with id '{}' was deleted from Epic with id '{}'", deletedFromEpicTask.getId(), epicId);
        return deletedFromEpicTask;
    }

    @Override
    public Epic findEpicById(Long epicId) {
        final Epic epic = getEpicById(epicId);
        List<Task> epicsTasks = taskRepository.findAllByEpicId(epicId);
        epic.setEpicsTasks(epicsTasks);
        log.debug("Epic with id '{}' was found", epicId);
        return epic;
    }

    private Epic getEpicById(Long epicId) {
        return epicRepository.findById(epicId)
                .orElseThrow(() -> new NotFoundException(String.format("Epic with id '%s' was not found", epicId)));
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

    private void checkIfUserCanAddModifyEpicsTasks(Long userId, Epic epic) {
        if (!userId.equals(epic.getExecutiveId())) {
            throw new NotAuthorizedException(String.format("User with id '%s' is not authorized to add tasks to " +
                    "epic with id '%s'", userId, epic.getId()));
        }
    }

    private void checkIfTaskAndEpicBelongsToTheSameEvent(Epic epic, Task task) {
        if (!epic.getEventId().equals(task.getEventId())) {
            throw new OperationNotAllowedException(String.format("Task with id '%s' can not be added to epic " +
                    "with id '%s' as they belong to different events", task.getId(), epic.getId()));
        }
    }

    private void checkIfTaskBelongsToEpic(Task task, Epic epic) {
        if (!task.getEpic().getId().equals(epic.getId())) {
            throw new OperationNotAllowedException(String.format("Task with id '%s' does not belong to epic " +
                    "with id '%s'", task.getId(), epic.getId()));
        }
    }

    private List<Specification<Task>> searchFilterToSpecificationList(TaskSearchFilter searchFilter) {
        List<Specification<Task>> resultList = new ArrayList<>();
        resultList.add(TaskSpecification.eventIdEquals(searchFilter.eventId()));
        resultList.add(TaskSpecification.assigneeIdEquals(searchFilter.assigneeId()));
        resultList.add(TaskSpecification.authorIdEquals(searchFilter.authorId()));
        return resultList.stream().filter(Objects::nonNull).toList();
    }
}
