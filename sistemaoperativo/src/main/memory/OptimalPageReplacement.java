package memory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Algoritmo de reemplazo OPTIMAL corregido.
 * - El puntero avanza SOLO cuando realmente se accede a una página.
 * - Maneja correctamente accesos futuros por proceso.
 * - No se adelanta en función de ráfagas de CPU.
 */
public class OptimalPageReplacement implements PageReplacementAlgorithm {

  private final Map<String, List<Integer>> futureAccesses;
  private final Map<String, Integer> currentPositions;

  public OptimalPageReplacement() {
    this.futureAccesses = new HashMap<>();
    this.currentPositions = new HashMap<>();
  }

  /**
   * Registra la secuencia futura de accesos del proceso.
   */
  public void setFutureAccesses(String processId, List<Integer> accessSequence) {
    futureAccesses.put(processId, new ArrayList<>(accessSequence));
    currentPositions.put(processId, 0);
  }

  /**
   * Avanza UNA posición cuando se accede realmente a memoria.
   */
  public void advancePointerOnRealAccess(String processId) {
    int current = currentPositions.getOrDefault(processId, 0);
    int maxSize = futureAccesses.getOrDefault(processId, List.of()).size();

    if (current < maxSize) {
      currentPositions.put(processId, current + 1);
    }
  }

  /**
   * Elimina datos de un proceso finalizado
   */
  public void unregisterProcess(String processId) {
    futureAccesses.remove(processId);
    currentPositions.remove(processId);
  }

  @Override
  public int selectVictimFrame(List<PageFrame> frames, int currentTime) {

    int victimIndex = -1;
    int farthestUse = Integer.MIN_VALUE;

    for (int i = 0; i < frames.size(); i++) {

      PageFrame frame = frames.get(i);

      // Si el marco está libre, usarlo
      if (!frame.isOccupied()) {
        return i;
      }

      int nextUse = findNextUse(frame.getProcessId(), frame.getPageId());

      // Si NO SE USARÁ MÁS: ¡victima inmediata!
      if (nextUse == -1) {
        return i;
      }

      // Escoger página cuyo próximo uso está MÁS LEJOS
      if (nextUse > farthestUse) {
        farthestUse = nextUse;
        victimIndex = i;
      }
    }

    // Si todo falló, algo anda raro, pero selecciona el primero
    return victimIndex != -1 ? victimIndex : 0;
  }

  /**
   * Busca el próximo uso futuro real de la página.
   * Retorna:
   * - índice en lista futura
   * - -1 si nunca más se usa
   */
  private int findNextUse(String processId, int pageId) {

    List<Integer> accesses = futureAccesses.get(processId);
    if (accesses == null || accesses.isEmpty()) {
      return -1;
    }

    int start = currentPositions.getOrDefault(processId, 0);

    for (int i = start; i < accesses.size(); i++) {
      if (accesses.get(i) == pageId) {
        return i;
      }
    }

    return -1; // ya no se usará en el futuro
  }

  @Override
  public void notifyPageAccess(int frameIndex, String processId, int pageId, int currentTime) {
    // Aquí avanzamos el puntero SOLO si hay un acceso real
    advancePointerOnRealAccess(processId);
  }

  @Override
  public void notifyPageLoaded(int frameIndex, String processId, int pageId, int currentTime) {
    // No se requiere acción
  }

  @Override
  public void reset() {
    futureAccesses.clear();
    currentPositions.clear();
  }

  @Override
  public String getName() {
    return "Optimal (Corregido)";
  }
}
