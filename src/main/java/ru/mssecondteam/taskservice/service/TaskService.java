package ru.mssecondteam.taskservice.service;

import ru.mssecondteam.taskservice.dto.TaskSearchFilter;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.model.Task;

import java.util.List;

public interface TaskService {
    Task createTask(Long userId, Task task);

    Task updateTask(Long taskId, Long userId, TaskUpdateRequest updateRequest);

    Task findTaskById(Long taskId);

    List<Task> searchTasks(Integer page, Integer size, TaskSearchFilter searchFilter);

    void deleteTaskById(Long taskId, Long userId);
}
