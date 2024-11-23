package ru.mssecondteam.taskservice.service;

import ru.mssecondteam.taskservice.dto.TaskSearchFilter;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicUpdateRequest;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.model.Task;

import java.util.List;

public interface TaskService {

    Task createTask(Long userId, Task task);

    Task updateTask(Long taskId, Long userId, TaskUpdateRequest updateRequest);

    Task findTaskById(Long taskId);

    List<Task> searchTasks(Integer page, Integer size, TaskSearchFilter searchFilter);

    void deleteTaskById(Long taskId, Long userId);

    Epic createEpic(Epic epic);

    Epic updateEpic(Long epicId, EpicUpdateRequest updateRequest);

    Task addTaskToEpic(Long userId, Long epicId, Long taskId);

    Task deleteTaskFromEpic(Long userId, Long epicId, Long taskId);

    Epic findEpicById(Long epicId);
}
