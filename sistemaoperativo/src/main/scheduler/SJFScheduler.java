//src/main/scheduler/SJFScheduler.java
package scheduler;

import model.Process;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Shortest Job First (SJF) - apropiativo por defecto
 * Ejecuta primero el proceso con la rafaga de CPU mas corta.
 */
public class SJFScheduler implements SchedulingAlgorithm {
  private final PriorityQueue<Process> readyQueue;
  private final PerformanceMetrics metrics;
  private final Lock queueLock;
   private final Comparator<Process> comparator;
  private final boolean autoReinsertOnInterrupt;

  //Constructor por defecto: NO reinserta automáticamente procesos en onProcessInterrupted.
  public SJFScheduler() {
    this(false);
  }

    public SJFScheduler(boolean autoReinsertOnInterrupt) {
    // Comparator estable: current CPU burst, arrivalTime, pid (para determinismo)
    Comparator<Process> cmp = Comparator
      .comparingInt((Process p) -> p.getCurrentCPUBurstTime())
      .thenComparingInt(Process::getArrivalTime)
      .thenComparing(Process::getPid);

    // Guardar comparator en campo y usarlo para la PriorityQueue
    this.comparator = cmp;
    this.readyQueue = new PriorityQueue<>(cmp);
    this.metrics = new PerformanceMetrics();
    this.queueLock = new ReentrantLock();
    this.autoReinsertOnInterrupt = autoReinsertOnInterrupt;
  }

  @Override
  public Process getNextProcess() {
    queueLock.lock();
    try { 
      return readyQueue.poll();
    } finally { 
      queueLock.unlock();
    }
  }

  @Override
  public void addProcess(Process process) {
    // Antes de encolar: sólo aceptar procesos que estén realmente en READY
    if (process == null) return;
    queueLock.lock();
    try {
      // Evitar duplicados y forzar re-heapify si el proceso ya estaba
      readyQueue.remove(process);
      readyQueue.offer(process);
      System.out.println("SJF: Proceso " + process.getPid() +
          " agregado/reordenado. Proxima rafaga: " + process.getCurrentCPUBurstTime() +
          ". Tamaño cola: " + readyQueue.size());
    } finally {
      queueLock.unlock();
    }
    metrics.recordArrival(process);
  }
  /**
   * Actualiza la prioridad de un proceso que ya está en la cola.
   * Debe llamarse cuando cambie el tiempo restante (por ejemplo,
   * si hay un ajuste externo que modifica las ráfagas mientras está en la cola).
   *
   * @param process proceso cuya prioridad cambió
   * @return true si el proceso existía en la cola y fue re-colocado; false si no estaba
   */
  public boolean updateProcessPriority(Process process) {
    queueLock.lock();
    try {
      boolean existed = readyQueue.remove(process);
      if (existed) {
        readyQueue.offer(process); // reinsertar para re-heapify
        System.out.println("SJF: Prioridad actualizada para " + process.getPid() +
            " (nueva rafaga: " + process.getCurrentCPUBurstTime() + ")");
      }
      return existed;
    } finally {
      queueLock.unlock();
    }
  }

  /**
   * Elimina un proceso de la cola si está presente.
   */
  public boolean removeProcess(Process process) {
    queueLock.lock();
    try {
      boolean removed = readyQueue.remove(process);
      if (removed) {
        System.out.println("SJF: Proceso " + process.getPid() + " removido de la cola");
      }
      return removed;
    } finally {
      queueLock.unlock();
    }
  }

  @Override
  public void onProcessCompletion(Process process) {
    int completionTime = process.getCompletionTime() >= 0
        ? process.getCompletionTime()
        : SimulationClock.getTime();
    metrics.recordCompletion(process, completionTime);
    System.out.println("SJF: Proceso " + process.getPid() + " completado");
  }

    @Override
  public void onProcessInterrupted(Process process) {
    // Control de reinsercion configurable:
    // - Si autoReinsertOnInterrupt == true, reinserta (ej. si interrupcion por quantum)
    // - Si false (por defecto) no reinserta: se espera que Dispatcher/MemoryManager
    //   re-agregue el proceso cuando realmente esté listo (recomendado para I/O/page-fault).
    if (process == null) {
      System.out.println("SJF: onProcessInterrupted recibido null");
      return;
    }

    if (process.getRemainingCPUTime() > 0 && autoReinsertOnInterrupt) {
      addProcess(process); // addProcess ya gestiona los locks internamente
      System.out.println("SJF: Proceso " + process.getPid() + " reinsertado por interrupcion");
    } else {
      System.out.println("SJF: Proceso " + process.getPid() + " interrumpido (no reinsertado automáticamente)");
    }
  }

  @Override
  public void onProcessStarted(Process process) {
    metrics.recordFirstExecution(process, SimulationClock.getTime());
  }
  
  @Override
  public void recordCPUExecution(Process process, int time) {
    metrics.addCPUTime(process, time);
  }

  @Override
  public List<Process> getReadyQueue() {
    queueLock.lock();
    try {
      // Devuelve lista ordenada sin alterar la PriorityQueue original
      List<Process> sortedList = new ArrayList<>(readyQueue);
      // Usar comparator guardado para evitar NullPointerException
      sortedList.sort(comparator);
      return sortedList;
    } finally {
      queueLock.unlock();
    }
  }

  @Override
  public boolean isPreemptive() {
    return autoReinsertOnInterrupt;
  }

  @Override
  public String getMetrics() {
    return "SJF Scheduler\n" + metrics.generateReport();
  }
  
  @Override
  public PerformanceMetrics getPerformanceMetrics() {
    return metrics;
  }
}