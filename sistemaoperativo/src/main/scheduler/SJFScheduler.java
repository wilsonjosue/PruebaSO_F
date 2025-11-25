package scheduler;

import model.Process;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Shortest Job First (SJF) - No apropiativo por defecto
 * Ejecuta primero el proceso con la rafaga de CPU mas corta.
 */
public class SJFScheduler implements SchedulingAlgorithm {
  private final PriorityQueue<Process> readyQueue;
  private final PerformanceMetrics metrics;
  private final Lock queueLock;

  public SJFScheduler() {
    // Cola de prioridad ordenada por la proxima rafaga de CPU disponible
    this.readyQueue = new PriorityQueue<>(
        Comparator.comparingInt(SJFScheduler::getNextCPUBurstTime)
            .thenComparingInt(Process::getArrivalTime));
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
        System.out.println("SJF: Proceso " + process.getPid() +
          " agregado. Proxima rafaga: " + getNextCPUBurstTime(process) +
          ". Tamaño cola: " + readyQueue.size());
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
    System.out.println("SJF: Proceso " + process.getPid() + " completado");
  }

  @Override
  public void onProcessInterrupted(Process process) {
    if (process != null && process.getRemainingCPUTime() > 0) {
      queueLock.lock();
      try {
        readyQueue.offer(process);
        System.out.println("SJF: Proceso " + process.getPid() + " reinsertado");
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
      List<Process> sortedList = new ArrayList<>(readyQueue);
      sortedList.sort(Comparator.comparingInt(Process::getRemainingCPUTime));
      return sortedList;
    } finally {
      queueLock.unlock();
    }
  }

  @Override
  public boolean isPreemptive() {
    return false;
  }

  @Override
  public String getMetrics() {
    return "SJF Scheduler\n" + metrics.generateReport();
  }
  
  @Override
  public PerformanceMetrics getPerformanceMetrics() {
    return metrics;
  }

  private static int getNextCPUBurstTime(Process process) {
    int nextBurst = process.getCurrentCPUBurstTime();
    if (nextBurst > 0) {
      return nextBurst;
    }
    // Fallback para procesos que no tienen rafaga de CPU inmediata
    return Math.max(1, process.getRemainingCPUTime());
  }
}