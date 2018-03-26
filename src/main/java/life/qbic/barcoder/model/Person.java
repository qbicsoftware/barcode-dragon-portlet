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
package life.qbic.barcoder.model;

import java.util.List;

public class Person {
  private String zdvID;
  private String firstName;
  private String lastName;
  private String email;
  private String phone;
  private List<Affiliation> affiliations;
  private int instituteID;

  public Person(String zdvID, String firstName, String lastName, String email, String telephone,
      List<Affiliation> affiliations) {
    super();
    this.zdvID = zdvID;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.phone = telephone;
    this.affiliations = affiliations;
  }

  public Person(String zdvID, String first, String last, String email, String tel,
      int instituteID) {
    super();
    this.zdvID = zdvID;
    this.firstName = first;
    this.lastName = last;
    this.email = email;
    this.phone = tel;
    this.instituteID = instituteID;
  }

  public void addAffiliation(Affiliation affiliation) {
    affiliations.add(affiliation);
  }

  public List<Affiliation> getAffiliations() {
    return affiliations;
  }

  public String getZdvID() {
    return zdvID;
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

}
