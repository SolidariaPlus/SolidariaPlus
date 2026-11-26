package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.kafka;

import com.solidariaplus.identity.identityaccess.application.port.in.EndMembershipUseCase;
import com.solidariaplus.identity.identityaccess.application.port.in.RegisterMemberUseCase;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberId;
import com.solidariaplus.identity.identityaccess.domain.model.vo.MemberNumber;
import java.util.UUID;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
class MemberManagementEventsListener {
    private static final String MEMBER_ADMITTED = "ec.solidariaplus.membership.MemberAdmitted.v1";
    private static final String MEMBERSHIP_ENDED = "ec.solidariaplus.membership.MembershipEnded.v1";

    private final JsonMapper mapper;
    private final RegisterMemberUseCase registerMember;
    private final EndMembershipUseCase endMembership;

    MemberManagementEventsListener(
            JsonMapper mapper, RegisterMemberUseCase registerMember, EndMembershipUseCase endMembership) {
        this.mapper = mapper;
        this.registerMember = registerMember;
        this.endMembership = endMembership;
    }

    @RetryableTopic(
            attempts = "5",
            backOff = @BackOff(delay = 1000),
            retryTopicSuffix = ".identity-access.retry",
            dltTopicSuffix = ".identity-access.dlt",
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            autoCreateTopics = "false")
    @KafkaListener(topics = "member-management.events", groupId = "identity-access-service")
    public void onMessage(String payload) {
        CloudEvent event = mapper.readValue(payload, CloudEvent.class);
        switch (event.type()) {
            case MEMBER_ADMITTED -> {
                MemberAdmitted data = mapper.treeToValue(event.data(), MemberAdmitted.class);
                registerMember.register(new RegisterMemberUseCase.Command(
                        event.id(), new MemberId(data.memberId()), new MemberNumber(data.memberNumber())));
            }
            case MEMBERSHIP_ENDED -> {
                MembershipEnded data = mapper.treeToValue(event.data(), MembershipEnded.class);
                endMembership.end(new EndMembershipUseCase.Command(event.id(), new MemberId(data.memberId())));
            }
            default -> {}
        }
    }

    record CloudEvent(UUID id, String type, JsonNode data) {}

    record MemberAdmitted(UUID memberId, String memberNumber) {}

    record MembershipEnded(UUID memberId) {}
}
