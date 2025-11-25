package memory;

import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Algoritmo de reemplazo de paginas FIFO (First In, First Out)
 * Reemplaza la pagina que ha estado mas tiempo en memoria
 */
public class FIFOPageReplacement implements PageReplacementAlgorithm {
  private Queue<Integer> loadOrder;
  
  public FIFOPageReplacement() {
    this.loadOrder = new LinkedList<>();
  }
  
  @Override
  public int selectVictimFrame(List<PageFrame> frames, int currentTime) {
    // Seleccionar la pagina que llego primero (mas antigua)
    if (loadOrder.isEmpty()) {
      // Si no hay registro, buscar el marco con menor loadTime
      int oldestFrameIndex = -1;
      int oldestTime = Integer.MAX_VALUE;
      
      for (int i = 0; i < frames.size(); i++) {
        PageFrame frame = frames.get(i);
        if (frame.isOccupied() && frame.getLoadTime() < oldestTime) {
          oldestTime = frame.getLoadTime();
          oldestFrameIndex = i;
        }
      }
      return oldestFrameIndex;
    }
    
    // Obtener el marco mas antiguo de la cola
    int oldestFrameIndex = loadOrder.poll();
    return oldestFrameIndex;
  }
  
  @Override
  public void notifyPageAccess(int frameIndex, String processId, int pageId, int currentTime) {
    // FIFO no se ve afectado por accesos
  }
  
  @Override
  public void notifyPageLoaded(int frameIndex, String processId, int pageId, int currentTime) {
    // Agregar al final de la cola
    loadOrder.offer(frameIndex);
  }
  
  @Override
  public void reset() {
    loadOrder.clear();
  }
  
  @Override
  public String getName() {
    return "FIFO (First In, First Out)";
  }
}
