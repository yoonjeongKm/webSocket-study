package com.example.chatserver.chat.domain;

import com.example.chatserver.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;

    @Builder.Default
    private String isGroupChat="N";

    /**
     * 채팅방에 속한 참가자 목록 (1:N)
     * - ChatParticipant 엔티티의 chatRoom 필드에 의해 매핑됨
     * - 채팅방 삭제 시 관련 참가자도 자동 삭제 (CascadeType.REMOVE)
     * - orphanRemoval을 안하는 이유는 ChatParticipant를 참조하고 있는 곳이 없어서
     * mappedBy = "chatRoom" => private ChatRoom chatRoom;
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE)
    private List<ChatParticipant> chatParticipants = new ArrayList<>();

    /**
     * 채팅방에 속한 메시지 목록 (1:N)
     * - ChatMessage 엔티티의 chatRoom 필드에 의해 매핑됨
     * - orphanRemoval = true → 메시지 목록에서 제거된 엔티티는 DB에서도 삭제됨
     * - orphanRemoval을 넣으면 ReadStatus 도 같이 매핑되어있어서 삭제됨
     * - 채팅방 삭제 시 메시지도 함께 삭제됨
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();
}
