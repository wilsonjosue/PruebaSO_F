package memory;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Algoritmo de reemplazo de páginas LRU (Least Recently Used)
 * Reemplaza la página que no ha sido usada por más tiempo
 */
public class LRUPageReplacement implements PageReplacementAlgorithm {
  private Map<Integer, Integer> lastAccessTime;
  
  public LRUPageReplacement() {
    this.lastAccessTime = new HashMap<>();
  }
  
  @Override
  public int selectVictimFrame(List<PageFrame> frames, int currentTime) {
    // Seleccionar la página con el tiempo de acceso más antiguo
    int victimIndex = -1;
    int oldestAccessTime = Integer.MAX_VALUE;
    
    for (int i = 0; i < frames.size(); i++) {
      PageFrame frame = frames.get(i);
      if (frame.isOccupied()) {
        int accessTime = lastAccessTime.getOrDefault(i, frame.getLoadTime());
        if (accessTime < oldestAccessTime) {
          oldestAccessTime = accessTime;
          victimIndex = i;
        }
      }
    }
    
    // Limpiar el registro del marco víctima
    if (victimIndex != -1) {
      lastAccessTime.remove(victimIndex);
    }
    
    return victimIndex;
  }
  
  @Override
  public void notifyPageAccess(int frameIndex, String processId, int pageId, int currentTime) {
    // Actualizar el tiempo de último acceso
    lastAccessTime.put(frameIndex, currentTime);
  }
  
  @Override
  public void notifyPageLoaded(int frameIndex, String processId, int pageId, int currentTime) {
    // Registrar el tiempo de carga como tiempo de acceso inicial
    lastAccessTime.put(frameIndex, currentTime);
  }
  
  @Override
  public void reset() {
    lastAccessTime.clear();
  }
  
  @Override
  public String getName() {
    return "LRU (Least Recently Used)";
  }
}
