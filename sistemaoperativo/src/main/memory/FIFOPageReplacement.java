//src/main/memory/FIFOPageReplacement.java
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
    // Extraer índices válidos desde la cola hasta encontrar uno ocupado.
    while (!loadOrder.isEmpty()) {
      Integer idx = loadOrder.poll();
      if (idx == null) continue;
      if (idx >= 0 && idx < frames.size()) {
        PageFrame f = frames.get(idx);
        if (f.isOccupied()) {
          return idx;
        } else {
          // índice obsoleto, seguir buscando
          continue;
        }
      }
      // índice inválido (por cualquier razón) => seguir
    }

    // Si no hay info válida en la cola, escoger por loadTime (fallback)
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

  @Override
  public void notifyPageLoaded(int frameIndex, String processId, int pageId, int currentTime) {
    // Evitar duplicados: solo añadir si no existe en la cola
    if (!loadOrder.contains(frameIndex)) {
      loadOrder.offer(frameIndex);
    }
  }
  @Override
  public void notifyPageUnloaded(int frameIndex) {
    // Remover cualquier ocurrencia del índice liberado (si existe)
    loadOrder.remove(frameIndex);
  }
  @Override public void notifyPageAccess(int frameIndex, String processId, int pageId, int currentTime) { 
    // FIFO no se ve afectado por accesos } @Override public void notifyPageLoaded(int frameIndex, String processId, int pageId, int currentTime) { 
    // Agregar al final de la cola loadOrder.offer(frameIndex); 
    // } @Override public void reset() { loadOrder.clear(); } @Override public String getName() 
    // { return "FIFO (First In, First Out)"; 
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
