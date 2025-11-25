package memory;

import java.util.List;

/**
 * Interface para algoritmos de reemplazo de páginas
 */
public interface PageReplacementAlgorithm {
  
  /**
   * Selecciona un marco de página para ser reemplazado
   * 
   * @param frames Lista de marcos disponibles
   * @param currentTime Tiempo actual de simulación
   * @return Índice del marco a reemplazar
   */
  int selectVictimFrame(List<PageFrame> frames, int currentTime);
  
  /**
   * Notifica al algoritmo que una página fue accedida
   * 
   * @param frameIndex Índice del marco accedido
   * @param currentTime Tiempo actual
   */
  void notifyPageAccess(int frameIndex, String processId, int pageId, int currentTime);
  
  /**
   * Notifica al algoritmo que una página fue cargada
   * 
   * @param frameIndex Índice del marco donde se cargó
   * @param currentTime Tiempo actual
   */
  void notifyPageLoaded(int frameIndex, String processId, int pageId, int currentTime);
  
  /**
   * Reinicia el estado del algoritmo
   */
  void reset();
  
  /**
   * Obtiene el nombre del algoritmo
   * 
   * @return Nombre del algoritmo
   */
  String getName();
}
