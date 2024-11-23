package ru.mssecondteam.taskservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mssecondteam.taskservice.model.Epic;

public interface EpicRepository extends JpaRepository<Epic, Long> {
}