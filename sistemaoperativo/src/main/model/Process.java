//src/main/model/Process.java
package model;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa un proceso en el sistema operativo simulado
 * Implementa Runnable para ejecutar como hilo independiente
 */
public class Process implements Runnable {
  private String pid;
  private int arrivalTime;
  private List<Burst> bursts;
  private int currentBurstIndex;
  private int priority;
  private int requiredPages;
  private ProcessState state;
  
  // Memoria virtual
  private Set<Integer> pageIds; // IDs de paginas que requiere este proceso
  private Set<Integer> loadedPages; // Paginas actualmente en memoria
  
  // Sincronizacion
  private Lock lock;
  private Condition memoryAvailable;
  private Condition ioComplete;
  private volatile boolean memoryReady;
  private volatile boolean ioReady;
  
  // Métricas de ejecucion
  private int completionTime;
  private int waitingTime;
  private int turnaroundTime;
  private int responseTime;
  private int firstExecutionTime;

  public enum ProcessState {
    NEW, READY, RUNNING, BLOCKED_MEMORY, BLOCKED_IO, TERMINATED
  }

  public Process(String pid, int arrivalTime, List<Burst> bursts, int priority, int requiredPages) {
    this.pid = pid;
    this.arrivalTime = arrivalTime;
    this.bursts = new ArrayList<>(bursts);
    this.currentBurstIndex = 0;
    this.priority = priority;
    this.requiredPages = requiredPages;
    this.state = ProcessState.NEW;
    
    // Inicializar paginas (IDs consecutivos desde 0)
    this.pageIds = new HashSet<>();
    this.loadedPages = Collections.newSetFromMap(new ConcurrentHashMap<>());
    for (int i = 0; i < requiredPages; i++) {
      pageIds.add(i);
    }
    
    // Inicializar sincronizacion
    this.lock = new ReentrantLock();
    this.memoryAvailable = lock.newCondition();
    this.ioComplete = lock.newCondition();
    this.memoryReady = false;
    this.ioReady = true;
    
    // Inicializar métricas
    this.completionTime = -1;
    this.waitingTime = 0;
    this.turnaroundTime = 0;
    this.responseTime = -1;
    this.firstExecutionTime = -1;
  }

  @Override
  public void run() {
    // La ejecucion real se controla desde el ProcessDispatcher
    // Este método se llama cuando el hilo es iniciado
    System.out.println("Hilo del proceso " + pid + " iniciado");
  }
  
  // Métodos de sincronizacion
  
  /**
   * Espera hasta que la memoria esté disponible para este proceso
   */
  public void waitForMemory() throws InterruptedException {
    lock.lock();
    try {
      while (!memoryReady) {
        memoryAvailable.await();
      }
    } finally {
      lock.unlock();
    }
  }
  
  /**
   * Notifica que la memoria esta lista para este proceso
   */
  public void signalMemoryReady() {
    lock.lock();
    try {
      memoryReady = true;
      memoryAvailable.signalAll();
    } finally {
      lock.unlock();
    }
  }
  
  /**
   * Espera hasta que la operacion de E/S se complete
   */
  public void waitForIO() throws InterruptedException {
    lock.lock();
    try {
      while (!ioReady) {
        ioComplete.await();
      }
    } finally {
      lock.unlock();
    }
  }
  
  /**
   * Notifica que la operacion de E/S se completo
   */
  public void signalIOComplete() {
    lock.lock();
    try {
      ioReady = true;
      ioComplete.signalAll();
    } finally {
      lock.unlock();
    }
  }
  
  /**
   * Resetea el estado de memoria
   */
  public void resetMemoryReady() {
    lock.lock();
    try {
      memoryReady = false;
    } finally {
      lock.unlock();
    }
  }
  
  /**
   * Resetea el estado de E/S
   */
  public void resetIOReady() {
    lock.lock();
    try {
      ioReady = false;
    } finally {
      lock.unlock();
    }
  }

  // Getters
  public String getPid() {
    return pid;
  }

  public int getArrivalTime() {
    return arrivalTime;
  }

  public List<Burst> getBursts() {
    return new ArrayList<>(bursts);
  }

  public int getPriority() {
    return priority;
  }

  public int getRequiredPages() {
    return requiredPages;
  }

  public ProcessState getState() {
    lock.lock();
    try {
      return this.state;
    } finally {
      lock.unlock();
    }
  }

  public void setState(ProcessState state) {
    lock.lock();
    try {
      this.state = state;
    } finally {
      lock.unlock();
    }
  }
  //concurrentes no es necesario bloquearlos
  public Set<Integer> getPageIds() {
    return new HashSet<>(pageIds);
  }
  
  public Set<Integer> getLoadedPages() {
    return new HashSet<>(loadedPages);
  }
  
  public void addLoadedPage(int pageId) {
    loadedPages.add(pageId);
  }
  
  public void removeLoadedPage(int pageId) {
    loadedPages.remove(pageId);
  }
  
  public void clearLoadedPages() {
    loadedPages.clear();
  }
  
  public boolean isPageLoaded(int pageId) {
    return loadedPages.contains(pageId);
  }
  
