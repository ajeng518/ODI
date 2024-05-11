package com.homegravity.Odi.domain.chat.repository;

import com.homegravity.Odi.domain.chat.dto.ChatRoomDTO;
import com.homegravity.Odi.domain.chat.service.ChatMessageService;
import com.homegravity.Odi.domain.member.entity.Member;
import com.homegravity.Odi.domain.party.entity.Party;
import com.homegravity.Odi.domain.party.entity.PartyMember;
import com.homegravity.Odi.domain.party.respository.PartyMemberRepository;
import com.homegravity.Odi.domain.party.respository.PartyRepository;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ChatRoomRepository {
    // Redis CacheKeys
    private static final String CHAT_ROOMS = "CHAT_ROOM"; // 채팅룸 저장
    public static final String ENTER_INFO = "ENTER_INFO"; // 채팅룸에 입장한 클라이언트의 sessionId와 채팅룸 id를 맵핑한 정보 저장

    private final ChatMessageService chatMessageService;
    private final PartyMemberRepository partyMemberRepository;

    @Resource(name = "redisTemplate")
    private HashOperations<String, String, ChatRoomDTO> hashOpsChatRoom;
    @Resource(name = "redisTemplate")
    private HashOperations<String, String, String> hashOpsEnterInfo;

    // 모든 채팅방 조회
    public List<ChatRoomDTO> findAllRoomByMember(Member member) {
//        return hashOpsChatRoom.values(CHAT_ROOMS);
        // 유저 기반으로 참여하고 있는 파티 모두 조회
        List<Party> parties = partyMemberRepository.findAllByMember(member).stream()
                .map(PartyMember::getParty)
                .collect(Collectors.toList());
        // 파티에서 채팅방 ID 모두 조회
        List<String> roomIds = parties.stream()
                .map(Party::getRoomId)
                .collect(Collectors.toList());
        // 채팅방 ID으로 ChatRoomDTO 모두 조회
        List<ChatRoomDTO> chatRooms = roomIds.stream()
                .map(id -> findRoomById(id))
                .collect(Collectors.toList());
        return chatRooms;
    }

    // 특정 채팅방 조회
    public ChatRoomDTO findRoomById(String id) {
//        ChatRoomDTO chatRoom = hashOpsChatRoom.get(CHAT_ROOMS, id);
//        assert chatRoom != null;
        ChatRoomDTO chatRoom = ChatRoomDTO.builder()
                .roomId(id)
                .chatMessages(chatMessageService.getAllChatMessage(id))
                .lastMessage(chatMessageService.getLastMessage(id))
                .build();
        return chatRoom;
    }

    // 채팅방 생성 : 서버간 채팅방 공유를 위해 redis hash에 저장한다.
    public String createChatRoom() {
        ChatRoomDTO chatRoom = ChatRoomDTO.create();
        hashOpsChatRoom.put(CHAT_ROOMS, chatRoom.getRoomId(), chatRoom);
        return chatRoom.getRoomId();
    }

    // 유저가 입장한 채팅방ID와 유저 세션ID 맵핑 정보 저장
    public void setUserEnterInfo(String sessionId, String roomId) {
        hashOpsEnterInfo.put(ENTER_INFO, sessionId, roomId);
    }

    // 유저 세션으로 입장해 있는 채팅방 ID 조회
    public String getUserEnterRoomId(String sessionId) {
        return hashOpsEnterInfo.get(ENTER_INFO, sessionId);
    }

    // 유저 세션정보와 맵핑된 채팅방ID 삭제
    public void removeUserEnterInfo(String sessionId) {
        hashOpsEnterInfo.delete(ENTER_INFO, sessionId);
    }

}