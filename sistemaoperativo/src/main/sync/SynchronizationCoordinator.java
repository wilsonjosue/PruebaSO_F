//src/main/sync/SynchronizationCoordinator.java
package sync;

import model.Process;
import memory.MemoryManager;
import scheduler.SchedulingAlgorithm;
import io.IOManager;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/**
 * Coordinador de sincronización entre módulos del sistema
 * Asegura la correcta secuencia de ejecución entre planificador, memoria y E/S
 * Añadí una bandera hasReadyProcess para waitForReadyProcess en lugar de depender sólo de scheduler.getReadyQueue() mientras se mantiene el coordinationLock.
 */
public class SynchronizationCoordinator {
  private MemoryManager memoryManager;
  private SchedulingAlgorithm scheduler;
  private IOManager ioManager;
  
  private Lock coordinationLock;
  private Condition processReady;
  // Para evitar llamar al scheduler desde dentro del lock en waitForReadyProcess
  private volatile boolean hasReadyProcess = false;
  
  public SynchronizationCoordinator(MemoryManager memoryManager, 
                                     SchedulingAlgorithm scheduler,
                                     IOManager ioManager) {
    this.memoryManager = memoryManager;
    this.scheduler = scheduler;
    this.ioManager = ioManager;
    
    this.coordinationLock = new ReentrantLock();
    this.processReady = coordinationLock.newCondition();
    
    System.out.println("SynchronizationCoordinator inicializado");
  }
  
  /**
   * Prepara un proceso para ejecución
   * Coordina con memoria para asegurar que las páginas estén cargadas
   * @param process Proceso a preparar
   * @return true si el proceso está listo, false si fue bloqueado
   */
  public boolean prepareProcessForExecution(Process process) {
    // No mantenemos coordinationLock mientras llamamos a memoryManager
    boolean ready = memoryManager.loadPagesForProcess(process);
    if (ready) {
      coordinationLock.lock();
      try {
        hasReadyProcess = true;
        processReady.signalAll();
      } finally {
        coordinationLock.unlock();
      }
    }
    return ready;
  }

  /**
   * Maneja el cambio de contexto entre procesos
   * @param currentProcess Proceso actual (puede ser null)
   * @param nextProcess Siguiente proceso a ejecutar
   */
  public void handleContextSwitch(Process currentProcess, Process nextProcess) {
    if (currentProcess != null) {
      System.out.println(String.format("[SYNC] Cambio de contexto: %s -> %s",
          currentProcess.getPid(),
          nextProcess != null ? nextProcess.getPid() : "IDLE"));
    }

    // Preparar siguiente proceso fuera del coordinationLock (evitar nested locks)
    if (nextProcess != null) {
      prepareProcessForExecution(nextProcess);
    }
  }

  /**
   * Maneja el bloqueo de un proceso por E/S
   * @param process Proceso que se bloquea
   * @param ioDuration Duración de la operación de E/S
   */
  public void handleIOBlocking(Process process, int ioDuration) {
    // No mantenemos coordinationLock mientras iniciamos la operación de E/S
    System.out.println(String.format("[SYNC] Proceso %s bloqueado por E/S (duración: %d)",
        process.getPid(), ioDuration));
    ioManager.startIOOperation(process, ioDuration);
  }

  /**
   * Notifica que un proceso completó su E/S
   * @param process Proceso que completó E/S
   */
  public void notifyIOComplete(Process process) {
    // No mantenemos coordinationLock mientras interactuamos con scheduler y proceso
    System.out.println(String.format("[SYNC] Proceso %s completó E/S, vuelve a cola de listos",
        process.getPid()));

    // Actualizar estado del proceso (rápido)
    process.setState(Process.ProcessState.READY);

    // Agregar al scheduler fuera del coordinationLock (scheduler manejará su propia sincronización)
    scheduler.addProcess(process);

    // Notificar a posibles waiters
    coordinationLock.lock();
    try {
      hasReadyProcess = true;
      processReady.signalAll();
    } finally {
      coordinationLock.unlock();
    }
  }

  /**
   * Notifica que un proceso completó su ejecución
   * @param process Proceso completado
   */
  public void notifyProcessComplete(Process process) {
    System.out.println(String.format("[SYNC] Proceso %s completado, liberando recursos",
        process.getPid()));

    // Marcar terminado (rápido)
    process.setState(Process.ProcessState.TERMINATED);

    // Liberar recursos y notificar al scheduler fuera del lock (evitar nested locks)
    memoryManager.freePagesForProcess(process);
    scheduler.onProcessCompletion(process);
  }

  /**
   * Espera a que haya procesos listos
   */
  public void waitForReadyProcess() throws InterruptedException {
    coordinationLock.lock();
    try {
      while (!hasReadyProcess) {
        processReady.await();
      }
      // Resetear la bandera antes de salir
      hasReadyProcess = false;
    } finally {
      coordinationLock.unlock();
    }
  }
  
  /**
   * Notifica que hay procesos listos
   */
  public void signalProcessReady() {
    coordinationLock.lock();
    try {
      hasReadyProcess = true;
      processReady.signalAll();
    } finally {
      coordinationLock.unlock();
    }
  }
  
  public MemoryManager getMemoryManager() {
    return memoryManager;
  }
  
  public SchedulingAlgorithm getScheduler() {
    return scheduler;
  }
  
  public IOManager getIOManager() {
    return ioManager;
  }
}


