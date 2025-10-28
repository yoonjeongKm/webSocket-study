package com.example.chatserver.common.domain;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@MappedSuperclass
public class BaseTimeEntity {
    @CreationTimestamp
    private Long createdTime;
    @UpdateTimestamp
    private Long updatedTime;
}
