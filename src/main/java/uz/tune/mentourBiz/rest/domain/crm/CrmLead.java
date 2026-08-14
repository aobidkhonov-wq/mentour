package uz.tune.mentourBiz.rest.domain.crm;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;

import java.util.UUID;

@Table(name = "crm_leads")
@Entity
@Getter
@Setter
public class CrmLead extends BaseEntity {

    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "student_surname")
    private String studentSurname;

    @Column(name = "phone")
    private String phone;

    @Column(name = "student_phone")
    private String studentPhone;

    @Column(name = "demo_status")
    private String demoStatus;

    public String resolvedName() {
        if (name != null && !name.isBlank()) return name;
        String sn = (studentName != null ? studentName : "") + (studentSurname != null ? " " + studentSurname : "");
        return sn.isBlank() ? "" : sn.trim();
    }

    public String resolvedPhone() {
        if (phone != null && !phone.isBlank()) return phone;
        return studentPhone != null ? studentPhone : "";
    }
}
