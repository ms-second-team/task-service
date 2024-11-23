package ru.mssecondteam.taskservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long id;

    private String title;

    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "event_id")
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epic_id")
    @ToString.Exclude
    private Epic epic;
}
