package ru.mssecondteam.taskservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicFullDto;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicShortDto;
import ru.mssecondteam.taskservice.dto.epic.dto.EpicUpdateRequest;
import ru.mssecondteam.taskservice.dto.epic.dto.NewEpicRequest;
import ru.mssecondteam.taskservice.model.Epic;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

@Mapper(componentModel = "spring", uses = TaskMapper.class)
public interface EpicMapper {
    EpicShortDto toEpicShortDto(Epic epic);

    EpicFullDto toEpicFullDto(Epic epic);

    Epic toEpicModel(NewEpicRequest newEpic);

    @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)
    void updateEpic(EpicUpdateRequest updateRequest, @MappingTarget Epic epicToUpdate);
}
