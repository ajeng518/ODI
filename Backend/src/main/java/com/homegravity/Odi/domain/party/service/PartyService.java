package com.homegravity.Odi.domain.party.service;

import com.homegravity.Odi.domain.map.service.MapService;
import com.homegravity.Odi.domain.member.entity.Member;
import com.homegravity.Odi.domain.member.repository.MemberRepository;
import com.homegravity.Odi.domain.party.dto.PartyDTO;
import com.homegravity.Odi.domain.party.dto.PartyMemberDTO;
import com.homegravity.Odi.domain.party.dto.request.PartyRequestDTO;
import com.homegravity.Odi.domain.party.dto.request.SelectPartyRequestDTO;
import com.homegravity.Odi.domain.party.dto.response.PartyResponseDTO;
import com.homegravity.Odi.domain.party.entity.Party;
import com.homegravity.Odi.domain.party.entity.PartyBoardStats;
import com.homegravity.Odi.domain.party.entity.PartyMember;
import com.homegravity.Odi.domain.party.entity.RoleType;
import com.homegravity.Odi.domain.party.respository.PartyBoardStatsRepository;
import com.homegravity.Odi.domain.party.respository.PartyMemberRepository;
import com.homegravity.Odi.domain.party.respository.PartyRepository;
import com.homegravity.Odi.global.redis.handler.TransactionHandler;
import com.homegravity.Odi.global.redis.repository.RedisLockRepository;
import com.homegravity.Odi.global.response.error.ErrorCode;
import com.homegravity.Odi.global.response.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;
    private final PartyBoardStatsRepository partyBoardStatsRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final MemberRepository memberRepository;

    private final MapService mapService;
    private final RedisLockRepository redisLockRepository;
    private final TransactionHandler transactionHandler;

    @Transactional
    public Long createParty(PartyRequestDTO partyRequestDTO, Member member) {

        Party party = partyRepository.save(Party.of(partyRequestDTO, member.getGender()));

        PartyBoardStats partyBoardStats = PartyBoardStats.of(0, 0); // 생성시 조회수 초기화
        partyMemberRepository.save(PartyMember.of(RoleType.ORGANIZER, false, party, member));
        partyBoardStats.updateParty(party);

        return party.getId();
    }

    @Transactional
    public PartyResponseDTO getPartyDetail(Long partyId, Member member) {

        Party party = partyRepository.findParty(partyId);
        if (party == null) { // 파티가 없다면 삭제되었거나 요청에 문제가 있음
            throw BusinessException.builder().errorCode(ErrorCode.NOT_FOUND_ERROR).message(ErrorCode.NOT_FOUND_ERROR.getMessage()).build();
        }

        // 조회수, 신청자 수 갱신
        PartyBoardStats partyBoardStats = partyBoardStatsRepository.findPartyBoardStats(party);
        partyBoardStats.addViewCount(); // 조회수 증가

        // 파티 참여 신청자 수 조회
        partyBoardStats.updateRequestCount(partyMemberRepository.countAllPartyGuests(party));

        // 파티원, 파티 참여 신정자 목록 조회
        RoleType role = partyMemberRepository.findParticipantRole(party, member);

        List<PartyMemberDTO> guests = null;
        if (role != null && role.equals(RoleType.ORGANIZER)) { // 방장이라면 신청자 목록도 조회 가능
            guests = partyMemberRepository.findAllPartyMember(party, RoleType.REQUESTER);
        }

        List<PartyMemberDTO> participants = partyMemberRepository.findAllPartyMember(party, RoleType.PARTICIPANT);

        // Naver Map API 호출
        String pathInfo = mapService.getNaverPathInfo(party.getDeparturesLocation().getX(), party.getDeparturesLocation().getY(), party.getArrivalsLocation().getX(), party.getArrivalsLocation().getY());

        return PartyResponseDTO.of(party, partyBoardStats, role, participants, guests, pathInfo);
    }

    @Transactional(readOnly = true)
    public Slice<PartyDTO> getAllParties(Pageable pageable, SelectPartyRequestDTO requestDTO) {

        return partyRepository.findAllParties(pageable, requestDTO);
    }

    public Long joinParty(Long partyId, Member member) {

        String key = "joinParty_"+partyId.toString() +"_" +member.getId();

        //lettuce를 활용한 스핀락 활용
        return redisLockRepository.runOnLettuceLock(
                key, () -> transactionHandler.runOnWriteTransaction(
                        () -> joinPartyLogic(partyId, member)
                ));
    }

    public Long joinPartyLogic(Long partyId, Member member) {
        Party party = partyRepository.findParty(partyId);

        if (party == null) {//party가 없을 경우 예외 처리
            throw BusinessException.builder()
                    .errorCode(ErrorCode.NOT_FOUND_ERROR).message(ErrorCode.NOT_FOUND_ERROR.getMessage()).build();
        }

        boolean isPartyMember = partyMemberRepository.existPartyMember(party, member);

        // 해당 파티에 이미 신청하려는 사용자가 있다면 중복 신청 불가
        if (isPartyMember) {
            throw BusinessException.builder()
                    .errorCode(ErrorCode.PARTY_MEMBER_ALREADY_JOIN_EXIST).message(ErrorCode.PARTY_MEMBER_ALREADY_JOIN_EXIST.getMessage()).build();
        }

        PartyMember partyMember = PartyMember.of(RoleType.REQUESTER, false, party, member);

        partyMemberRepository.save(partyMember);

        return partyMember.getId();
    }

    public boolean deleteJoinParty(Long partyId, Member member){
        String key = "deleteJoinParty"+partyId.toString() +"_" +member.getId();

        return redisLockRepository.runOnLettuceLock(
                key, () -> transactionHandler.runOnWriteTransaction(
                        () -> deleteJoinPartyLogic(partyId, member)));
    }

    public boolean deleteJoinPartyLogic(Long partyId, Member member){
        Party party =partyRepository.findParty(partyId);

        if (party == null) {//party가 없을 경우 예외 처리
            throw BusinessException.builder()
                    .errorCode(ErrorCode.NOT_FOUND_ERROR).message(ErrorCode.NOT_FOUND_ERROR.getMessage()).build();
        }

        PartyMember partyMember = partyMemberRepository.findPartyMemberByMember(party, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTY_MEMBER_NOT_EXIST, ErrorCode.PARTY_MEMBER_NOT_EXIST.getMessage()));

        partyMemberRepository.delete(partyMember);
        return true;
    }

    // 파티 조회
    @Transactional(readOnly = true)
    public Party getParty(Long partyId) {
        return Optional.ofNullable(partyRepository.findParty(partyId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "파티를 찾을 수 없습니다."));
    }
}