  public boolean allPagesLoaded() {
    return loadedPages.containsAll(pageIds);
  }
  
  // Métricas
  public int getCompletionTime() {
    lock.lock();
    try {
      return completionTime;
    } finally {
      lock.unlock();
    }
  }

  public void setCompletionTime(int completionTime) {
    lock.lock();
    try {
      this.completionTime = completionTime;
    } finally {
      lock.unlock();
    }
  }

  public int getWaitingTime() {
    lock.lock();
    try {
      return waitingTime;
    } finally {
      lock.unlock();
    }
  }
  
  public void setWaitingTime(int waitingTime) {
    lock.lock();
    try {
      this.waitingTime = waitingTime;
    } finally {
      lock.unlock();
    }
  }
  
  public int getTurnaroundTime() {
    lock.lock();
    try {
      return turnaroundTime;
    } finally {
      lock.unlock();
    }
  }
  
  public void setTurnaroundTime(int turnaroundTime) {
    lock.lock();
    try {
      this.turnaroundTime = turnaroundTime;
    } finally {
      lock.unlock();
    }
  }
  
  public int getResponseTime() {
    lock.lock();
    try {
      return responseTime;
    } finally {
      lock.unlock();
    }
  }
  
  public void setResponseTime(int responseTime) {
    lock.lock();
    try {
      this.responseTime = responseTime;
    } finally {
      lock.unlock();
    }
  }
  
  public int getFirstExecutionTime() {
    lock.lock();
    try {
      return firstExecutionTime;
    } finally {
      lock.unlock();
    }
  }
  
  public void setFirstExecutionTime(int firstExecutionTime) {
    lock.lock();
    try {
      this.firstExecutionTime = firstExecutionTime;
    } finally {
      lock.unlock();
    } 
  }

  //Obtiene el tiempo total de CPU que necesita el proceso
  public int getTotalCPUTime() {
    int total = 0;
    for (Burst burst : bursts) {
      if (burst.getType() == Burst.BurstType.CPU) {
        total += burst.getDuration();
      }
    }
    return total;
  }

  //Obtiene el tiempo restante de CPU
  public int getRemainingCPUTime() {
    int remaining = 0;
    for (int i = currentBurstIndex; i < bursts.size(); i++) {
      Burst burst = bursts.get(i);
      if (burst.getType() == Burst.BurstType.CPU) {
        remaining += burst.getRemainingTime();
      }
    }
    return remaining;
  }

  //Obtiene la rafaga actual de CPU (tiempo restante)
  public int getCurrentCPUBurstTime() {
    if (currentBurstIndex < bursts.size()) {
      Burst current = bursts.get(currentBurstIndex);
      if (current.getType() == Burst.BurstType.CPU) {
        return current.getRemainingTime();
      }
    }
    return 0;
  }

  //Obtiene la rafaga actual
  public Burst getCurrentBurst() {
    if (currentBurstIndex < bursts.size()) {
      return bursts.get(currentBurstIndex);
    }
    return null;
  }

  /**
   * Ejecuta la rafaga actual por un tiempo especificado
   * @param time Tiempo a ejecutar
   * @return Tiempo realmente ejecutado
   */
  public int executeBurst(int time) {
    if (currentBurstIndex >= bursts.size()) {
      return 0;
    }
    
    Burst current = bursts.get(currentBurstIndex);
    int executed = Math.min(time, current.getRemainingTime());
    current.setRemainingTime(current.getRemainingTime() - executed);
    
    // Si la rafaga se completo, avanzar al siguiente
    if (current.getRemainingTime() <= 0) {
      currentBurstIndex++;
    }
    
    return executed;
  }

  //Completa la rafaga actual y pasa a la siguiente
  public void completeCurrentBurst() {
    if (currentBurstIndex < bursts.size()) {
      bursts.get(currentBurstIndex).setRemainingTime(0);
      currentBurstIndex++;
    }
  }
  
  //Reinicia el índice de burst actual (para simulaciones)
  public void resetBursts() {
    currentBurstIndex = 0;
    for (Burst burst : bursts) {
      burst.setRemainingTime(burst.getDuration());
    }
  }

  //Verifica si el proceso ha completado todas sus rafagas
  public boolean isCompleted() {
    return currentBurstIndex >= bursts.size();
  }

  //Verifica si la siguiente rafaga es de E/S
  public boolean isNextBurstIO() {
    if (currentBurstIndex < bursts.size()) {
      Burst next = bursts.get(currentBurstIndex);
      return next.getType() == Burst.BurstType.IO;
    }
    return false;
  }

  //Obtiene el tiempo de la proxima rafaga de E/S
  public int getNextIOBurstTime() {
    if (currentBurstIndex < bursts.size()) {
      Burst next = bursts.get(currentBurstIndex);
      if (next.getType() == Burst.BurstType.IO) {
        return next.getDuration();
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    lock.lock();
    try {
      return String.format("Process[%s, Arrival: %d, State: %s, Bursts: %d]",
          pid, arrivalTime, state, bursts.size());
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    Process process = (Process) obj;
    return pid.equals(process.pid);
  }

  @Override
  public int hashCode() {
    return pid.hashCode();
  }
}