package ru.mssecondteam.taskservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "epics")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Epic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "executive_id")
    private Long executiveId;

    @Column(name = "event_id")
    private Long eventId;

    private LocalDateTime deadline;

    @OneToMany(mappedBy = "epic")
    @ToString.Exclude
    private List<Task> epicsTasks;
}
