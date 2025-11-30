//src/main/memory/PageReplacementAlgorithm.java
package memory;

import java.util.List;

/**
 * Interface para algoritmos de reemplazo de paginas
 */
public interface PageReplacementAlgorithm {
  
  /**
   * Selecciona un marco de pagina para ser reemplazad
   * @param frames Lista de marcos disponibles
   * @param currentTime Tiempo actual de simulacion
   * @return Indice del marco a reemplazar
   */
  int selectVictimFrame(List<PageFrame> frames, int currentTime);
  
  /**
   * Notifica al algoritmo que una pagina fue accedida
   * @param frameIndex Indice del marco accedido
   * @param currentTime Tiempo actual
   */
  void notifyPageAccess(int frameIndex, String processId, int pageId, int currentTime);
  
  /**
   * Notifica al algoritmo que una pagina fue cargada
   * @param frameIndex Indice del marco donde se cargo
   * @param currentTime Tiempo actual
   */
  void notifyPageLoaded(int frameIndex, String processId, int pageId, int currentTime);

  /**
   * Notifica al algoritmo que un marco fue liberado (page unloaded).
   * Permite limpiar estructuras internas (colas, mapas, etc.) para evitar índices obsoletos.
   * @param frameIndex Indice del marco liberado
   */
  void notifyPageUnloaded(int frameIndex);
  
  //Reinicia el estado del algoritmo
  void reset();
  
  /**
   * Obtiene el nombre del algoritmo
   * @return Nombre del algoritmo
   */
  String getName();
}
