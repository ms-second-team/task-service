package ru.mssecondteam.taskservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.mssecondteam.taskservice.dto.NewTaskRequest;
import ru.mssecondteam.taskservice.dto.TaskDto;
import ru.mssecondteam.taskservice.dto.TaskUpdateRequest;
import ru.mssecondteam.taskservice.model.Task;

import java.util.List;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "epicId", expression = "java(task.getEpic() == null ? null : task.getEpic().getId())")
    TaskDto toDto(Task task);

    Task toModel(NewTaskRequest newTask);

    @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)
    void updateTask(TaskUpdateRequest updateRequest, @MappingTarget Task taskToUpdate);

    List<TaskDto> toDtoList(List<Task> tasks);
}
