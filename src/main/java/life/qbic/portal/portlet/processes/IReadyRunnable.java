package life.qbic.portal.portlet.processes;

public interface IReadyRunnable extends Runnable {
    
  public boolean wasSuccess();
  
  public void setSuccess(boolean b);

}
