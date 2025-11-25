package scheduler;

import model.Process;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * First-Come, First-Served (FCFS) - No apropiativo
 * Los procesos se ejecutan en el orden de llegada a la cola.
 * Ventajas: Simple, sin hambruna
 * Desventajas: Poco rendimiento con procesos largos al inicio
 */
public class FCFSScheduler implements SchedulingAlgorithm {
  private final Queue<Process> readyQueue;
  private final PerformanceMetrics metrics;
  private final Lock queueLock;

  public FCFSScheduler() {
    this.readyQueue = new LinkedList<>();
    this.metrics = new PerformanceMetrics();
    this.queueLock = new ReentrantLock();
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
      System.out.println("FCFS: Proceso " + process.getPid() +
          " agregado a cola. Tamaño cola: " + readyQueue.size());
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
    System.out.println("FCFS: Proceso " + process.getPid() + " completado");
  }

  @Override
  public void onProcessInterrupted(Process process) {
    // FCFS no es apropiativo, si ocurre una interrupción (ej: I/O)
    // el proceso debe ir al FINAL de la cola para respetar el orden FIFO
    if (process != null && process.getRemainingCPUTime() > 0) {
      queueLock.lock();
      try {
        readyQueue.offer(process);
        System.out.println("FCFS: Proceso " + process.getPid() + " reinsertado al final de la cola");
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
    return false; // FCFS no es apropiativo
  }

  @Override
  public String getMetrics() {
    return "FCFS Scheduler\n" + metrics.generateReport();
  }
  
  @Override
  public PerformanceMetrics getPerformanceMetrics() {
    return metrics;
  }
}