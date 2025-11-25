package memory;

import model.Process;
import scheduler.SimulationClock;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestor de memoria virtual con soporte para reemplazo de páginas
 * Simula la memoria física dividida en marcos de página
 */
public class MemoryManager {
  private List<PageFrame> frames;
  private int totalFrames;
  private PageReplacementAlgorithm replacementAlgorithm;
  private Lock memoryLock;
  private Map<String, Process> processRegistry;

  // Métricas
  private int pageFaults;
  private int pageReplacements;
  private Map<String, Integer> processPageFaults;
  private Map<String, Set<Integer>> pageTable; // Tabla de páginas por proceso

  public MemoryManager(int totalFrames, PageReplacementAlgorithm algorithm) {
    if (totalFrames <= 0) {
      throw new IllegalArgumentException("El número de marcos debe ser positivo");
    }

    this.totalFrames = totalFrames;
    this.frames = new ArrayList<>();
    for (int i = 0; i < totalFrames; i++) {
      frames.add(new PageFrame(i));
    }

    this.replacementAlgorithm = algorithm;
    this.memoryLock = new ReentrantLock();
    this.pageFaults = 0;
    this.pageReplacements = 0;
    this.processPageFaults = new HashMap<>();
    this.pageTable = new HashMap<>();
    this.processRegistry = new HashMap<>();

    System.out.println(String.format("MemoryManager inicializado: %d marcos, Algoritmo: %s",
        totalFrames, algorithm.getName()));
  }

  /**
   * Prepara la memoria para un proceso. Para demanda real sólo registramos el proceso
   * y garantizamos que existe al menos una página inicial cargada si es necesario.
   */
  public boolean loadPagesForProcess(Process process) {
    memoryLock.lock();
    try {
      String pid = process.getPid();
      registerProcessIfNeeded(process);
      pageTable.putIfAbsent(pid, new HashSet<>());

      if (process.getRequiredPages() > 0) {
        ensurePageLoadedInternal(process, 0);
      }

      process.signalMemoryReady();
      System.out.println(String.format("[MEMORIA] Proceso %s listo para ejecución", pid));
      return true;

    } finally {
      memoryLock.unlock();
    }
  }

  /**
   * Carga bajo demanda una página específica si todavía no se encuentra en memoria.
   */
  public void ensurePageLoaded(Process process, int pageId) {
    if (process == null || pageId < 0) {
      return;
    }

    memoryLock.lock();
    try {
      registerProcessIfNeeded(process);
      pageTable.putIfAbsent(process.getPid(), new HashSet<>());
      ensurePageLoadedInternal(process, pageId);
    } finally {
      memoryLock.unlock();
    }
  }

  /**
   * Carga una página específica en memoria
   * 
   * @param processId ID del proceso
   * @param pageId    ID de la página
   */
  private void loadPageInternal(Process process, int pageId) {
    String processId = process.getPid();
    int currentTime = SimulationClock.getTime();

    // Buscar un marco libre
    int frameIndex = findFreeFrame();

    if (frameIndex == -1) {
      // No hay marcos libres, aplicar algoritmo de reemplazo
      System.out.println(String.format("[MEMORIA] No hay marcos libres, aplicando %s",
          replacementAlgorithm.getName()));

      frameIndex = replacementAlgorithm.selectVictimFrame(frames, currentTime);

      if (frameIndex == -1) {
        System.err.println("[MEMORIA] ERROR: No se pudo seleccionar un marco víctima");
        return;
      }

      // Reemplazar la página
      PageFrame victimFrame = frames.get(frameIndex);
      String victimProcess = victimFrame.getProcessId();
      int victimPage = victimFrame.getPageId();

      System.out.println(String.format("[MEMORIA] Reemplazando página %s-P%d con %s-P%d en Frame[%d]",
          victimProcess, victimPage, processId, pageId, frameIndex));

      // Actualizar tabla de páginas de la víctima
      if (pageTable.containsKey(victimProcess)) {
        pageTable.get(victimProcess).remove(victimPage);
      }

      Process victim = processRegistry.get(victimProcess);
      if (victim != null) {
        victim.removeLoadedPage(victimPage);
      }

      victimFrame.unloadPage();
      pageReplacements++;
    }

    // Cargar la nueva página
    PageFrame frame = frames.get(frameIndex);
    frame.loadPage(processId, pageId, currentTime);

    // Actualizar tabla de páginas
    pageTable.get(processId).add(pageId);
    process.addLoadedPage(pageId);

    // Notificar al algoritmo de reemplazo
    replacementAlgorithm.notifyPageLoaded(frameIndex, processId, pageId, currentTime);

    // Registrar fallo de página
    pageFaults++;
    processPageFaults.put(processId, processPageFaults.getOrDefault(processId, 0) + 1);

    System.out.println(String.format("[MEMORIA] Página %s-P%d cargada en Frame[%d] - Fallo de página #%d",
      processId, pageId, frameIndex, pageFaults));
  }

