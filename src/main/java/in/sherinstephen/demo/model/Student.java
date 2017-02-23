package in.sherinstephen.demo.model;

import java.util.Date;
import java.util.List;

/**
 * @author Sherin (I073367)
 * @since 21/2/17
 */
public class Student {
    private Integer id;
    private String firstName;
    private String lastName;
    private Date dateOfBirth;
    private Department department;
    private List<Grade> grades;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Grade> getGrades() {
        return grades;
    }

    public void setGrades(List<Grade> grades) {
        this.grades = grades;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Student student = (Student) o;

        return id == student.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", department=" + department +
                ", grades=" + grades +
                '}';
    }

    public static class Grade {
        String subject;
        int grade;
        int max;

        public Grade(String subject, int grade, int max) {
            this.subject = subject;
            this.grade = grade;
            this.max = max;
        }

        @Override
        public String toString() {
            return "(" + grade + "/" + max + ")";
        }
    }
}
