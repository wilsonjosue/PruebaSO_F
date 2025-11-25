package io;

import model.Process;
import scheduler.SimulationClock;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestor de operaciones de E/S
 * Simula dispositivos de E/S y maneja procesos bloqueados por E/S
 */
public class IOManager {
  private Map<String, IOOperation> activeOperations;
  private Lock ioLock;
  private int totalIOOperations;
  private int completedIOOperations;
  
  public IOManager() {
    this.activeOperations = new HashMap<>();
    this.ioLock = new ReentrantLock();
    this.totalIOOperations = 0;
    this.completedIOOperations = 0;
    
    System.out.println("IOManager inicializado");
  }
  
  /**
   * Inicia una operación de E/S para un proceso
   * 
   * @param process Proceso que solicita E/S
   * @param duration Duración de la operación
   */
  public void startIOOperation(Process process, int duration) {
    ioLock.lock();
    try {
      String pid = process.getPid();
      int startTime = SimulationClock.getTime();
      int endTime = startTime + duration;
      
      IOOperation operation = new IOOperation(pid, startTime, endTime, duration);
      activeOperations.put(pid, operation);
      totalIOOperations++;
      
      process.setState(Process.ProcessState.BLOCKED_IO);
      process.resetIOReady();
      
      System.out.println(String.format("[E/S] Proceso %s inicia operación de E/S (duración: %d, finaliza en t=%d)",
          pid, duration, endTime));
      
    } finally {
      ioLock.unlock();
    }
  }
  
  /**
   * Actualiza las operaciones de E/S en curso y completa las que terminaron
   * 
   * @return Lista de procesos que completaron su E/S
   */
  public List<Process> updateIOOperations(List<Process> allProcesses) {
    ioLock.lock();
    try {
      List<Process> completedProcesses = new ArrayList<>();
      int currentTime = SimulationClock.getTime();
      
      Iterator<Map.Entry<String, IOOperation>> iterator = activeOperations.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, IOOperation> entry = iterator.next();
        IOOperation operation = entry.getValue();
        
        if (currentTime >= operation.getEndTime()) {
          // La operación de E/S ha terminado
          String pid = entry.getKey();
          
          // Buscar el proceso correspondiente
          Process process = findProcess(allProcesses, pid);
          if (process != null) {
            process.signalIOComplete();
            process.setState(Process.ProcessState.READY);
            completedProcesses.add(process);
            
            System.out.println(String.format("[E/S] Proceso %s completó operación de E/S en t=%d",
                pid, currentTime));
          }
          
          completedIOOperations++;
          iterator.remove();
        }
      }
      
      return completedProcesses;
      
    } finally {
      ioLock.unlock();
    }
  }
  
  /**
   * Busca un proceso por su PID
   */
  private Process findProcess(List<Process> processes, String pid) {
    for (Process p : processes) {
      if (p.getPid().equals(pid)) {
        return p;
      }
    }
    return null;
  }
  
  /**
   * Verifica si un proceso tiene una operación de E/S activa
   */
  public boolean hasActiveIOOperation(String pid) {
    ioLock.lock();
    try {
      return activeOperations.containsKey(pid);
    } finally {
      ioLock.unlock();
    }
  }
  
  /**
   * Obtiene el estado de las operaciones de E/S
   */
  public String getIOState() {
    ioLock.lock();
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("\n=== ESTADO DE E/S ===\n");
      sb.append(String.format("Operaciones activas: %d\n", activeOperations.size()));
      sb.append(String.format("Total de operaciones: %d\n", totalIOOperations));
      sb.append(String.format("Operaciones completadas: %d\n", completedIOOperations));
      
      if (!activeOperations.isEmpty()) {
        sb.append("\nOperaciones en curso:\n");
        int currentTime = SimulationClock.getTime();
        for (IOOperation op : activeOperations.values()) {
          int remaining = op.getEndTime() - currentTime;
          sb.append(String.format("  %s: finaliza en t=%d (quedan %d unidades)\n",
              op.getProcessId(), op.getEndTime(), remaining));
        }
      }
      
      return sb.toString();
    } finally {
      ioLock.unlock();
    }
  }
  
  /**
   * Obtiene métricas de E/S
   */
  public String getIOMetrics() {
    ioLock.lock();
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("\n=== MÉTRICAS DE E/S ===\n");
      sb.append(String.format("Total de operaciones de E/S: %d\n", totalIOOperations));
      sb.append(String.format("Operaciones completadas: %d\n", completedIOOperations));
      sb.append(String.format("Operaciones activas: %d\n", activeOperations.size()));
      
      return sb.toString();
    } finally {
      ioLock.unlock();
    }
  }
  
  /**
   * Resetea el gestor de E/S
   */
  public void reset() {
    ioLock.lock();
    try {
      activeOperations.clear();
      totalIOOperations = 0;
      completedIOOperations = 0;
      System.out.println("[E/S] IOManager reseteado");
    } finally {
      ioLock.unlock();
    }
  }
  
  // Getters
  
  public int getTotalIOOperations() {
    return totalIOOperations;
  }
  
  public int getCompletedIOOperations() {
    return completedIOOperations;
  }
  
  public int getActiveIOOperations() {
    ioLock.lock();
    try {
      return activeOperations.size();
    } finally {
      ioLock.unlock();
    }
  }
  
  /**
   * Clase interna para representar una operación de E/S
   */
  private static class IOOperation {
    private String processId;
    private int startTime;
    private int endTime;
    private int duration;
    
    public IOOperation(String processId, int startTime, int endTime, int duration) {
      this.processId = processId;
      this.startTime = startTime;
      this.endTime = endTime;
      this.duration = duration;
    }
    
    public String getProcessId() {
      return processId;
    }
    
    public int getStartTime() {
      return startTime;
    }
    
    public int getEndTime() {
      return endTime;
    }
    
    public int getDuration() {
      return duration;
    }
  }
}
