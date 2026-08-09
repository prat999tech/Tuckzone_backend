package com.school.canteen.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A child a PARENT orders for, entered directly by the parent (name/class/section) rather
 * than linked to a real login-capable {@link StudentProfile}. Unlike the older
 * parent-child-link flow, a ward has no admission number and no roll number — it was never
 * required to already exist as a student account in the system.
 */
@Entity
@Table(name = "wards")
public class Ward extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parent;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "student_class", nullable = false)
    private String studentClass;

    @Column(name = "section", nullable = false)
    private String section;

    public User getParent() {
        return parent;
    }

    public void setParent(User parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentClass() {
        return studentClass;
    }

    public void setStudentClass(String studentClass) {
        this.studentClass = studentClass;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}
