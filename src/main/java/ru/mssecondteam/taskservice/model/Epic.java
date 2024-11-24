package ru.mssecondteam.taskservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @OneToMany(mappedBy = "epic", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Task> epicsTasks;

    public void addTask(Task task) {
        if (epicsTasks == null) {
            epicsTasks = new ArrayList<>();
        }
        epicsTasks.add(task);
        task.setEpic(this);
    }

    public void removeTask(Task task) {
        if (epicsTasks == null) {
            epicsTasks = new ArrayList<>();
        }
        epicsTasks.remove(task);
        task.setEpic(null);
    }
}
