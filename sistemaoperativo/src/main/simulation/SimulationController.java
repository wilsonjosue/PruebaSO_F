package simulation;

import model.Process;
import model.Burst;
import scheduler.SchedulingAlgorithm;
import scheduler.SimulationClock;
import scheduler.GanttChart;
import memory.MemoryManager;
import io.IOManager;
import sync.SynchronizationCoordinator;
import java.util.*;

/**
 * Controlador principal de la simulación del sistema operativo
 * Coordina todos los módulos y ejecuta la simulación paso a paso
 */
public class SimulationController {
  private List<Process> allProcesses;
  private SchedulingAlgorithm scheduler;
  private MemoryManager memoryManager;
  private IOManager ioManager;
  private SynchronizationCoordinator coordinator;
  private GanttChart ganttChart;
  
  private int quantum; // Para Round Robin
  private boolean running;
  private int maxSimulationTime;
  
  public SimulationController(SchedulingAlgorithm scheduler,
                              MemoryManager memoryManager,
                              IOManager ioManager,
                              int quantum,
                              int maxSimulationTime) {
    this.allProcesses = new ArrayList<>();
    this.scheduler = scheduler;
    this.memoryManager = memoryManager;
    this.ioManager = ioManager;
    this.quantum = quantum;
    this.maxSimulationTime = maxSimulationTime;
    
    this.coordinator = new SynchronizationCoordinator(memoryManager, scheduler, ioManager);
    this.ganttChart = new GanttChart();
    this.running = false;
    
    System.out.println("\n=== SIMULADOR DE SISTEMA OPERATIVO ===");
    System.out.println("Algoritmo de planificación: " + scheduler.getClass().getSimpleName());
    System.out.println("Algoritmo de reemplazo: " + memoryManager.getReplacementAlgorithm().getName());
    System.out.println("Marcos de memoria: " + memoryManager.getTotalFrames());
    System.out.println("Quantum: " + (quantum > 0 ? quantum : "N/A"));
  }
  
  /**
   * Agrega procesos a la simulación
   */
  public void addProcesses(List<Process> processes) {
    allProcesses.addAll(processes);
    System.out.println(String.format("\nProcesos agregados: %d", processes.size()));
    for (Process p : processes) {
      System.out.println(String.format("  %s: Llegada=%d, CPU=%d, Páginas=%d, Ráfagas=%d",
          p.getPid(), p.getArrivalTime(), p.getTotalCPUTime(), 
          p.getRequiredPages(), p.getBursts().size()));
    }
  }
  
  /**
   * Ejecuta la simulación completa
   */
  public void runSimulation() {
    running = true;
    SimulationClock.reset();
    
    System.out.println("\n=== INICIANDO SIMULACIÓN ===\n");
    
    Process currentProcess = null;
    int quantumRemaining = 0;
    
    while (running && SimulationClock.getTime() < maxSimulationTime) {
      int currentTime = SimulationClock.getTime();
      
      System.out.println(String.format("\n--- Tiempo: %d ---", currentTime));
      
      // 1. Verificar llegada de nuevos procesos
      checkNewArrivals(currentTime);
      
      // 2. Actualizar operaciones de E/S
      List<Process> completedIO = ioManager.updateIOOperations(allProcesses);
      for (Process p : completedIO) {
        coordinator.notifyIOComplete(p);
      }
      
      // 3. Seleccionar proceso a ejecutar
      if (currentProcess == null || currentProcess.getState() != Process.ProcessState.RUNNING) {
        currentProcess = scheduler.getNextProcess();
        
        if (currentProcess != null) {
          // Preparar proceso para ejecución
          boolean ready = coordinator.prepareProcessForExecution(currentProcess);
          
          if (ready) {
            currentProcess.setState(Process.ProcessState.RUNNING);
            
            if (currentProcess.getFirstExecutionTime() == -1) {
              currentProcess.setFirstExecutionTime(currentTime);
              scheduler.onProcessStarted(currentProcess);
            }
            
            quantumRemaining = scheduler.isPreemptive() ? quantum : Integer.MAX_VALUE;
            
            System.out.println(String.format("[CPU] Ejecutando proceso %s", currentProcess.getPid()));
          } else {
            currentProcess = null;
          }
        }
      }
      
      // 4. Ejecutar proceso actual
      if (currentProcess != null) {
        Burst currentBurst = currentProcess.getCurrentBurst();
        
        // Verificar si la ráfaga actual es de E/S (puede ocurrir al inicio)
        if (currentBurst != null && currentBurst.getType() == Burst.BurstType.IO) {
          // Bloquear por E/S inmediatamente
          int ioDuration = currentBurst.getDuration();
          coordinator.handleIOBlocking(currentProcess, ioDuration);
          currentProcess.completeCurrentBurst();
          ganttChart.addEvent(currentTime,
              currentProcess.getPid() + " -> E/S");
          currentProcess = null;
          quantumRemaining = 0;
        } else if (currentBurst != null && currentBurst.getType() == Burst.BurstType.CPU) {
          // Ejecutar CPU
          int timeToExecute = Math.min(quantumRemaining, currentBurst.getRemainingTime());
          timeToExecute = Math.min(timeToExecute, 1); // Ejecutar 1 unidad a la vez
          
          int executed = currentProcess.executeBurst(timeToExecute);
          quantumRemaining -= executed;
          
          scheduler.recordCPUExecution(currentProcess, executed);
          memoryManager.notifyProcessCPUUsage(currentProcess, executed);
          ganttChart.addExecution(currentProcess.getPid(), currentTime, currentTime + executed);
          
          System.out.println(String.format("[CPU] %s ejecutó %d unidad(es). Restante burst: %d",
              currentProcess.getPid(), executed, currentBurst.getRemainingTime()));
          
          // Verificar si completó la ráfaga
          if (currentBurst.getRemainingTime() <= 0) {
            // Verificar si hay más ráfagas
            if (currentProcess.isCompleted()) {
              // Proceso terminado
              currentProcess.setCompletionTime(currentTime + executed);
              coordinator.notifyProcessComplete(currentProcess);
              ganttChart.addEvent(currentTime + executed, 
                  currentProcess.getPid() + " TERMINADO");
              currentProcess = null;
              quantumRemaining = 0;
            } else {
              // Verificar si siguiente ráfaga es E/S
              Burst nextBurst = currentProcess.getCurrentBurst();
              if (nextBurst != null && nextBurst.getType() == Burst.BurstType.IO) {
                // Bloquear por E/S
                int ioDuration = nextBurst.getDuration();
                coordinator.handleIOBlocking(currentProcess, ioDuration);
                currentProcess.completeCurrentBurst();
                ganttChart.addEvent(currentTime + executed,
                    currentProcess.getPid() + " -> E/S");
                currentProcess = null;
                quantumRemaining = 0;
              }
            }
          } else if (quantumRemaining <= 0 && scheduler.isPreemptive()) {
            // Quantum agotado en Round Robin
            System.out.println(String.format("[CPU] Quantum agotado para %s, reinsertando",
                currentProcess.getPid()));
            scheduler.onProcessInterrupted(currentProcess);
            currentProcess.setState(Process.ProcessState.READY);
            ganttChart.addEvent(currentTime + executed,
                currentProcess.getPid() + " -> QUANTUM");
            currentProcess = null;
            quantumRemaining = 0;
          }
        }
      } else {
        // CPU inactiva
        ganttChart.addExecution("IDLE", currentTime, currentTime + 1);
        System.out.println("[CPU] IDLE - No hay procesos listos");
      }
      
      // 5. Avanzar el reloj
      SimulationClock.incrementTime();
      
      // 6. Verificar si todos los procesos terminaron
      if (allProcessesCompleted()) {
        System.out.println("\n=== TODOS LOS PROCESOS COMPLETADOS ===");
        running = false;
      }
    }
    
    if (SimulationClock.getTime() >= maxSimulationTime) {
      System.out.println("\n=== TIEMPO MÁXIMO DE SIMULACIÓN ALCANZADO ===");
    }
    
    printFinalReport();
  }
  
