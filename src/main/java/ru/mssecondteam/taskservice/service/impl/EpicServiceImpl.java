package ru.mssecondteam.taskservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicUpdateRequest;
import ru.mssecondteam.taskservice.exception.NotAuthorizedException;
import ru.mssecondteam.taskservice.exception.NotFoundException;
import ru.mssecondteam.taskservice.exception.OperationNotAllowedException;
import ru.mssecondteam.taskservice.mapper.EpicMapper;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.model.Task;
import ru.mssecondteam.taskservice.repository.epic.EpicRepository;
import ru.mssecondteam.taskservice.repository.task.TaskRepository;
import ru.mssecondteam.taskservice.service.EpicService;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpicServiceImpl implements EpicService {

    private final EpicRepository epicRepository;

    private final EpicMapper epicMapper;

    private final TaskRepository taskRepository;

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
    public Epic addTaskToEpic(Long userId, Long epicId, Long taskId) {
        final Epic epic = getEpicById(epicId);
        checkIfUserCanModifyEpicsTasks(userId, epic);
        final Task task = getTaskById(taskId);
        checkIfTaskAndEpicBelongsToTheSameEvent(epic, task);
        checkIfTaskIsAvailableForAdding(task, epic);
        epic.addTask(task);
        Epic epicWithAddedTask = epicRepository.save(epic);
        log.info("Task with id '{}' was added to Epic with id '{}'", epicWithAddedTask.getId(), epicId);
        return epicWithAddedTask;
    }

    @Override
    public Epic deleteTaskFromEpic(Long userId, Long epicId, Long taskId) {
        final Epic epic = getEpicById(epicId);
        checkIfUserCanModifyEpicsTasks(userId, epic);
        final Task task = getTaskById(taskId);
        checkIfTaskBelongsToEpic(task, epic);
        epic.removeTask(task);
        Epic EpicWithDeletedTask = epicRepository.save(epic);
        log.info("Task with id '{}' was deleted from Epic with id '{}'", task.getId(), epicId);
        return EpicWithDeletedTask;
    }

    @Override
    public Epic findEpicById(Long epicId) {
        final Epic epic = getEpicById(epicId);
        log.debug("Epic with id '{}' was found", epicId);
        return epic;
    }


    private Epic getEpicById(Long epicId) {
        return epicRepository.findById(epicId)
                .orElseThrow(() -> new NotFoundException(String.format("Epic with id '%s' was not found", epicId)));
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

    private void checkIfUserCanModifyEpicsTasks(Long userId, Epic epic) {
        if (!userId.equals(epic.getExecutiveId())) {
            throw new NotAuthorizedException(String.format("User with id '%s' is not authorized to add tasks to " +
                    "epic with id '%s'", userId, epic.getId()));
        }
    }

    private Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException(String.format("Task with id '%s' was not found", taskId)));
    }

    private void checkIfTaskIsAvailableForAdding(Task task, Epic epic) {
        if (task.getEpic() != null) {
            throw new OperationNotAllowedException(String.format("Task with id '%s' already belongs to epic with id '%s'",
                    task.getId(), epic.getId()));
        }
    }
}
