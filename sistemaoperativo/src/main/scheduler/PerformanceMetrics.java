package scheduler;

import model.Process;
import model.Burst;
import java.util.*;

/**
 * Clase para calcular y almacenar métricas de rendimiento de schedulers
 * Calcula: Tiempo de espera, tiempo de retorno, tiempo de respuesta
 */
public class PerformanceMetrics {
  
  // Información por proceso
  private Map<String, ProcessMetrics> processMetrics;
  
  public PerformanceMetrics() {
    this.processMetrics = new HashMap<>();
  }
  
  /**
   * Registra la llegada de un proceso
   */
  public void recordArrival(Process process) {
    String pid = process.getPid();
    if (!processMetrics.containsKey(pid)) {
      ProcessMetrics pm = new ProcessMetrics(pid, process.getArrivalTime(), process);
      processMetrics.put(pid, pm);
    }
  }
  
  /**
   * Registra el primer inicio de ejecución de un proceso
   */
  public void recordFirstExecution(Process process, int time) {
    String pid = process.getPid();
    ProcessMetrics metrics = processMetrics.get(pid);
    if (metrics != null && metrics.firstExecutionTime == -1) {
      metrics.firstExecutionTime = time;
    }
  }
  
  /**
   * Registra la finalización de un proceso
   */
  public void recordCompletion(Process process, int time) {
    String pid = process.getPid();
    ProcessMetrics metrics = processMetrics.get(pid);
    if (metrics != null) {
      metrics.completionTime = time;
    }
  }
  
  /**
   * Obtiene el tiempo de espera promedio
   */
  public double getAverageWaitingTime() {
    if (processMetrics.isEmpty()) return 0.0;
    
    double totalWait = 0;
    int count = 0;
    
    for (ProcessMetrics metrics : processMetrics.values()) {
      if (metrics.completionTime != -1) {
        // Tiempo de espera = Tiempo de retorno - Tiempo de CPU
        int waitTime = metrics.getWaitingTime();
        totalWait += waitTime;
        count++;
      }
    }
    
    return count > 0 ? totalWait / count : 0.0;
  }
  
  /**
   * Obtiene el tiempo de retorno promedio
   */
  public double getAverageTurnaroundTime() {
    if (processMetrics.isEmpty()) return 0.0;
    
    double totalTurnaround = 0;
    int count = 0;
    
    for (ProcessMetrics metrics : processMetrics.values()) {
      if (metrics.completionTime != -1) {
        int turnaroundTime = metrics.getTurnaroundTime();
        totalTurnaround += turnaroundTime;
        count++;
      }
    }
    
    return count > 0 ? totalTurnaround / count : 0.0;
  }
  
  /**
   * Obtiene el tiempo de respuesta promedio
   */
  public double getAverageResponseTime() {
    if (processMetrics.isEmpty()) return 0.0;
    
    double totalResponse = 0;
    int count = 0;
    
    for (ProcessMetrics metrics : processMetrics.values()) {
      if (metrics.firstExecutionTime != -1) {
        int responseTime = metrics.getResponseTime();
        totalResponse += responseTime;
        count++;
      }
    }
    
    return count > 0 ? totalResponse / count : 0.0;
  }
  
  /**
   * Obtiene el número de procesos completados
   */
  public int getCompletedProcessCount() {
    int count = 0;
    for (ProcessMetrics metrics : processMetrics.values()) {
      if (metrics.completionTime != -1) {
        count++;
      }
    }
    return count;
  }
  
  /**
   * Genera reporte detallado de métricas
   */
  public String generateReport() {
    StringBuilder sb = new StringBuilder();
    sb.append("\nMÉTRICAS DE RENDIMIENTO\n");
    sb.append(String.format("Procesos completados: %d\n", getCompletedProcessCount()));
    sb.append(String.format("Tiempo de espera promedio: %.2f unidades\n", getAverageWaitingTime()));
    sb.append(String.format("Tiempo de retorno promedio: %.2f unidades\n", getAverageTurnaroundTime()));
    sb.append(String.format("Tiempo de respuesta promedio: %.2f unidades\n", getAverageResponseTime()));
    
    sb.append("\nDetalles por Proceso\n");
    for (ProcessMetrics metrics : processMetrics.values()) {
      if (metrics.completionTime != -1) {
        sb.append(String.format("%s: Llegada=%d, Primera Ejecución=%d, Finalización=%d, " +
            "Espera=%d, Retorno=%d, Respuesta=%d\n",
            metrics.pid,
            metrics.arrivalTime,
            metrics.firstExecutionTime,
            metrics.completionTime,
            metrics.getWaitingTime(),
            metrics.getTurnaroundTime(),
            metrics.getResponseTime()));
      }
    }
    
    return sb.toString();
  }
  
  /**
   * Clase interna para almacenar métricas de un proceso individual
   */
  private static class ProcessMetrics {
    String pid;
    int arrivalTime;
    int firstExecutionTime;
    int completionTime;
    int executedCPUTime;
    int totalIOTime;
    
    ProcessMetrics(String pid, int arrivalTime, Process process) {
      this.pid = pid;
      this.arrivalTime = arrivalTime;
      this.firstExecutionTime = -1;
      this.completionTime = -1;
      this.executedCPUTime = 0;
      this.totalIOTime = calculateTotalIOTime(process);
    }
    
    int getTurnaroundTime() {
      return completionTime - arrivalTime;
    }
    
    int getWaitingTime() {
      // Tiempo de espera = Tiempo de retorno - Tiempo de CPU
      int waitTime = getTurnaroundTime() - executedCPUTime - totalIOTime;
      return Math.max(0, waitTime);
    }
    
    int getResponseTime() {
      return firstExecutionTime - arrivalTime;
    }
        
    private int calculateTotalIOTime(Process process) {
      int total = 0;
      for (Burst burst : process.getBursts()) {
        if (burst.getType() == Burst.BurstType.IO) {
          total += burst.getDuration();
        }
      }
      return total;
    }
  }
  
  /**
   * Registra tiempo de CPU ejecutado para un proceso
   */
  public void addCPUTime(Process process, int cpuTime) {
    String pid = process.getPid();
    ProcessMetrics metrics = processMetrics.get(pid);
    if (metrics != null) {
      metrics.executedCPUTime += cpuTime;
    }
  }
  
  /**
   * Resetea todas las métricas
   */
  public void reset() {
    processMetrics.clear();
  }
}
