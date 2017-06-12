package life.qbic.barcoder.view;

import life.qbic.barcoder.processes.IReadyRunnable;

public class PrintReadyRunnable implements IReadyRunnable {

  private BarcodeView view;
  private boolean success = false;

  public PrintReadyRunnable(BarcodeView view) {
    this.view = view;
  }

  @Override
  public void run() {
    view.printCommandsDone(this);
  }

  @Override
  public boolean wasSuccess() {
    return success;
  }

  @Override
  public void setSuccess(boolean b) {
    this.success = b;
  }

}