  /**
   * Verifica la llegada de nuevos procesos
   */
  private void checkNewArrivals(int currentTime) {
    for (Process p : allProcesses) {
      if (p.getArrivalTime() == currentTime && p.getState() == Process.ProcessState.NEW) {
        p.setState(Process.ProcessState.READY);
        scheduler.addProcess(p);
        ganttChart.addEvent(currentTime, p.getPid() + " LLEGA");
        System.out.println(String.format("[LLEGADA] Proceso %s llegó al sistema", p.getPid()));
      }
    }
  }
  
  /**
   * Verifica si todos los procesos completaron su ejecución
   */
  private boolean allProcessesCompleted() {
    for (Process p : allProcesses) {
      if (p.getState() != Process.ProcessState.TERMINATED) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Imprime el reporte final de la simulación
   */
  private void printFinalReport() {
    System.out.println("\n" + "=".repeat(60));
    System.out.println("           REPORTE FINAL DE SIMULACIÓN");
    System.out.println("=".repeat(60));
    
    // Diagrama de Gantt
    System.out.println("\n" + ganttChart.toString());
    
    // Métricas del planificador
    System.out.println(scheduler.getMetrics());
    
    // Métricas de memoria
    System.out.println(memoryManager.getMemoryMetrics());
    
    // Métricas de E/S
    System.out.println(ioManager.getIOMetrics());
    
    // Estado final de memoria
    System.out.println(memoryManager.getMemoryState());
    
    // Resumen de procesos
    System.out.println("\n=== RESUMEN DE PROCESOS ===");
    for (Process p : allProcesses) {
      System.out.println(String.format("%s: Estado=%s, Llegada=%d, Primera Ejecución=%d, Finalización=%d",
          p.getPid(), p.getState(), p.getArrivalTime(), 
          p.getFirstExecutionTime(), p.getCompletionTime()));
    }
    
    System.out.println("\n" + "=".repeat(60));
    System.out.println("           FIN DE LA SIMULACIÓN");
    System.out.println("=".repeat(60));
  }
  
  /**
   * Detiene la simulación
   */
  public void stop() {
    running = false;
  }
  
  // Getters
  
  public List<Process> getAllProcesses() {
    return new ArrayList<>(allProcesses);
  }
  
  public SchedulingAlgorithm getScheduler() {
    return scheduler;
  }
  
  public MemoryManager getMemoryManager() {
    return memoryManager;
  }
  
  public IOManager getIOManager() {
    return ioManager;
  }
  
  public GanttChart getGanttChart() {
    return ganttChart;
  }
  
  public boolean isRunning() {
    return running;
  }
}
