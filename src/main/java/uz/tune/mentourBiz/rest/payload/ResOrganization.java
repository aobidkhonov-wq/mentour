package uz.tune.mentourBiz.rest.payload;

import lombok.Data;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.payload.res.ResSubscriptionPlan;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class ResOrganization {
    private UUID uuid;
    private String name;
    private ResAttachment logo;
    private SchoolStatus status;
    private Instant expiresAt;
    private ResSubscriptionPlan resSubscriptionPlan;
    private List<ResSchoolInfo> schools;
    private List<ResSchooLDirector> resSchooLDirector;
    private List<ResBooks> availableBooks;

    public ResOrganization(Organization org) {
        this.uuid = org.getUuid();
        this.name = org.getName();
        this.status = org.getStatus();
        this.expiresAt = org.getExpiresAt();
        if (org.getSubscriptionPlan() != null) {
            this.resSubscriptionPlan = new ResSubscriptionPlan(org.getSubscriptionPlan());
        }
        if (org.getLogo() != null) {
            this.logo = new ResAttachment(org.getLogo());
        }

        if (org.getSchoolDirector() != null) {
            this.resSchooLDirector = org.getSchoolDirector().stream()
                    .map(ResSchooLDirector::new)
                    .toList();
        }

        if (org.getSchools() != null) {
            this.schools = org.getSchools().stream()
                    .map(ResSchoolInfo::new)
                    .toList();
        }

        if(org.getSchools() != null) {
            this.availableBooks = org.getSchools().stream()
                    .filter(s -> s.getAllowedBooks() != null)
                    .flatMap(s -> s.getAllowedBooks().stream())
                    .map(ResBooks::new)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(ResBooks::getBookUuid, b -> b, (b1, b2) -> b1),
                            m -> new ArrayList<>(m.values())
                    ));
        }
    }
}