package com.facochico.edu.domain.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class TagEntity {
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String name;

    public TagEntity(String name) {
        this.name = name;
    }
}
