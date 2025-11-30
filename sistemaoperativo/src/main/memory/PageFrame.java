//src/main/memory/PageFrame.java
package memory;

/**
 * Representa un marco de pagina en la memoria física
 */
public class PageFrame {
  private int frameId;
  private String processId;
  private int pageId;
  private boolean occupied;
  private int loadTime;
  private int lastAccessTime;
  
  public PageFrame(int frameId) {
    this.frameId = frameId;
    this.occupied = false;
    this.processId = null;
    this.pageId = -1;
    this.loadTime = -1;
    this.lastAccessTime = -1;
  }
  
  public void loadPage(String processId, int pageId, int currentTime) {
    this.processId = processId;
    this.pageId = pageId;
    this.occupied = true;
    this.loadTime = currentTime;
    this.lastAccessTime = currentTime;
  }
  
  public void unloadPage() {
    this.processId = null;
    this.pageId = -1;
    this.occupied = false;
    this.loadTime = -1;
    this.lastAccessTime = -1;
  }
  
  public void access(int currentTime) {
    this.lastAccessTime = currentTime;
  }
  
  // Getters
  public int getFrameId() {
    return frameId;
  }
  
  public String getProcessId() {
    return processId;
  }
  
  public int getPageId() {
    return pageId;
  }
  
  public boolean isOccupied() {
    return occupied;
  }
  
  public int getLoadTime() {
    return loadTime;
  }
  
  public int getLastAccessTime() {
    return lastAccessTime;
  }
  
  @Override
  public String toString() {
    if (occupied) {
      return String.format("Frame[%d]: %s-P%d (Load:%d, Access:%d)",
          frameId, processId, pageId, loadTime, lastAccessTime);
    } else {
      return String.format("Frame[%d]: FREE", frameId);
    }
  }
}
