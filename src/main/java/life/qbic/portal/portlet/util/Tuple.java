package life.qbic.portal.portlet.util;

public class Tuple {

  private Object one;
  private Object two;

  public Tuple(Object one, Object two) {
    this.one = one;
    this.two = two;
  }

  public Object getOne() {
    return one;
  }

  public Object getTwo() {
    return two;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((one == null) ? 0 : one.hashCode());
    result = prime * result + ((two == null) ? 0 : two.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Tuple other = (Tuple) obj;
    if (one == null) {
      if (other.one != null)
        return false;
    } else if (!one.equals(other.one))
      return false;
    if (two == null) {
      if (other.two != null)
        return false;
    } else if (!two.equals(other.two))
      return false;
    return true;
  }


}
