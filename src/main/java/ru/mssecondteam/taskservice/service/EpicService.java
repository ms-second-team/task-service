package ru.mssecondteam.taskservice.service;

import ru.mssecondteam.taskservice.dto.epic.dto.EpicUpdateRequest;
import ru.mssecondteam.taskservice.model.Epic;
import ru.mssecondteam.taskservice.model.Task;

public interface EpicService {
    Epic createEpic(Epic epic);

    Epic updateEpic(Long epicId, EpicUpdateRequest updateRequest);

    Task addTaskToEpic(Long userId, Long epicId, Long taskId);

    Task deleteTaskFromEpic(Long userId, Long epicId, Long taskId);

    Epic findEpicById(Long epicId);
}
