package com.homegravity.Odi.domain.party.service;

import com.homegravity.Odi.domain.member.entity.Member;
import com.homegravity.Odi.domain.party.dto.request.PartySettlementRequestDto;
import com.homegravity.Odi.domain.party.dto.response.PartySettlementResponseDto;
import com.homegravity.Odi.domain.party.entity.*;
import com.homegravity.Odi.domain.party.respository.PartyMemberRepository;
import com.homegravity.Odi.domain.party.respository.PartySettlementRepository;
import com.homegravity.Odi.domain.payment.service.PointService;
import com.homegravity.Odi.global.response.error.ErrorCode;
import com.homegravity.Odi.global.response.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PartySettlementService {

    private final PartyMemberRepository partyMemberRepository;
    private final PartySettlementRepository partySettlementRepository;
    private final PartyService partyService;
    private final PointService pointService;

    // 동승 파티 성사 (모집 완료)
    public PartySettlementResponseDto matchParty(Member member, Long partyId) {

        // 파티 존재 및 모집완료를 할 수 있는 상황인지 확인
        Party party = partyService.getParty(partyId);
        if (party.getState() != StateType.GATHERING) {
            throw new BusinessException(ErrorCode.PARTY_STATE_TYPE_ERROR, "파티가 모집중이 아닙니다.");
        }

        // 요청자가 파티장인지 확인
        // TODO: 권한 처리로 수정?
        if (partyMemberRepository.findParticipantRole(party, member) != RoleType.ORGANIZER) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "파티장만 요청할 수 있습니다.");
        }

        // 파티 멤버 조회
        List<PartyMember> partyMembers = partyMemberRepository.findAllPartyMember(party);

        // 예상 택시비 (임시: 10000원으로 가정)
        // TODO: 예상택시비 가져오는 로직으로 수정
        int expectedCost = 10000;
        int prepaidAmount = expectedCost / party.getCurrentParticipants();

        // 선불 가능 여부 확인 및 선불!
        canPrepaidAllMembers(partyMembers, prepaidAmount);
        partyMembers.forEach(partyMember -> {
            prePaid(partyMember, expectedCost, prepaidAmount);
        });

        // 모집 완료 설정 및 새 정산 데이터 생성
        party.updateState(StateType.COMPLETED);
        return PartySettlementResponseDto.from(partySettlementRepository.save(PartySettlement.of(party, expectedCost, member.getId())));
    }

    // 택시 이용 완료 = 동승 정산 요청
    public PartySettlementResponseDto askForSettlement(Member member, Long partyId, PartySettlementRequestDto requestDto) {

        // 파티 존재 및 모집완료를 할 수 있는 상황인지 확인
        Party party = partyService.getParty(partyId);
        if (party.getState() != StateType.COMPLETED) {
            throw new BusinessException(ErrorCode.PARTY_STATE_TYPE_ERROR, "파티가 모집중이 아닙니다.");
        }

        // 요청자가 파티원인지
        // TODO: 권한 처리로 수정?
        PartyMember partyMember = partyMemberRepository.findByPartyAndMember(party, member).orElseThrow(
                () -> new BusinessException(ErrorCode.FORBIDDEN_ERROR, "파티원만 요청할 수 있습니다.")
        );

        // 정산 금액 계산
        int cost = requestDto.getCost();
        int settlementAmount = cost / party.getCurrentParticipants();

        // 정산 정보 업데이트
        PartySettlement partySettlement = party.getPartySettlement();
        partySettlement.updateSettlementInfo(requestDto.getImage(), cost, member.getId());

        // 파티원들에게 정산 요청
        partyMemberRepository.findAllPartyMember(party).forEach(
                pm -> {
                    pm.updateSettleAmount(settlementAmount);
                }
        );

        // 정산 요청자 정산 완료 처리
        // (cost - settlementAmount) -> 1/N 정산에서 포함되지 않는 금액. 오디가 내드립니다..^__^
        pointService.getBackPrePaidCost(member, party, partyMember.getPaidAmount() + (cost - settlementAmount * party.getCurrentParticipants()));
        partyMember.updateIsPaid(true);
        partySettlement.paidCost(settlementAmount - partyMember.getPaidAmount());

        // 정산 요청으로 파티 상태 업데이트
        party.updateState(StateType.SETTLING);
        return PartySettlementResponseDto.from(partySettlement);
    }

    // 선불 할 수 있는지 검사
    public void canPrepaidAllMembers(List<PartyMember> partyMembers, int prepaidAmount) {

        for (PartyMember partyMember : partyMembers) {
            if (partyMember.getMember().getPoint() < prepaidAmount) {
                throw new BusinessException(ErrorCode.POINT_LACK_ERROR, String.format("선불시도 실패로 동승 성사에 실패했습니다. %s님의 포인트가 부족합니다.", partyMember.getMember().getNickname()));
            }
        }
    }

    public void prePaid(PartyMember partyMember, int expectedCost, int prepaidAmount) {

        // 선불 포인트 차감
        pointService.usePoint(partyMember.getMember(), partyMember.getParty(), expectedCost, prepaidAmount, partyMember.getPaidAmount());

        // 파티원 선불 내역 업데이트
        partyMember.updatePaidInfo(prepaidAmount);
        partyMemberRepository.save(partyMember);
    }


}