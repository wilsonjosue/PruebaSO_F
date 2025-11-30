//src/main/scheduler/RoundRobinScheduler.java
package scheduler;

import model.Process;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Round Robin (RR) - Apropiativo
 * Cada proceso ejecuta por un quantum de tiempo, luego pasa al siguiente.
 * Ventajas: Justo, sin hambruna, buen tiempo de respuesta
 * Desventajas: Overhead por cambios de contexto
 */
public class RoundRobinScheduler implements SchedulingAlgorithm {
  private final Queue<Process> readyQueue;
  private final int quantum;
  private final PerformanceMetrics metrics;
  private final Lock queueLock;
  private int contextSwitches;

  public RoundRobinScheduler(int quantum) {
    if (quantum <= 0) {
      throw new IllegalArgumentException("Quantum debe ser mayor a 0");
    }

    this.readyQueue = new LinkedList<>();
    this.quantum = quantum;
    this.metrics = new PerformanceMetrics();
    this.queueLock = new ReentrantLock();
    this.contextSwitches = 0;
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
    queueLock.lock();
    try {
      readyQueue.offer(process);
      System.out.println("RR: Proceso " + process.getPid() +
          " agregado. Tamaño cola: " + readyQueue.size());
    } finally {
      queueLock.unlock();
    }
    metrics.recordArrival(process);
  }

  @Override
  public void onProcessCompletion(Process process) {
    int completionTime = process.getCompletionTime() >= 0
        ? process.getCompletionTime()
        : SimulationClock.getTime();
    metrics.recordCompletion(process, completionTime);
    System.out.println("RR: Proceso " + process.getPid() + " completado");
  }

  @Override
  public void onProcessInterrupted(Process process) {
    // En RR, la interrupcion es normal por agotamiento de quantum
    if (process != null && process.getRemainingCPUTime() > 0) {
      queueLock.lock();
      try {
        readyQueue.offer(process);
        contextSwitches++;
        System.out.println("RR: Proceso " + process.getPid() +
            " interrumpido por quantum, reinsertado");
      } finally {
        queueLock.unlock();
      }
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
      return new ArrayList<>(readyQueue);
    } finally {
      queueLock.unlock();
    }
  }

  @Override
  public boolean isPreemptive() {
    return true; // Round Robin es apropiativo por quantum
  }

  @Override
  public String getMetrics() {
    return String.format("Round Robin Scheduler (Quantum=%d, Context Switches=%d)\n%s",
        quantum, contextSwitches, metrics.generateReport());
  }
  
  @Override
  public PerformanceMetrics getPerformanceMetrics() {
    return metrics;
  }

  public int getQuantum() {
    return quantum;
  }
  
  public int getContextSwitches() {
    return contextSwitches;
  }
}