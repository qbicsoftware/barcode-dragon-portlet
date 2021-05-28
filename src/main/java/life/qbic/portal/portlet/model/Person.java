/*******************************************************************************
 * QBiC Project Wizard enables users to create hierarchical experiments including different study
 * conditions using factorial design. Copyright (C) "2016" Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.portal.portlet.model;

public class Person {
  private String userID;
  private String title;
  private String firstName;
  private String lastName;
  private String email;
  private String phone;
  private Affiliation affiliation;
  private int instituteID;

  public Person(String userID, String firstName, String lastName, String email, String telephone) {
    super();
    this.userID = userID;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.phone = telephone;
  }

  public Person(String userID, String title, String first, String last, String email, String tel,
      int instituteID, Affiliation affiliation) {
    super();
    this.userID = userID;
    this.title = title;
    this.firstName = first;
    this.lastName = last;
    this.email = email;
    this.phone = tel;
    this.instituteID = instituteID;
    this.affiliation = affiliation;
  }

  public String getUserID() {
    return userID;
  }

  public String getTitle() {
    return title;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public int getInstituteID() {
    return instituteID;
  }

  public Affiliation getAffiliation() {
    return affiliation;
  }

  @Override
  public String toString() {
    return "Person{" +
            "userID='" + userID + '\'' +
            ", title='" + title + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", email='" + email + '\'' +
            ", phone='" + phone + '\'' +
            ", affiliation=" + affiliation +
            ", instituteID=" + instituteID +
            '}';
  }
}
