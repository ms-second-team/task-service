package ru.mssecondteam.taskservice.service;

import ru.mssecondteam.taskservice.dto.epic.EpicUpdateRequest;
import ru.mssecondteam.taskservice.model.Epic;

public interface EpicService {
    Epic createEpic(Long userId, Epic epic);

    Epic updateEpic(Long userId, Long epicId, EpicUpdateRequest updateRequest);

    Epic addTaskToEpic(Long userId, Long epicId, Long taskId);

    Epic deleteTaskFromEpic(Long userId, Long epicId, Long taskId);

    Epic findEpicById(Long epicId);
}
