package com.homegravity.Odi.domain.party.entity;

import com.homegravity.Odi.domain.member.entity.Member;
import com.homegravity.Odi.global.entity.BaseBy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE party_member SET deleted_at = NOW() where party_member_id = ?")
public class PartyMember extends BaseBy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_member_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private RoleType role;

    @Column(name = "is_paid")
    private Boolean isPaid;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @JoinColumn(name = "party_id")
    private Party party;

    @Builder
    private PartyMember(RoleType role, Boolean isPaid, Party party, Member member) {
        this.role = role;
        this.isPaid = isPaid;
        this.party = party;
        this.member = member;
    }

    public static PartyMember of(RoleType role, Boolean isPaid, Party party, Member member) {
        return PartyMember.builder()
                .role(role)
                .isPaid(isPaid)
                .party(party)
                .member(member)
                .build();
    }

    public void updateIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }


}