  /**
   * Busca un marco libre en memoria
   * 
   * @return Índice del marco libre, o -1 si no hay ninguno
   */
  private int findFreeFrame() {
    for (int i = 0; i < frames.size(); i++) {
      if (!frames.get(i).isOccupied()) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Verifica si una página está cargada en memoria
   * 
   * @param processId ID del proceso
   * @param pageId    ID de la página
   * @return true si está cargada
   */
  public boolean isPageLoaded(String processId, int pageId) {
    memoryLock.lock();
    try {
      return isPageLoadedInternal(processId, pageId);
    } finally {
      memoryLock.unlock();
    }
  }

  private boolean isPageLoadedInternal(String processId, int pageId) {
    Set<Integer> pages = pageTable.get(processId);
    return pages != null && pages.contains(pageId);
  }

  /**
   * Libera todas las páginas de un proceso
   * 
   * @param process Proceso a liberar
   */
  public void freePagesForProcess(Process process) {
    memoryLock.lock();
    try {
      String pid = process.getPid();

      System.out.println(String.format("\n[MEMORIA] Liberando páginas del proceso %s", pid));

      // Liberar todos los marcos que contienen páginas de este proceso
      for (PageFrame frame : frames) {
        if (frame.isOccupied() && frame.getProcessId().equals(pid)) {
          System.out.println(String.format("[MEMORIA] Liberando Frame[%d] (%s-P%d)",
              frame.getFrameId(), pid, frame.getPageId()));
          frame.unloadPage();
        }
      }

      // Limpiar tabla de páginas
      pageTable.remove(pid);
      processRegistry.remove(pid);
      if (replacementAlgorithm instanceof OptimalPageReplacement) {
        OptimalPageReplacement optimal = (OptimalPageReplacement) replacementAlgorithm;
        optimal.unregisterProcess(pid);
      }

      process.clearLoadedPages();

    } finally {
      memoryLock.unlock();
    }
  }

  /**
   * Accede a una página (actualiza información de acceso)
   * 
   * @param processId ID del proceso
   * @param pageId    ID de la página
   */
  public void accessPage(String processId, int pageId) {
    memoryLock.lock();
    try {
      int currentTime = SimulationClock.getTime();

      // Buscar el marco que contiene esta página
      for (int i = 0; i < frames.size(); i++) {
        PageFrame frame = frames.get(i);
        if (frame.isOccupied() &&
            frame.getProcessId().equals(processId) &&
            frame.getPageId() == pageId) {

          frame.access(currentTime);
          replacementAlgorithm.notifyPageAccess(i, processId, pageId, currentTime);
          return;
        }
      }
    } finally {
      memoryLock.unlock();
    }
  }

  /**
   * Obtiene el estado actual de la memoria
   * 
   * @return String con el estado de todos los marcos
   */
  public String getMemoryState() {
    memoryLock.lock();
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("\n=== ESTADO DE MEMORIA ===\n");
      sb.append(String.format("Marcos totales: %d\n", totalFrames));
      sb.append(String.format("Marcos ocupados: %d\n", getOccupiedFrameCount()));
      sb.append(String.format("Marcos libres: %d\n", getFreeFrameCount()));
      sb.append(String.format("\nAlgoritmo: %s\n", replacementAlgorithm.getName()));
      sb.append(String.format("Fallos de página: %d\n", pageFaults));
      sb.append(String.format("Reemplazos: %d\n", pageReplacements));

      sb.append("\nEstado de marcos:\n");
      for (PageFrame frame : frames) {
        sb.append(frame.toString()).append("\n");
      }

      sb.append("\nTabla de páginas por proceso:\n");
      for (Map.Entry<String, Set<Integer>> entry : pageTable.entrySet()) {
        sb.append(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
      }

      return sb.toString();
    } finally {
      memoryLock.unlock();
    }
  }

  /**
   * Obtiene métricas de memoria
   * 
   * @return String con las métricas
   */
  public String getMemoryMetrics() {
    memoryLock.lock();
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("\n=== MÉTRICAS DE MEMORIA ===\n");
      sb.append(String.format("Algoritmo de reemplazo: %s\n", replacementAlgorithm.getName()));
      sb.append(String.format("Marcos totales: %d\n", totalFrames));
      sb.append(String.format("Total de fallos de página: %d\n", pageFaults));
      sb.append(String.format("Total de reemplazos: %d\n", pageReplacements));

      sb.append("\nFallos de página por proceso:\n");
      for (Map.Entry<String, Integer> entry : processPageFaults.entrySet()) {
        sb.append(String.format("  %s: %d fallos\n", entry.getKey(), entry.getValue()));
      }

      return sb.toString();
    } finally {
      memoryLock.unlock();
    }
  }

  // Getters

  public int getTotalFrames() {
    return totalFrames;
  }

  public int getOccupiedFrameCount() {
    int count = 0;
    for (PageFrame frame : frames) {
      if (frame.isOccupied()) {
        count++;
      }
    }
    return count;
  }

  public int getFreeFrameCount() {
    return totalFrames - getOccupiedFrameCount();
  }

  public int getPageFaults() {
    return pageFaults;
  }

  public int getPageReplacements() {
    return pageReplacements;
  }

  public Map<String, Integer> getProcessPageFaults() {
    return new HashMap<>(processPageFaults);
  }

  public PageReplacementAlgorithm getReplacementAlgorithm() {
    return replacementAlgorithm;
  }

  public void setReplacementAlgorithm(PageReplacementAlgorithm algorithm) {
    memoryLock.lock();
    try {
      this.replacementAlgorithm = algorithm;
      algorithm.reset();
      if (algorithm instanceof OptimalPageReplacement optimal) {
        for (Process process : processRegistry.values()) {
          optimal.setFutureAccesses(process.getPid(), buildReferenceString(process));
        }
      }
      System.out.println(String.format("[MEMORIA] Algoritmo cambiado a: %s", algorithm.getName()));
    } finally {
      memoryLock.unlock();
    }
  }

  /**
   * Notifica a memoria que un proceso consumió CPU para actualizar accesos
   */
  public void notifyProcessCPUUsage(Process process, int executedUnits) {
    if (process == null || executedUnits <= 0) {
      return;
    }

    // Cada unidad de CPU simula un acceso a una página
    int pages = Math.max(1, process.getRequiredPages());
    int totalCpu = process.getTotalCPUTime();
    int remaining = process.getRemainingCPUTime();

    // Cuánto CPU ya se ha ejecutado
    int executedSoFar = totalCpu - remaining;

    int firstIndex = Math.max(0, executedSoFar - executedUnits);

    for (int i = 0; i < executedUnits; i++) {
      int accessIndex = firstIndex + i;
      int pageId = accessIndex % pages;
      ensurePageLoaded(process, pageId);
      accessPage(process.getPid(), pageId);
    }
  }

  private void registerProcessIfNeeded(Process process) {
    String pid = process.getPid();
    if (!processRegistry.containsKey(pid)) {
      processRegistry.put(pid, process);
      if (replacementAlgorithm instanceof OptimalPageReplacement optimal) {
        optimal.setFutureAccesses(pid, buildReferenceString(process));
      }
    } else {
      processRegistry.put(pid, process);
    }
  }

  private void ensurePageLoadedInternal(Process process, int pageId) {
    if (process.getRequiredPages() <= 0) {
      return;
    }

    String pid = process.getPid();
    if (isPageLoadedInternal(pid, pageId)) {
      return;
    }

    loadPageInternal(process, pageId);
  }

  private List<Integer> buildReferenceString(Process process) {
    int totalCpu = process.getTotalCPUTime();
    int pages = Math.max(1, process.getRequiredPages());
    List<Integer> sequence = new ArrayList<>(totalCpu);
    for (int i = 0; i < totalCpu; i++) {
      sequence.add(i % pages);
    }
    return sequence;
  }

  /**
   * Resetea las métricas de memoria
   */
  public void reset() {
    memoryLock.lock();
    try {
      // Limpiar todos los marcos
      for (PageFrame frame : frames) {
        frame.unloadPage();
      }

      pageFaults = 0;
      pageReplacements = 0;
      processPageFaults.clear();
      pageTable.clear();
      processRegistry.clear();
      replacementAlgorithm.reset();

      System.out.println("[MEMORIA] Memoria reseteada");
    } finally {
      memoryLock.unlock();
    }
  }
}
