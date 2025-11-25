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
 */
public class SynchronizationCoordinator {
  private MemoryManager memoryManager;
  private SchedulingAlgorithm scheduler;
  private IOManager ioManager;
  
  private Lock coordinationLock;
  private Condition processReady;
  
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
   * 
   * @param process Proceso a preparar
   * @return true si el proceso está listo, false si fue bloqueado
   */
  public boolean prepareProcessForExecution(Process process) {
    coordinationLock.lock();
    try {
      memoryManager.loadPagesForProcess(process);
      return true;
    } finally {
      coordinationLock.unlock();
    }
  }
  
  /**
   * Maneja el cambio de contexto entre procesos
   * 
   * @param currentProcess Proceso actual (puede ser null)
   * @param nextProcess Siguiente proceso a ejecutar
   */
  public void handleContextSwitch(Process currentProcess, Process nextProcess) {
    coordinationLock.lock();
    try {
      if (currentProcess != null) {
        System.out.println(String.format("[SYNC] Cambio de contexto: %s -> %s",
            currentProcess.getPid(), 
            nextProcess != null ? nextProcess.getPid() : "IDLE"));
      }
      
      // Preparar siguiente proceso
      if (nextProcess != null) {
        prepareProcessForExecution(nextProcess);
      }
      
    } finally {
      coordinationLock.unlock();
    }
  }
  
  /**
   * Maneja el bloqueo de un proceso por E/S
   * 
   * @param process Proceso que se bloquea
   * @param ioDuration Duración de la operación de E/S
   */
  public void handleIOBlocking(Process process, int ioDuration) {
    coordinationLock.lock();
    try {
      System.out.println(String.format("[SYNC] Proceso %s bloqueado por E/S (duración: %d)",
          process.getPid(), ioDuration));
      
      ioManager.startIOOperation(process, ioDuration);
      
    } finally {
      coordinationLock.unlock();
    }
  }
  
  /**
   * Notifica que un proceso completó su E/S
   * 
   * @param process Proceso que completó E/S
   */
  public void notifyIOComplete(Process process) {
    coordinationLock.lock();
    try {
      System.out.println(String.format("[SYNC] Proceso %s completó E/S, vuelve a cola de listos",
          process.getPid()));
      
      process.setState(Process.ProcessState.READY);
      scheduler.addProcess(process);
      processReady.signalAll();
      
    } finally {
      coordinationLock.unlock();
    }
  }
  
  /**
   * Notifica que un proceso completó su ejecución
   * 
   * @param process Proceso completado
   */
  public void notifyProcessComplete(Process process) {
    coordinationLock.lock();
    try {
      System.out.println(String.format("[SYNC] Proceso %s completado, liberando recursos",
          process.getPid()));
      
      process.setState(Process.ProcessState.TERMINATED);
      memoryManager.freePagesForProcess(process);
      scheduler.onProcessCompletion(process);
      
    } finally {
      coordinationLock.unlock();
    }
  }
  
  /**
   * Espera a que haya procesos listos
   */
  public void waitForReadyProcess() throws InterruptedException {
    coordinationLock.lock();
    try {
      processReady.await();
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
