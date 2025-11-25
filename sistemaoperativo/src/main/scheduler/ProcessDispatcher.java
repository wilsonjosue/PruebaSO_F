package scheduler;

import model.Process;
import model.Burst;
import java.util.*;

/**
 * Dispatcher principal que coordina la ejecucion de procesos
 * Maneja la simulacion completa con reloj sincronizado
 */
public class ProcessDispatcher {
  private final SchedulingAlgorithm scheduler;
  private final GanttChart ganttChart;
  private final List<Process> allProcesses;

  public ProcessDispatcher(SchedulingAlgorithm scheduler) {
    this.scheduler = scheduler;
    this.ganttChart = new GanttChart();
    this.allProcesses = new ArrayList<>();
  }

  /**
   * Registra un proceso para la simulacion
   */
  public void registerProcess(Process process) {
    allProcesses.add(process);
    process.setState(Process.ProcessState.NEW);
  }

  /**
   * Ejecuta la simulacion :)
   */
  public void runSimulation() {
    System.out.println("\n                             INICIANDO SIMULACIoN ");
    System.out.println("Algoritmo: " + scheduler.getClass().getSimpleName());
    System.out.println("Procesos registrados: " + allProcesses.size());
    
    SimulationClock.reset();
    int currentTime = 0;
    Process currentProcess = null;
    int quantumUsed = 0;
    int quantum = getQuantum();
    Map<String, Boolean> firstExecution = new HashMap<>();
    
    // Ordenar procesos por tiempo de llegada
    allProcesses.sort(Comparator.comparingInt(Process::getArrivalTime));
    
    while (!allProcessesCompleted()) {
      // 1. Agregar procesos que han llegado al scheduler
      addArrivingProcesses(currentTime);
      
      // 2. Si no hay proceso actual, obtener el siguiente
      if (currentProcess == null) {
        currentProcess = scheduler.getNextProcess();
        
        if (currentProcess != null) {
          // Marcar primera ejecucion
          if (!firstExecution.containsKey(currentProcess.getPid())) {
            scheduler.onProcessStarted(currentProcess);
            firstExecution.put(currentProcess.getPid(), true);
          }
          
          currentProcess.setState(Process.ProcessState.RUNNING);
          ganttChart.recordExecution(currentProcess.getPid(), currentTime);
          quantumUsed = 0;
          
          System.out.println(String.format("[T=%d] Ejecutando %s (CPU restante: %d)",
              currentTime, currentProcess.getPid(), currentProcess.getRemainingCPUTime()));
        } else {
          // CPU idle
          if (!allProcessesCompleted()) {
            System.out.println(String.format("[T=%d] CPU IDLE", currentTime));
            ganttChart.recordExecution("IDLE", currentTime);
          }
          currentTime++;
          SimulationClock.setTime(currentTime);
          continue;
        }
      }
      
      // 3. Ejecutar el proceso actual por 1 unidad de tiempo
      if (currentProcess != null) {
        Burst activeBurst = currentProcess.getCurrentBurst();
        if (activeBurst == null) {
          // Proceso termino entre iteraciones
          currentProcess.setState(Process.ProcessState.TERMINATED);
          currentProcess.setCompletionTime(currentTime);
          scheduler.onProcessCompletion(currentProcess);
          currentProcess = null;
          continue;
        }
        
        if (activeBurst.getType() == Burst.BurstType.IO) {
          handleImmediateIO(currentProcess, activeBurst, currentTime);
          currentProcess = null;
          quantumUsed = 0;
          continue;
        }
        
        int executed = currentProcess.executeBurst(1);
        scheduler.recordCPUExecution(currentProcess, executed);
        quantumUsed++;
        currentTime++;
        SimulationClock.setTime(currentTime);
        
        if (activeBurst.getRemainingTime() <= 0) {
          if (currentProcess.isCompleted()) {
            currentProcess.setState(Process.ProcessState.TERMINATED);
            currentProcess.setCompletionTime(currentTime);
            scheduler.onProcessCompletion(currentProcess);
            System.out.println(String.format("[T=%d] %s TERMINADO",
                currentTime, currentProcess.getPid()));
            currentProcess = null;
            quantumUsed = 0;
          } else {
            Burst nextBurst = currentProcess.getCurrentBurst();
            if (nextBurst != null && nextBurst.getType() == Burst.BurstType.IO) {
              handleImmediateIO(currentProcess, nextBurst, currentTime);
              currentProcess = null;
              quantumUsed = 0;
            }
          }
        } else if (scheduler.isPreemptive() && quantumUsed >= quantum) {
          System.out.println(String.format("[T=%d] %s interrumpido por quantum",
              currentTime, currentProcess.getPid()));
          currentProcess.setState(Process.ProcessState.READY);
          scheduler.onProcessInterrupted(currentProcess);
          currentProcess = null;
          quantumUsed = 0;
        }
      }
      
      // Prevenir loops infinitos
      if (currentTime > 1000) {
        System.out.println("ADVERTENCIA: Ya nos pasamos mas de 1000 unidades de tiempo");
        break;
      }
    }
    
    ganttChart.finalizeChart(currentTime);
    System.out.println("\nSIMULACIoN TERMINADA");
    System.out.println("Tiempo total: " + currentTime + " unidades");
  }

  /**
   * Agrega procesos que han llegado al sistema
   */
  private void addArrivingProcesses(int currentTime) {
    for (Process process : allProcesses) {
      if (process.getState() == Process.ProcessState.NEW && 
          process.getArrivalTime() <= currentTime) {
        process.setState(Process.ProcessState.READY);
        scheduler.addProcess(process);
        System.out.println(String.format("[T=%d] %s llega al sistema",
            currentTime, process.getPid()));
      }
    }
  }

  /**
   * Verifica si todos los procesos han terminado
   */
  private boolean allProcessesCompleted() {
    for (Process process : allProcesses) {
      if (process.getState() != Process.ProcessState.TERMINATED) {
        return false;
      }
    }
    return true;
  }

  /**
   * Obtiene el quantum del scheduler (solo para RR)
   */
  private int getQuantum() {
    if (scheduler instanceof RoundRobinScheduler) {
      return ((RoundRobinScheduler) scheduler).getQuantum();
    }
    return Integer.MAX_VALUE; // No hay quantum para otros schedulers
  }

  /**
   * Obtiene métricas de la simulacion
   */
  public String getMetrics() {
    return scheduler.getMetrics() + "\n" + ganttChart.toString();
  }

  /**
   * Obtiene el diagrama de Gantt
   */
  public GanttChart getGanttChart() {
    return ganttChart;
  }
  
  /**
   * Obtiene las métricas de rendimiento
   */
  public PerformanceMetrics getPerformanceMetrics() {
    return scheduler.getPerformanceMetrics();
  }

  /**
   * Maneja una rafaga de I/O completandola de forma inmediata para el modo consola
   */
  private void handleImmediateIO(Process process, Burst ioBurst, int currentTime) {
    process.setState(Process.ProcessState.BLOCKED_IO);
    System.out.println(String.format("[T=%d] %s bloqueado por I/O (%d unidades)",
        currentTime, process.getPid(), ioBurst.getDuration()));
    ganttChart.addEvent(currentTime, process.getPid() + " -> I/O");
    process.completeCurrentBurst();
    process.setState(Process.ProcessState.READY);
    scheduler.addProcess(process);
  }
}