package com.loopone.loopinbe.domain.team.teamLoop.entity;

import com.loopone.loopinbe.domain.team.team.entity.Team;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TeamLoop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 100)
    private String title;

    private String content;

    @Column(nullable = false)
    private LocalDate localDate;

    @OneToMany(mappedBy = "teamLoop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamLoopMemberProgress> memberProgress = new ArrayList<>();

    @OneToMany(mappedBy = "teamLoop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamLoopChecklist> teamLoopmemberChecks = new ArrayList<>();
}
